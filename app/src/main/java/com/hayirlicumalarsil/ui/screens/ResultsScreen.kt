package com.hayirlicumalarsil.ui.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.hayirlicumalarsil.R
import com.hayirlicumalarsil.ScanViewModel
import com.hayirlicumalarsil.detection.Candidate
import com.hayirlicumalarsil.formatBytes
import com.hayirlicumalarsil.scan.ScanState
import com.hayirlicumalarsil.ui.components.ConfettiOverlay
import com.hayirlicumalarsil.ui.components.ScoreBadge
import com.hayirlicumalarsil.ui.theme.HeroGradient
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun ResultsScreen(vm: ScanViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val selectedIds by vm.selectedIds.collectAsStateWithLifecycle()
    val allCandidates by vm.candidates.collectAsStateWithLifecycle()
    val sortKey by vm.sortKey.collectAsStateWithLifecycle()
    val sortAscending by vm.sortAscending.collectAsStateWithLifecycle()
    val minScore by vm.minScoreFilter.collectAsStateWithLifecycle()
    val gridColumns by vm.gridColumns.collectAsStateWithLifecycle()
    var previewIndex by remember { mutableStateOf<Int?>(null) }

    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) vm.onSystemDeleteApproved()
        else vm.onSystemDeleteDenied()
    }

    val deletedState = state as? ScanState.Deleted
    if (deletedState != null) {
        DeletedCelebration(deletedState, onDismiss = vm::dismissDeletedCelebration)
        return
    }

    if (allCandidates.isEmpty()) {
        EmptyResults()
        return
    }

    // Filtre + sıralama uygulanmış görünür liste.
    val visible = remember(allCandidates, sortKey, sortAscending, minScore) {
        val filtered = allCandidates.filter { it.score >= minScore }
        val sorted = when (sortKey) {
            ScanViewModel.SortKey.SCORE -> filtered.sortedBy { it.score }
            ScanViewModel.SortKey.DATE -> filtered.sortedBy { it.image.dateMillis }
        }
        if (sortAscending) sorted else sorted.reversed()
    }

    val selectedVisible = visible.filter { it.image.id in selectedIds }
    val selectedBytes = selectedVisible.sumOf { it.image.sizeBytes }

    Column(Modifier.fillMaxSize()) {
        // Başlık
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "${visible.size} görsel",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    text = "${selectedVisible.size} seçili • ${formatBytes(selectedBytes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = vm::selectAll) { Text("Tümü") }
            TextButton(onClick = vm::clearSelection) { Text("Hiçbiri") }
        }

        ControlsBar(
            sortKey = sortKey,
            sortAscending = sortAscending,
            onSortKey = vm::setSortKey,
            onToggleDir = vm::toggleSortDirection,
            minScore = minScore,
            onMinScore = vm::setMinScoreFilter,
            gridColumns = gridColumns,
            onGridColumns = vm::setGridColumns,
        )

        val cells = if (gridColumns in 1..5) GridCells.Fixed(gridColumns)
        else GridCells.Adaptive(minSize = 110.dp)

        LazyVerticalGrid(
            columns = cells,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(visible, key = { _, c -> c.image.id }) { idx, candidate ->
                CandidateCell(
                    candidate = candidate,
                    selected = candidate.image.id in selectedIds,
                    onToggle = { vm.toggleSelection(candidate.image.id) },
                    onPreview = { previewIndex = idx },
                )
            }
        }

        Button(
            onClick = {
                vm.requestDelete { sender ->
                    deleteLauncher.launch(IntentSenderRequest.Builder(sender).build())
                }
            },
            enabled = selectedVisible.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .height(54.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            ),
        ) {
            Icon(painterResource(R.drawable.ic_delete), contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text(
                text = if (selectedVisible.isEmpty()) "Silinecek görsel seç"
                else "${selectedVisible.size} görseli sil • ${formatBytes(selectedBytes)}",
                fontWeight = FontWeight.Bold,
            )
        }
    }

    previewIndex?.let { start ->
        CandidatePreviewPager(
            candidates = visible,
            startIndex = start.coerceIn(0, (visible.size - 1).coerceAtLeast(0)),
            selectedIds = selectedIds,
            onToggle = { vm.toggleSelection(it) },
            onDismiss = { previewIndex = null },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ControlsBar(
    sortKey: ScanViewModel.SortKey,
    sortAscending: Boolean,
    onSortKey: (ScanViewModel.SortKey) -> Unit,
    onToggleDir: () -> Unit,
    minScore: Int,
    onMinScore: (Int) -> Unit,
    gridColumns: Int,
    onGridColumns: (Int) -> Unit,
) {
    var localScore by remember(minScore) { mutableIntStateOf(minScore) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(12.dp)) {
            // Sıralama
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Sırala", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.size(8.dp))
                FilterChip(
                    selected = sortKey == ScanViewModel.SortKey.SCORE,
                    onClick = { onSortKey(ScanViewModel.SortKey.SCORE) },
                    label = { Text("Puan") },
                )
                Spacer(Modifier.size(6.dp))
                FilterChip(
                    selected = sortKey == ScanViewModel.SortKey.DATE,
                    onClick = { onSortKey(ScanViewModel.SortKey.DATE) },
                    label = { Text("Tarih") },
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onToggleDir) {
                    Text(
                        text = if (sortAscending) "↑" else "↓",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            // Sütun sayısı
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Sütun", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.size(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ColumnChip("Oto", gridColumns == 0) { onGridColumns(0) }
                    (1..5).forEach { n ->
                        ColumnChip(n.toString(), gridColumns == n) { onGridColumns(n) }
                    }
                }
            }

            // Skor filtresi
            Text(
                text = "En düşük skor: %$localScore",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
            Slider(
                value = localScore.toFloat(),
                onValueChange = { localScore = it.roundToInt() },
                onValueChangeFinished = { onMinScore(localScore) },
                valueRange = 0f..100f,
            )
            Text(
                text = "Bu değerin üstündekiler gösterilir ve otomatik seçilir; sil dediğinde yalnız bunlar silinir.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ColumnChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) })
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CandidateCell(
    candidate: Candidate,
    selected: Boolean,
    onToggle: () -> Unit,
    onPreview: () -> Unit,
) {
    val shape = MaterialTheme.shapes.large
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(shape)
            .then(
                if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, shape)
                else Modifier
            )
            // Fotoğrafa dokunmak seçimi açar/kapatır.
            .clickable(onClick = onToggle),
    ) {
        AsyncImage(
            model = candidate.image.uri,
            contentDescription = candidate.image.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        ScoreBadge(
            score = candidate.score,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(6.dp),
        )
        // Seçili göstergesi (yalnız gösterge, dokunma tüm karttan yapılır).
        if (selected) {
            Icon(
                painter = painterResource(R.drawable.ic_check_circle),
                contentDescription = "Seçili",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Color.White),
            )
        }
        // Büyütme (önizleme) butonu.
        IconButton(
            onClick = onPreview,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(4.dp)
                .size(32.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.45f)),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_scan),
                contentDescription = "Önizle",
                tint = Color.White,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun CandidatePreviewPager(
    candidates: List<Candidate>,
    startIndex: Int,
    selectedIds: Set<Long>,
    onToggle: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    if (candidates.isEmpty()) return
    val pagerState = rememberPagerState(initialPage = startIndex) { candidates.size }
    val scope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black.copy(alpha = 0.96f),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    // Sistem çubuklarının (üst durum + alt gezinme tuşları) altında
                    // kalmasın diye insets kadar boşluk bırak.
                    .windowInsetsPadding(WindowInsets.systemBars),
            ) {
                val current = candidates[pagerState.currentPage.coerceIn(0, candidates.size - 1)]
                // Üst çubuk
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 8.dp, top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${pagerState.currentPage + 1} / ${candidates.size}  •  ${current.image.name}",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        maxLines = 1,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(painterResource(R.drawable.ic_close), contentDescription = "Kapat", tint = Color.White)
                    }
                }

                // Kaydırmalı büyük görsel + oklar
                Box(Modifier.fillMaxWidth().weight(1f)) {
                    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                        AsyncImage(
                            model = candidates[page].image.uri,
                            contentDescription = candidates[page].image.name,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    if (pagerState.currentPage > 0) {
                        NavArrow("‹", Modifier.align(Alignment.CenterStart)) {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                        }
                    }
                    if (pagerState.currentPage < candidates.size - 1) {
                        NavArrow("›", Modifier.align(Alignment.CenterEnd)) {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        }
                    }
                }

                // Alt bilgi + seçim
                val selected = current.image.id in selectedIds
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ScoreBadge(score = current.score)
                        Spacer(Modifier.size(8.dp))
                        Text(
                            text = formatBytes(current.image.sizeBytes),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                        )
                    }
                    if (current.matchedKeywords.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            current.matchedKeywords.take(6).forEach { kw ->
                                AssistChip(onClick = {}, label = { Text(kw) })
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = { onToggle(current.image.id) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = if (selected) {
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError,
                            )
                        } else {
                            ButtonDefaults.buttonColors()
                        },
                    ) {
                        Icon(
                            painterResource(
                                if (selected) R.drawable.ic_check_circle else R.drawable.ic_radio_unchecked
                            ),
                            contentDescription = null,
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(if (selected) "Silinecekler listesinde ✓ (çıkar)" else "Silinecekler listesine ekle")
                    }
                }
            }
        }
    }
}

@Composable
private fun NavArrow(symbol: String, modifier: Modifier, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .padding(8.dp)
            .size(44.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.4f)),
    ) {
        Text(symbol, color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun EmptyResults() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = "🧹", style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Henüz sonuç yok",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Ana sayfadan bir tarama başlat; bulunan cuma görselleri burada listelenecek.",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DeletedCelebration(state: ScanState.Deleted, onDismiss: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(text = "🎉", style = MaterialTheme.typography.displayLarge)
            Spacer(Modifier.height(16.dp))
            Card(
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            ) {
                Column(
                    modifier = Modifier
                        .background(Brush.linearGradient(HeroGradient))
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "${state.count} görsel silindi!",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "${formatBytes(state.bytes)} yer kazandın 🌟",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            Button(onClick = onDismiss) { Text("Harika!") }
        }
        ConfettiOverlay(Modifier.fillMaxSize())
    }
}
