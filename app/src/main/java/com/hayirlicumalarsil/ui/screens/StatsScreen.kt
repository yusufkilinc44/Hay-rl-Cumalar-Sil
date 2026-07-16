package com.hayirlicumalarsil.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hayirlicumalarsil.R
import com.hayirlicumalarsil.ScanViewModel
import com.hayirlicumalarsil.formatBytes
import com.hayirlicumalarsil.ui.components.WeeklyBarChart

@Composable
fun StatsScreen(vm: ScanViewModel) {
    val stats by vm.stats.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(20.dp))
        Text(
            text = "İstatistikler 📊",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
        )
        Spacer(Modifier.height(16.dp))

        StatRow {
            StatCard(
                icon = R.drawable.ic_delete,
                value = stats.totalDeleted.toString(),
                label = "Toplam silinen görsel",
                container = MaterialTheme.colorScheme.primaryContainer,
                content = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.weight(1f),
            )
            StatCard(
                icon = R.drawable.ic_savings,
                value = formatBytes(stats.totalBytesFreed),
                label = "Toplam kazanılan alan",
                container = MaterialTheme.colorScheme.tertiaryContainer,
                content = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.weight(1f),
            )
        }
        StatRow {
            StatCard(
                icon = R.drawable.ic_scan,
                value = stats.totalScans.toString(),
                label = "Yapılan tarama",
                container = MaterialTheme.colorScheme.secondaryContainer,
                content = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.weight(1f),
            )
            StatCard(
                icon = R.drawable.ic_photo,
                value = stats.totalScannedFiles.toString(),
                label = "Taranan görsel",
                container = MaterialTheme.colorScheme.surfaceVariant,
                content = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
        }
        StatRow {
            StatCard(
                emoji = "📈",
                value = "${stats.thisWeekDeleted} • ${formatBytes(stats.thisWeekBytes)}",
                label = "Bu hafta silinen",
                container = MaterialTheme.colorScheme.primaryContainer,
                content = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.weight(1f),
            )
            StatCard(
                emoji = "📅",
                value = "${stats.thisMonthDeleted} • ${formatBytes(stats.thisMonthBytes)}",
                label = "Bu ay silinen",
                container = MaterialTheme.colorScheme.secondaryContainer,
                content = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.weight(1f),
            )
        }
        StatRow {
            StatCard(
                emoji = "⚖️",
                value = formatBytes(stats.averageFileBytes),
                label = "Ortalama görsel boyutu",
                container = MaterialTheme.colorScheme.tertiaryContainer,
                content = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.weight(1f),
            )
            StatCard(
                icon = R.drawable.ic_bar_chart,
                value = formatBytes(stats.largestFileBytes),
                label = stats.largestFileName?.let { "En büyük: $it" } ?: "En büyük silinen dosya",
                container = MaterialTheme.colorScheme.surfaceVariant,
                content = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = "Haftalık kazanılan alan",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))
                if (stats.weeklyBytes.all { it.second == 0L }) {
                    Text(
                        text = "Henüz silme yapılmadı. İlk temizlikten sonra grafik burada büyüyecek 🌱",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    WeeklyBarChart(data = stats.weeklyBytes)
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun StatRow(content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

@Composable
private fun StatCard(
    value: String,
    label: String,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
    icon: Int? = null,
    emoji: String? = null,
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = container, contentColor = content),
    ) {
        Column(Modifier.padding(16.dp)) {
            if (icon != null) {
                Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(22.dp))
            } else if (emoji != null) {
                Text(text = emoji, fontSize = 20.sp)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
            )
            Text(text = label, style = MaterialTheme.typography.labelMedium, maxLines = 2)
        }
    }
}
