package com.hayirlicumalarsil.scan

sealed interface ScanState {
    data object Idle : ScanState
    data class Scanning(val scanned: Int, val total: Int, val found: Int) : ScanState
    data object Results : ScanState
    data class Deleted(val count: Int, val bytes: Long) : ScanState
}
