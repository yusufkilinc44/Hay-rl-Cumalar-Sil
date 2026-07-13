package com.hayirlicumalarsil.detection

import com.hayirlicumalarsil.data.DetectionSettings
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId

data class ScoreResult(
    val score: Int,
    val matchedKeywords: List<String>,
)

/**
 * Saf puanlama mantığı — OCR'dan bağımsızdır, birim testlerle doğrulanır.
 *
 * Puan bileşenleri (hepsi Ayarlar'dan değiştirilebilir):
 *  - İlk güçlü anahtar kelime eşleşmesi: [DetectionSettings.strongWeight] puan,
 *    her ek güçlü eşleşme +10 puan
 *  - Zayıf anahtar kelime eşleşmeleri: her biri [DetectionSettings.weakWeight] puan (en çok 3 adet sayılır)
 *  - Dosya perşembe/cuma günü oluşmuşsa: [DetectionSettings.dayBonus] puan
 *  - WhatsApp dosya adı deseni (IMG-yyyyMMdd-WAxxxx): [DetectionSettings.nameBonus] puan
 * Sonuç 0–100 aralığına sıkıştırılır.
 */
object FridayScorer {

    private val WHATSAPP_NAME_REGEX = Regex("IMG-\\d{8}-WA\\d+.*", RegexOption.IGNORE_CASE)
    private const val EXTRA_STRONG_MATCH_BONUS = 10
    private const val MAX_COUNTED_WEAK_MATCHES = 3

    fun score(
        rawText: String,
        fileName: String,
        dateMillis: Long,
        settings: DetectionSettings,
    ): ScoreResult {
        val normalizedText = TextNormalizer.normalize(rawText)
        val matched = mutableListOf<String>()
        var score = 0

        if (normalizedText.isNotBlank()) {
            var strongCount = 0
            for (keyword in settings.strongKeywords) {
                val needle = TextNormalizer.normalize(keyword)
                if (needle.isNotBlank() && normalizedText.contains(needle)) {
                    strongCount++
                    matched += keyword
                }
            }
            if (strongCount > 0) {
                score += settings.strongWeight + (strongCount - 1) * EXTRA_STRONG_MATCH_BONUS
            }

            var weakCount = 0
            for (keyword in settings.weakKeywords) {
                val needle = TextNormalizer.normalize(keyword)
                if (needle.isNotBlank() && containsWord(normalizedText, needle)) {
                    matched += keyword
                    if (weakCount < MAX_COUNTED_WEAK_MATCHES) weakCount++
                }
            }
            score += weakCount * settings.weakWeight
        }

        val day = Instant.ofEpochMilli(dateMillis).atZone(ZoneId.systemDefault()).dayOfWeek
        if (day == DayOfWeek.THURSDAY || day == DayOfWeek.FRIDAY) {
            score += settings.dayBonus
        }

        if (WHATSAPP_NAME_REGEX.matches(fileName)) {
            score += settings.nameBonus
        }

        return ScoreResult(score.coerceIn(0, 100), matched)
    }

    /**
     * Zayıf kelimeler tam kelime olarak aranır; "cuma" kelimesinin "cumartesi"
     * içinde eşleşmesini engeller. Çok kelimeli ifadeler alt dizi olarak aranır.
     */
    private fun containsWord(text: String, needle: String): Boolean {
        if (needle.contains(' ')) return text.contains(needle)
        return text.split(' ').any { it == needle }
    }
}
