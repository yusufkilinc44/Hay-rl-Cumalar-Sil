package com.hayirlicumalarsil.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId

data class MediaImage(
    val id: Long,
    val uri: Uri,
    val name: String,
    val sizeBytes: Long,
    val dateMillis: Long,
    val path: String,
    val dateModifiedMillis: Long = 0L,
)

fun MediaImage.dayOfWeek(): DayOfWeek =
    Instant.ofEpochMilli(dateMillis).atZone(ZoneId.systemDefault()).dayOfWeek

/** MediaStore üzerinden cihazdaki görselleri listeler. */
class MediaScanner(private val context: Context) {

    fun loadImages(settings: DetectionSettings): List<MediaImage> {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.DATA,
        )
        val images = mutableListOf<MediaImage>()
        val cursor = context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            "${MediaStore.Images.Media.DATE_ADDED} DESC",
        ) ?: return emptyList()

        cursor.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val sizeCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val addedCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val takenCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            val modifiedCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
            val dataCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)

            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                val name = c.getString(nameCol) ?: continue
                val size = c.getLong(sizeCol)
                val taken = c.getLong(takenCol)
                val dateMillis = if (taken > 0) taken else c.getLong(addedCol) * 1000L
                val path = c.getString(dataCol) ?: ""

                val image = MediaImage(
                    id = id,
                    uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id),
                    name = name,
                    sizeBytes = size,
                    dateMillis = dateMillis,
                    path = path,
                    dateModifiedMillis = c.getLong(modifiedCol),
                )
                if (passesFilters(image, settings)) images += image
            }
        }
        return images
    }

    private fun passesFilters(image: MediaImage, settings: DetectionSettings): Boolean {
        if (settings.whatsappOnly &&
            !image.path.contains("whatsapp images", ignoreCase = true)
        ) return false

        if (image.sizeBytes < settings.minSizeKb * 1024L) return false

        if (settings.thursdayFridayOnly) {
            val day = image.dayOfWeek()
            if (day != DayOfWeek.THURSDAY && day != DayOfWeek.FRIDAY) return false
        }
        return true
    }
}
