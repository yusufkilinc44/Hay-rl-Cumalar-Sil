package com.hayirlicumalarsil

import com.hayirlicumalarsil.data.db.DeleteRecord
import com.hayirlicumalarsil.data.db.ScanRecord
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class StatsUi(
    val totalDeleted: Int = 0,
    val totalBytesFreed: Long = 0,
    val totalScans: Int = 0,
    val totalScannedFiles: Int = 0,
    val totalFound: Int = 0,
    val thisWeekDeleted: Int = 0,
    val thisWeekBytes: Long = 0,
    val thisMonthDeleted: Int = 0,
    val thisMonthBytes: Long = 0,
    val largestFileName: String? = null,
    val largestFileBytes: Long = 0,
    val averageFileBytes: Long = 0,
    val lastScanTimestamp: Long? = null,
    val lastScanScanned: Int = 0,
    val lastScanFound: Int = 0,
    /** Son 8 haftanın (etiket, silinen bayt) çiftleri — grafikte kullanılır. */
    val weeklyBytes: List<Pair<String, Long>> = emptyList(),
)

fun buildStats(deletes: List<DeleteRecord>, scans: List<ScanRecord>): StatsUi {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val weekStart = today.with(DayOfWeek.MONDAY)
    val monthStart = today.withDayOfMonth(1)

    fun DeleteRecord.localDate(): LocalDate =
        Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate()

    val thisWeek = deletes.filter { !it.localDate().isBefore(weekStart) }
    val thisMonth = deletes.filter { !it.localDate().isBefore(monthStart) }
    val largest = deletes.maxByOrNull { it.sizeBytes }

    val labelFormat = DateTimeFormatter.ofPattern("d MMM", Locale("tr"))
    val weekly = (7 downTo 0).map { weeksAgo ->
        val start = weekStart.minusWeeks(weeksAgo.toLong())
        val end = start.plusWeeks(1)
        val bytes = deletes
            .filter { val d = it.localDate(); !d.isBefore(start) && d.isBefore(end) }
            .sumOf { it.sizeBytes }
        start.format(labelFormat) to bytes
    }

    val lastScan = scans.firstOrNull()

    return StatsUi(
        totalDeleted = deletes.size,
        totalBytesFreed = deletes.sumOf { it.sizeBytes },
        totalScans = scans.size,
        totalScannedFiles = scans.sumOf { it.scannedCount },
        totalFound = scans.sumOf { it.foundCount },
        thisWeekDeleted = thisWeek.size,
        thisWeekBytes = thisWeek.sumOf { it.sizeBytes },
        thisMonthDeleted = thisMonth.size,
        thisMonthBytes = thisMonth.sumOf { it.sizeBytes },
        largestFileName = largest?.fileName,
        largestFileBytes = largest?.sizeBytes ?: 0,
        averageFileBytes = if (deletes.isEmpty()) 0 else deletes.sumOf { it.sizeBytes } / deletes.size,
        lastScanTimestamp = lastScan?.timestamp,
        lastScanScanned = lastScan?.scannedCount ?: 0,
        lastScanFound = lastScan?.foundCount ?: 0,
        weeklyBytes = weekly,
    )
}

fun formatBytes(bytes: Long): String {
    val kb = 1024.0
    val mb = kb * 1024
    val gb = mb * 1024
    return when {
        bytes >= gb -> String.format(Locale("tr"), "%.2f GB", bytes / gb)
        bytes >= mb -> String.format(Locale("tr"), "%.1f MB", bytes / mb)
        bytes >= kb -> String.format(Locale("tr"), "%.0f KB", bytes / kb)
        else -> "$bytes B"
    }
}
