package com.hayirlicumalarsil.ui.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.hayirlicumalarsil.scan.ScanState
import com.hayirlicumalarsil.detection.Candidate
import com.hayirlicumalarsil.formatBytes
import com.hayirlicumalarsil.ui.components.ConfettiOverlay
import com.hayirlicumalarsil.ui.components.ScoreBadge
import com.hayirlicumalarsil.ui.theme.HeroGradient

@Composable
fun ResultsScreen(vm: ScanViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    val selectedIds by vm.selectedIds.collectAsStateWithLifecycle()
    val candidates by vm.candidates.collectAsStateWithLifecycle()
    var detailCandidate by remember { mutableStateOf<Candidate?>(null) }

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

    if (candidates.isEmpty()) {
        EmptyResults()
        return
    }

    val selectedCandidates = candidates.filter { it.image.id in selectedIds }
    val selectedBytes = selectedCandidates.sumOf { it.image.sizeBytes }

    Column(Modifier.fillMaxSize()) {
        // Başlık ve seçim kısayolları
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "${candidates.size} görsel bulundu",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    text = "${selectedCandidates.size} seçili • ${formatBytes(selectedBytes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = vm::selectAll) { Text("Tümü") }
            TextButton(onClick = vm::clearSelection) { Text("Hiçbiri") }
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 110.dp),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(candidates, key = { it.image.id }) { candidate ->
                CandidateCell(
                    candidate = candidate,
                    selected = candidate.image.id in selectedIds,
                    onToggle = { vm.toggleSelection(candidate.image.id) },
                    onDetail = { detailCandidate = candidate },
                )
            }
        }

        // Silme çubuğu
        Button(
            onClick = {
                vm.requestDelete { sender ->
                    deleteLauncher.launch(IntentSenderRequest.Builder(sender).build())
                }
            },
            enabled = selectedCandidates.isNotEmpty(),
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
                text = if (selectedCandidates.isEmpty()) "Silinecek görsel seç"
                else "${selectedCandidates.size} görseli sil • ${formatBytes(selectedBytes)}",
                fontWeight = FontWeight.Bold,
            )
        }
    }

    detailCandidate?.let { candidate ->
        CandidatePreviewDialog(
            candidate = candidate,
            selected = candidate.image.id in selectedIds,
            onToggle = { vm.toggleSelection(candidate.image.id) },
            onDismiss = { detailCandidate = null },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CandidateCell(
    candidate: Candidate,
    selected: Boolean,
    onToggle: () -> Unit,
    onDetail: () -> Unit,
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
            // Dokunma = büyük önizleme; uzun basma = seçimi değiştir.
            .combinedClickable(onClick = onDetail, onLongClick = onToggle),
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
        // Köşedeki işaret kutucuğu doğrudan seçimi açıp kapatır.
        Icon(
            painter = painterResource(
                if (selected) R.drawable.ic_check_circle else R.drawable.ic_radio_unchecked
            ),
            contentDescription = if (selected) "Seçili" else "Seçili değil",
            tint = if (selected) MaterialTheme.colorScheme.primary else Color.White,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .size(28.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.35f))
                .clickable(onClick = onToggle)
                .padding(2.dp),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f))
                    )
                )
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Text(
                text = formatBytes(candidate.image.sizeBytes),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
            )
        }
    }
}

/**
 * Görselin tam ekran (neredeyse tüm ekranı kaplayan) önizlemesi; doğru görseli
 * seçtiğini teyit etmek için. Skor, eşleşen kelimeler ve okunan metin de gösterilir;
 * alttan doğrudan seç/kaldır yapılabilir.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CandidatePreviewDialog(
    candidate: Candidate,
    selected: Boolean,
    onToggle: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(Modifier.fillMaxSize()) {
                // Üst çubuk: dosya adı + kapat
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 8.dp, top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = candidate.image.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(painterResource(R.drawable.ic_close), contentDescription = "Kapat")
                    }
                }

                // Büyük görsel — ekranın çoğunu kaplar
                AsyncImage(
                    model = candidate.image.uri,
                    contentDescription = candidate.image.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color.Black)
                        .clip(MaterialTheme.shapes.medium),
                )

                // Alt bilgi + seçim butonu
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ScoreBadge(score = candidate.score)
                        Spacer(Modifier.size(8.dp))
                        Text(
                            text = "Cuma Skoru • ${formatBytes(candidate.image.sizeBytes)}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    if (candidate.matchedKeywords.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            candidate.matchedKeywords.forEach { keyword ->
                                AssistChip(onClick = {}, label = { Text(keyword) })
                            }
                        }
                    }
                    if (candidate.recognizedText.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Okunan metin:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = candidate.recognizedText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = onToggle,
                        modifier = Modifier.fillMaxWidth(),
                        colors = if (selected) {
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        Text(if (selected) "Silinecekler listesinde ✓" else "Silinecekler listesine ekle")
                    }
                }
            }
        }
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
