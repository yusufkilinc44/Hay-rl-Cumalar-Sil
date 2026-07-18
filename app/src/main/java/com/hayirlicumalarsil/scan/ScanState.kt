package com.hayirlicumalarsil.scan

sealed interface ScanState {
    data object Idle : ScanState
    data class Scanning(
        val scanned: Int,
        val total: Int,
        val found: Int,
        /** Daha önce tarandığı için OCR'sız geçilen görsel sayısı. */
        val skipped: Int = 0,
    ) : ScanState
    data object Results : ScanState
    data class Deleted(val count: Int, val bytes: Long) : ScanState
}
