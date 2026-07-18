package com.hayirlicumalarsil.detection

import android.net.Uri
import com.hayirlicumalarsil.data.DetectionSettings
import com.hayirlicumalarsil.data.MediaImage
import com.hayirlicumalarsil.data.accepts
import com.hayirlicumalarsil.data.db.ScannedImageRecord

data class Candidate(
    val image: MediaImage,
    val score: Int,
    val matchedKeywords: List<String>,
    val recognizedText: String,
)

/**
 * Cache'teki bir görsel kaydını güncel ayarlara göre değerlendirir. Önce kapsam
 * filtreleri (WhatsApp/boyut/gün — tarama anıyla aynı mantık), sonra skor eşiği
 * uygulanır. Uymuyorsa null döner. OCR gerektirmez; saklı ham metni yeniden
 * puanlar, böylece eşik/kelime değişince tüm geçmiş anında yeniden değerlendirilir.
 */
fun buildCandidate(row: ScannedImageRecord, settings: DetectionSettings): Candidate? {
    // Eski (sütun eklenmeden önce yazılmış) kayıtlarda isWhatsapp=false olabilir;
    // WhatsApp dosya adı desenini de kabul ederek köprü kur.
    val isWhatsapp = row.isWhatsapp || FridayScorer.isWhatsappName(row.name)
    if (!settings.accepts(isWhatsapp, row.sizeBytes, row.dateMillis)) return null

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
