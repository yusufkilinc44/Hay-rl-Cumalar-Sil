package com.hayirlicumalarsil.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hayirlicumalarsil.BuildConfig
import com.hayirlicumalarsil.ScanViewModel
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(vm: ScanViewModel) {
    val settings by vm.settings.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(20.dp))
        Text(
            text = "Ayarlar ⚙️",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
        )
        Spacer(Modifier.height(16.dp))

        SettingsCard(title = "Tespit hassasiyeti") {
            SettingSlider(
                label = "Tespit eşiği",
                description = "Cuma Skoru bu değerin üzerindeki görseller aday sayılır",
                value = settings.threshold,
                range = 0f..100f,
                onCommit = vm::setThreshold,
            )
            SettingSlider(
                label = "Güçlü kelime puanı",
                description = "\"Hayırlı cumalar\" gibi kesin ifadelerin puanı",
                value = settings.strongWeight,
                range = 0f..100f,
                onCommit = vm::setStrongWeight,
            )
            SettingSlider(
                label = "Zayıf kelime puanı",
                description = "\"dua\", \"amin\" gibi destekleyici kelimelerin puanı (en çok 3 kelime sayılır)",
                value = settings.weakWeight,
                range = 0f..30f,
                onCommit = vm::setWeakWeight,
            )
            SettingSlider(
                label = "Perşembe/Cuma günü bonusu",
                description = "Dosya perşembe veya cuma günü oluştuysa eklenen puan",
                value = settings.dayBonus,
                range = 0f..30f,
                onCommit = vm::setDayBonus,
            )
            SettingSlider(
                label = "WhatsApp dosya adı bonusu",
                description = "IMG-...-WA... adlı dosyalara eklenen puan",
                value = settings.nameBonus,
                range = 0f..30f,
                onCommit = vm::setNameBonus,
            )
        }

        SettingsCard(title = "Tarama kapsamı") {
            SettingSwitch(
                label = "Sadece WhatsApp görselleri",
                description = "Kapalıysa galerideki tüm görseller taranır",
                checked = settings.whatsappOnly,
                onCheckedChange = vm::setWhatsappOnly,
            )
            SettingSwitch(
                label = "Sadece perşembe/cuma dosyaları",
                description = "Diğer günlerde oluşan dosyalar hiç taranmaz (taramayı hızlandırır)",
                checked = settings.thursdayFridayOnly,
                onCheckedChange = vm::setThursdayFridayOnly,
            )
            SettingSlider(
                label = "En küçük dosya boyutu (KB)",
                description = "Bundan küçük dosyalar taranmaz",
                value = settings.minSizeKb,
                range = 0f..500f,
                suffix = " KB",
                onCommit = vm::setMinSizeKb,
            )
        }

        SettingsCard(title = "Güçlü anahtar kelimeler") {
            Text(
                text = "Bu ifadelerden biri görselde okunursa yüksek puan verilir.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            KeywordEditor(
                keywords = settings.strongKeywords,
                onAdd = vm::addStrongKeyword,
                onRemove = vm::removeStrongKeyword,
            )
        }

        SettingsCard(title = "Zayıf anahtar kelimeler") {
            Text(
                text = "Bu kelimeler tek başına yetmez, puanı destekler.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            KeywordEditor(
                keywords = settings.weakKeywords,
                onAdd = vm::addWeakKeyword,
                onRemove = vm::removeWeakKeyword,
            )
        }

        OutlinedButton(
            onClick = vm::resetSettings,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Rounded.RestartAlt, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text("Varsayılan ayarlara dön")
        }

        Spacer(Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(text = "🕌", style = MaterialTheme.typography.headlineLarge)
                Text(
                    text = "Hayırlı Cumalar Sil",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Sürüm ${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Görseller cihazından çıkmaz; tüm analiz telefonda yapılır.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun SettingSlider(
    label: String,
    description: String,
    value: Int,
    range: ClosedFloatingPointRange<Float>,
    onCommit: (Int) -> Unit,
    suffix: String = "",
) {
    var local by remember(value) { mutableFloatStateOf(value.toFloat()) }
    Column(Modifier.padding(vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${local.roundToInt()}$suffix",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Slider(
            value = local,
            onValueChange = { local = it },
            onValueChangeFinished = { onCommit(local.roundToInt()) },
            valueRange = range,
        )
    }
}

@Composable
private fun SettingSwitch(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun KeywordEditor(
    keywords: Set<String>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
) {
    var newWord by remember { mutableStateOf("") }

    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        keywords.sorted().forEach { keyword ->
            InputChip(
                selected = false,
                onClick = { onRemove(keyword) },
                label = { Text(keyword) },
                trailingIcon = {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "$keyword kelimesini kaldır",
                        modifier = Modifier.size(16.dp),
                    )
                },
            )
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = newWord,
            onValueChange = { newWord = it },
            label = { Text("Yeni kelime ekle") },
            modifier = Modifier.weight(1f),
            singleLine = true,
        )
        IconButton(
            onClick = {
                if (newWord.isNotBlank()) {
                    onAdd(newWord)
                    newWord = ""
                }
            },
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "Ekle")
        }
    }
}
