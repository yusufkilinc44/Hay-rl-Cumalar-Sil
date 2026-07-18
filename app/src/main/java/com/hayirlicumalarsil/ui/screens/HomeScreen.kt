package com.hayirlicumalarsil.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.hayirlicumalarsil.R
import com.hayirlicumalarsil.ScanViewModel
import com.hayirlicumalarsil.StatsUi
import com.hayirlicumalarsil.detection.Candidate
import com.hayirlicumalarsil.formatBytes
import com.hayirlicumalarsil.scan.ScanState
import com.hayirlicumalarsil.ui.theme.HeroGradient
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun mediaPermissions(): Array<String> = when {
    Build.VERSION.SDK_INT >= 34 -> arrayOf(
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
    )
    Build.VERSION.SDK_INT >= 33 -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
    else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
}

private fun requestedPermissions(): Array<String> {
    val perms = mediaPermissions().toMutableList()
    if (Build.VERSION.SDK_INT >= 33) perms += Manifest.permission.POST_NOTIFICATIONS
    return perms.toTypedArray()
}

private fun hasImagePermission(context: Context): Boolean =
    mediaPermissions().any {
        context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
    }

@Composable
fun HomeScreen(vm: ScanViewModel, onGoResults: () -> Unit) {
    val context = LocalContext.current
    val state by vm.state.collectAsStateWithLifecycle()
    val stats by vm.stats.collectAsStateWithLifecycle()
    val candidates by vm.candidates.collectAsStateWithLifecycle()
    val settingsChanged by vm.settingsChangedSinceLastScan.collectAsStateWithLifecycle()

    var showFirstScanNotice by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (mediaPermissions().any { grants[it] == true }) vm.startScan()
    }
    fun startWithPermission() {
        if (hasImagePermission(context)) vm.startScan()
        else permissionLauncher.launch(requestedPermissions())
    }
    // İlk taramada uzun süreceği uyarısını göster; sonrakiler hızlı olacak.
    fun onTaraClick() {
        if (stats.totalScans == 0) showFirstScanNotice = true else startWithPermission()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Hayırlı Cumalar Sil",
            style = MaterialTheme.typography.headlineMedium.copy(
                brush = Brush.linearGradient(HeroGradient),
                fontWeight = FontWeight.ExtraBold,
            ),
        )
        Text(
            text = "Cuma kutlama görsellerini bul, onayla, sil ✨",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(14.dp))

        // Özet kart
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.linearGradient(HeroGradient))
                    .padding(vertical = 18.dp, horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                HeroStat(icon = R.drawable.ic_delete, valueText = stats.totalDeleted.toString(), label = "Toplam silinen")
                HeroStat(icon = R.drawable.ic_storage, valueText = formatBytes(stats.totalBytesFreed), label = "Kazanılan alan")
            }
        }

        Spacer(Modifier.height(14.dp))

        if (state is ScanState.Scanning) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                ScanningPanel(state as ScanState.Scanning, onCancel = vm::cancelScan)
            }
        } else {
            ScanButton(onClick = ::onTaraClick)
            Spacer(Modifier.height(12.dp))

            if (settingsChanged) {
                SettingsChangedBanner(
                    onScanAll = {
                        if (hasImagePermission(context)) vm.rescanAll()
                        else permissionLauncher.launch(requestedPermissions())
                    },
                    onScanNew = ::startWithPermission,
                )
                Spacer(Modifier.height(12.dp))
            }

            ResultsPreviewCard(
                modifier = Modifier.fillMaxWidth().weight(1f),
                candidates = candidates,
                stats = stats,
                onGoResults = onGoResults,
            )
            Spacer(Modifier.height(12.dp))
        }
    }

    if (showFirstScanNotice) {
        AlertDialog(
            onDismissRequest = { showFirstScanNotice = false },
            title = { Text("İlk tarama biraz sürebilir ⏳") },
            text = {
                Text(
                    "İlk taramada tüm görseller ilk kez okunacağı için işlem biraz uzun " +
                        "sürebilir. Merak etme: sonraki taramalar çok daha hızlı olacak, çünkü " +
                        "daha önce taranan görseller tekrar taranmaz."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showFirstScanNotice = false
                    startWithPermission()
                }) { Text("Taramayı başlat") }
            },
            dismissButton = {
                TextButton(onClick = { showFirstScanNotice = false }) { Text("Vazgeç") }
            },
        )
    }
}

