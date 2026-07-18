package com.hayirlicumalarsil.detection

import android.net.Uri
import com.hayirlicumalarsil.data.DetectionSettings
import com.hayirlicumalarsil.data.MediaImage
import com.hayirlicumalarsil.data.db.ScannedImageRecord

data class Candidate(
    val image: MediaImage,
    val score: Int,
    val matchedKeywords: List<String>,
    val recognizedText: String,
)

/**
 * Cache'teki bir görsel kaydını güncel ayarlara göre skorlar. Eşiği geçemezse
 * null döner. OCR gerektirmez — yalnızca saklı ham metni yeniden puanlar, bu yüzden
 * eşik/kelime değişince tüm geçmiş anında yeniden değerlendirilebilir.
 */
fun buildCandidate(row: ScannedImageRecord, settings: DetectionSettings): Candidate? {
    val result = FridayScorer.score(row.recognizedText, row.name, row.dateMillis, settings)
    if (result.score < settings.threshold) return null
    return Candidate(
        image = MediaImage(
            id = row.mediaId,
            uri = Uri.parse(row.uriString),
            name = row.name,
            sizeBytes = row.sizeBytes,
            dateMillis = row.dateMillis,
            path = "",
            dateModifiedMillis = row.dateModifiedMillis,
        ),
        score = result.score,
        matchedKeywords = result.matchedKeywords,
        recognizedText = row.recognizedText.take(600),
    )
}
