package com.hayirlicumalarsil.detection

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.hayirlicumalarsil.data.MediaImage
import kotlinx.coroutines.tasks.await

/** ML Kit ile görseldeki metni okur. Skorlama [FridayScorer]/[buildCandidate] tarafında yapılır. */
class FridayDetector {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /**
     * Görseli okur ve tanınan ham metni döner. Bitmap çözülemezse null,
     * OCR başarısız olursa boş string döner (çağıran ikisini ayırt edebilir:
     * null = dosya okunamadı, "" = metin bulunamadı).
     */
    suspend fun recognizeText(context: Context, image: MediaImage): String? {
        val bitmap = decodeSampled(context, image) ?: return null
        return try {
            recognizer.process(InputImage.fromBitmap(bitmap, 0)).await().text
        } catch (e: Exception) {
            ""
        } finally {
            bitmap.recycle()
        }
    }

    /** OOM riskine karşı görseli en fazla [MAX_DIMENSION] piksel olacak şekilde küçülterek yükler. */
    private fun decodeSampled(context: Context, image: MediaImage): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(image.uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

            var sample = 1
            while (bounds.outWidth / sample > MAX_DIMENSION || bounds.outHeight / sample > MAX_DIMENSION) {
                sample *= 2
            }
            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            context.contentResolver.openInputStream(image.uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            }
        } catch (e: Exception) {
            null
        } catch (e: OutOfMemoryError) {
            null
        }
    }

    private companion object {
        const val MAX_DIMENSION = 1600
    }
}
