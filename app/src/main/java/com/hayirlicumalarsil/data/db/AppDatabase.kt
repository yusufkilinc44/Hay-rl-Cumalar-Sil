package com.hayirlicumalarsil.data.db

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "scan_history")
data class ScanRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val scannedCount: Int,
    val foundCount: Int,
)

@Entity(tableName = "delete_history")
data class DeleteRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val fileName: String,
    val sizeBytes: Long,
)

@Dao
interface HistoryDao {
    @Insert
    suspend fun insertScan(record: ScanRecord)

    @Insert
    suspend fun insertDeletes(records: List<DeleteRecord>)

    @Query("SELECT * FROM scan_history ORDER BY timestamp DESC")
    fun scans(): Flow<List<ScanRecord>>

    @Query("SELECT * FROM delete_history ORDER BY timestamp DESC")
    fun deletes(): Flow<List<DeleteRecord>>
}

@Database(
    entities = [ScanRecord::class, DeleteRecord::class, ScannedImageRecord::class],
    version = 3,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
    abstract fun scanCacheDao(): ScanCacheDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        /** v1 → v2: yalnızca yeni scanned_image tablosunu ekler; mevcut istatistikler korunur. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `scanned_image` (
                        `mediaId` INTEGER NOT NULL,
                        `uriString` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `sizeBytes` INTEGER NOT NULL,
                        `dateMillis` INTEGER NOT NULL,
                        `dateModifiedMillis` INTEGER NOT NULL,
                        `recognizedText` TEXT NOT NULL,
                        `scannedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`mediaId`)
                    )
                    """.trimIndent()
                )
            }
        }

        /** v2 → v3: scanned_image tablosuna isWhatsapp sütunu eklenir. */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `scanned_image` ADD COLUMN `isWhatsapp` INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "hcs.db",
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .fallbackToDestructiveMigration()
                    .build().also { instance = it }
            }
    }
}
