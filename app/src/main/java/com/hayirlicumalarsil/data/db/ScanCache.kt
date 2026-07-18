package com.hayirlicumalarsil.data.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * Taranmış her görselin kalıcı kaydı. Pahalı adım OCR olduğu için skor değil
 * **ham OCR metni** saklanır; skor/aday durumu okuma anında güncel ayarlara göre
 * hesaplanır (eşik/kelime değişince yeniden tarama gerekmez). [dateModifiedMillis]
 * aynı [mediaId] için dosya değişmişse yeniden OCR'ı tetikler.
 */
@Entity(tableName = "scanned_image")
data class ScannedImageRecord(
    @PrimaryKey val mediaId: Long,
    val uriString: String,
    val name: String,
    val sizeBytes: Long,
    val dateMillis: Long,
    val dateModifiedMillis: Long,
    val recognizedText: String,
    val scannedAt: Long,
    val isWhatsapp: Boolean = false,
)

@Dao
interface ScanCacheDao {
    @Query("SELECT * FROM scanned_image")
    fun observeAll(): Flow<List<ScannedImageRecord>>

    @Query("SELECT * FROM scanned_image")
    suspend fun allOnce(): List<ScannedImageRecord>

    @Upsert
    suspend fun upsert(record: ScannedImageRecord)

    @Query("DELETE FROM scanned_image WHERE mediaId IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM scanned_image")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM scanned_image")
    fun countFlow(): Flow<Int>
}
