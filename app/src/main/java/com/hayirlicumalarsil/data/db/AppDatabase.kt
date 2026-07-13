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

@Database(entities = [ScanRecord::class, DeleteRecord::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "hcs.db",
                ).build().also { instance = it }
            }
    }
}
