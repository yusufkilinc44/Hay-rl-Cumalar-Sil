package com.hayirlicumalarsil.detection

import com.hayirlicumalarsil.data.DetectionSettings

data class ScoreResult(
    val score: Int,
    val matchedKeywords: List<String>,
)

/**
 * Saf, metin-tabanlı puanlama — OCR'dan bağımsızdır, birim testlerle doğrulanır.
 *
 * Kural: bir görselin aday olması için MUTLAKA en az bir güçlü ifade
 * ("hayırlı cumalar" vb.) içermesi gerekir. Yalnızca zayıf kelime taşıyanlar
 * (karikatür, sıradan foto vb.) elenir. Gün/dosya-adı bonusu yoktur; WhatsApp ve
 * perşembe/cuma yalnızca tarama kapsamı filtreleridir.
 *
 * Puan:
 *  - İlk güçlü eşleşme: [DetectionSettings.strongWeight]; her ek güçlü eşleşme +10
 *  - Zayıf eşleşmeler: her biri [DetectionSettings.weakWeight] (en çok 3 sayılır)
 * Sonuç 0–100 aralığına sıkıştırılır.
 */
object FridayScorer {

    val WHATSAPP_NAME_REGEX = Regex("IMG-\\d{8}-WA\\d+.*", RegexOption.IGNORE_CASE)
    private const val EXTRA_STRONG_MATCH_BONUS = 10
    private const val MAX_COUNTED_WEAK_MATCHES = 3

    fun isWhatsappName(fileName: String): Boolean = WHATSAPP_NAME_REGEX.matches(fileName)

    fun score(rawText: String, settings: DetectionSettings): ScoreResult {
        val normalizedText = TextNormalizer.normalize(rawText)
        if (normalizedText.isBlank()) return ScoreResult(0, emptyList())

        val matched = mutableListOf<String>()
        var strongCount = 0
        for (keyword in settings.strongKeywords) {
            val needle = TextNormalizer.normalize(keyword)
            if (needle.isNotBlank() && normalizedText.contains(needle)) {
                strongCount++
                matched += keyword
            }
        }

        // Güçlü ifade yoksa cuma kutlaması değildir → elenir.
        if (strongCount == 0) return ScoreResult(0, emptyList())

        var score = settings.strongWeight + (strongCount - 1) * EXTRA_STRONG_MATCH_BONUS

        var weakCount = 0
        for (keyword in settings.weakKeywords) {
            val needle = TextNormalizer.normalize(keyword)
            if (needle.isNotBlank() && containsWord(normalizedText, needle)) {
                matched += keyword
                if (weakCount < MAX_COUNTED_WEAK_MATCHES) weakCount++
            }
        }
        score += weakCount * settings.weakWeight

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