@Composable
private fun HeroStat(icon: Int, label: String, valueText: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(painterResource(icon), contentDescription = null, tint = Color.White)
        Spacer(Modifier.height(6.dp))
        Text(
            text = valueText,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
        )
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = Color.White)
    }
}

@Composable
private fun SettingsChangedBanner(onScanAll: () -> Unit, onScanNew: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = "⚙️ Ayarları değiştirdin",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                text = "Son taramadan bu yana ayarlar değişti. Daha önce taranan görsellerde " +
                    "yeni ayarlara göre aday çıkabilir.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onScanAll, modifier = Modifier.weight(1f)) {
                    Text("Tümünü tara")
                }
                OutlinedButton(onClick = onScanNew, modifier = Modifier.weight(1f)) {
                    Text("Sadece yeniler")
                }
            }
        }
    }
}

@Composable
private fun ResultsPreviewCard(
    modifier: Modifier,
    candidates: List<Candidate>,
    stats: StatsUi,
    onGoResults: () -> Unit,
) {
    val dateFormat = remember { SimpleDateFormat("d MMM yyyy", Locale("tr")) }
    Card(
        modifier = modifier.clickable(enabled = candidates.isNotEmpty(), onClick = onGoResults),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
        ) {
            Text(
                text = if (candidates.isEmpty()) "Henüz aday yok" else "${candidates.size} aday görsel bulundu",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            if (candidates.isNotEmpty()) {
                Text(
                    text = "${formatBytes(candidates.sumOf { it.image.sizeBytes })} yer kazanabilirsin",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }

            // Taranan tarih aralığı
            val start = stats.firstScanTimestamp
            val end = stats.lastScanTimestamp
            if (start != null && end != null) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.12f),
                ) {
                    Text(
                        text = "🗓 Tarandı: ${dateFormat.format(Date(start))} – ${dateFormat.format(Date(end))}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            if (candidates.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Yukarıdaki TARA düğmesine basarak cuma görsellerini aramaya başla 🔍",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                Text(
                    text = "En son bulunanlar",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                Spacer(Modifier.height(8.dp))
                // En son taranan birkaç aday küçük önizleme
                val recent = remember(candidates) {
                    candidates.sortedByDescending { it.scannedAt }.take(3)
                }
                Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    recent.forEach { c ->
                        AsyncImage(
                            model = c.image.uri,
                            contentDescription = c.image.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .clip(MaterialTheme.shapes.large),
                        )
                    }
                    repeat(3 - recent.size) { Spacer(Modifier.weight(1f)) }
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onGoResults,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("İncele ve sil", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ScanButton(onClick: () -> Unit) {
    val pulse = rememberInfiniteTransition(label = "pulse")
    val ringScale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Restart),
        label = "ringScale",
    )
    val ringAlpha by pulse.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Restart),
        label = "ringAlpha",
    )

    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(150.dp)
                .scale(ringScale)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = ringAlpha)),
        )
        Box(
            modifier = Modifier
                .size(150.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(HeroGradient))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    painterResource(R.drawable.ic_scan),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(46.dp),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "TARA",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        }
    }
}

@Composable
private fun ScanningPanel(state: ScanState.Scanning, onCancel: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (state.total == 0) {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text("Görseller listeleniyor…", color = MaterialTheme.colorScheme.onPrimaryContainer)
            } else {
                Text(
                    text = "${state.scanned} / ${state.total}",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    "görsel tarandı",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = { state.scanned.toFloat() / state.total },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "🕌 ${state.found} cuma görseli adayı bulundu",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                if (state.skipped > 0) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "↩️ ${state.skipped} görsel daha önce tarandığı için atlandı",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onCancel,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) {
                Text("Durdur")
            }
        }
    }
}
