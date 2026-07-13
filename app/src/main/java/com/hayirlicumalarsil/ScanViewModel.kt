package com.hayirlicumalarsil

import android.app.Application
import android.content.IntentSender
import android.os.Build
import android.provider.MediaStore
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hayirlicumalarsil.data.DetectionSettings
import com.hayirlicumalarsil.data.MediaScanner
import com.hayirlicumalarsil.data.SettingsRepository
import com.hayirlicumalarsil.data.db.AppDatabase
import com.hayirlicumalarsil.data.db.DeleteRecord
import com.hayirlicumalarsil.data.db.ScanRecord
import com.hayirlicumalarsil.detection.Candidate
import com.hayirlicumalarsil.detection.FridayDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface ScanState {
    data object Idle : ScanState
    data class Scanning(val scanned: Int, val total: Int, val found: Int) : ScanState
    data object Results : ScanState
    data class Deleted(val count: Int, val bytes: Long) : ScanState
}

class ScanViewModel(app: Application) : AndroidViewModel(app) {

    private val settingsRepo = SettingsRepository(app)
    private val dao = AppDatabase.get(app).historyDao()
    private val scanner = MediaScanner(app)
    private val detector = FridayDetector()

    val settings = settingsRepo.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, DetectionSettings())

    private val _state = MutableStateFlow<ScanState>(ScanState.Idle)
    val state = _state.asStateFlow()

    /** Son taramada eşiği geçen adaylar (puana göre sıralı). */
    val candidates = mutableStateListOf<Candidate>()

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds = _selectedIds.asStateFlow()

    val stats = combine(dao.deletes(), dao.scans()) { deletes, scans ->
        buildStats(deletes, scans)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatsUi())

    private var scanJob: Job? = null
    private var pendingDelete: List<Candidate> = emptyList()

    fun startScan() {
        if (_state.value is ScanState.Scanning) return
        scanJob = viewModelScope.launch {
            val activeSettings = settingsRepo.settings.first()
            _state.value = ScanState.Scanning(0, 0, 0)

            val images = withContext(Dispatchers.IO) { scanner.loadImages(activeSettings) }
            _state.value = ScanState.Scanning(0, images.size, 0)

            val found = mutableListOf<Candidate>()
            images.forEachIndexed { index, image ->
                val candidate = withContext(Dispatchers.IO) {
                    detector.analyze(getApplication(), image, activeSettings)
                }
                if (candidate != null && candidate.score >= activeSettings.threshold) {
                    found += candidate
                }
                _state.value = ScanState.Scanning(index + 1, images.size, found.size)
            }

            found.sortByDescending { it.score }
            candidates.clear()
            candidates.addAll(found)
            _selectedIds.value = found.map { it.image.id }.toSet()

            dao.insertScan(
                ScanRecord(
                    timestamp = System.currentTimeMillis(),
                    scannedCount = images.size,
                    foundCount = found.size,
                )
            )
            _state.value = ScanState.Results
        }
    }

    fun cancelScan() {
        scanJob?.cancel()
        scanJob = null
        _state.value = if (candidates.isEmpty()) ScanState.Idle else ScanState.Results
    }

    fun toggleSelection(id: Long) {
        _selectedIds.value =
            if (id in _selectedIds.value) _selectedIds.value - id else _selectedIds.value + id
    }

    fun selectAll() {
        _selectedIds.value = candidates.map { it.image.id }.toSet()
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
    }

    fun selectedCandidates(): List<Candidate> =
        candidates.filter { it.image.id in _selectedIds.value }

    /**
     * Silme akışını başlatır. Android 11+ üzerinde sistem onay penceresi için
     * [IntentSender] üretir ve [onIntentSender] ile arayüze iletir; eski
     * sürümlerde doğrudan siler.
     */
    fun requestDelete(onIntentSender: (IntentSender) -> Unit) {
        val selection = selectedCandidates()
        if (selection.isEmpty()) return
        pendingDelete = selection

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val pendingIntent = MediaStore.createDeleteRequest(
                getApplication<Application>().contentResolver,
                selection.map { it.image.uri },
            )
            onIntentSender(pendingIntent.intentSender)
        } else {
            viewModelScope.launch {
                val resolver = getApplication<Application>().contentResolver
                val deleted = withContext(Dispatchers.IO) {
                    selection.filter { candidate ->
                        try {
                            resolver.delete(candidate.image.uri, null, null) > 0
                        } catch (e: SecurityException) {
                            false
                        }
                    }
                }
                finalizeDelete(deleted)
            }
        }
    }

    /** Sistem onay penceresinde kullanıcı "Sil" dediğinde çağrılır. */
    fun onSystemDeleteApproved() {
        viewModelScope.launch { finalizeDelete(pendingDelete) }
    }

    fun onSystemDeleteDenied() {
        pendingDelete = emptyList()
    }

    private suspend fun finalizeDelete(deleted: List<Candidate>) {
        pendingDelete = emptyList()
        if (deleted.isEmpty()) return

        val now = System.currentTimeMillis()
        dao.insertDeletes(
            deleted.map {
                DeleteRecord(timestamp = now, fileName = it.image.name, sizeBytes = it.image.sizeBytes)
            }
        )
        val deletedIds = deleted.map { it.image.id }.toSet()
        candidates.removeAll { it.image.id in deletedIds }
        _selectedIds.value = _selectedIds.value - deletedIds
        _state.value = ScanState.Deleted(deleted.size, deleted.sumOf { it.image.sizeBytes })
    }

    fun dismissDeletedCelebration() {
        _state.value = if (candidates.isEmpty()) ScanState.Idle else ScanState.Results
    }

    // --- Ayar güncellemeleri ---

    fun setThreshold(v: Int) = launchSetting { settingsRepo.setThreshold(v) }
    fun setStrongWeight(v: Int) = launchSetting { settingsRepo.setStrongWeight(v) }
    fun setWeakWeight(v: Int) = launchSetting { settingsRepo.setWeakWeight(v) }
    fun setDayBonus(v: Int) = launchSetting { settingsRepo.setDayBonus(v) }
    fun setNameBonus(v: Int) = launchSetting { settingsRepo.setNameBonus(v) }
    fun setMinSizeKb(v: Int) = launchSetting { settingsRepo.setMinSizeKb(v) }
    fun setWhatsappOnly(v: Boolean) = launchSetting { settingsRepo.setWhatsappOnly(v) }
    fun setThursdayFridayOnly(v: Boolean) = launchSetting { settingsRepo.setThursdayFridayOnly(v) }
    fun addStrongKeyword(word: String) = launchSetting { settingsRepo.addStrongKeyword(word) }
    fun removeStrongKeyword(word: String) = launchSetting { settingsRepo.removeStrongKeyword(word) }
    fun addWeakKeyword(word: String) = launchSetting { settingsRepo.addWeakKeyword(word) }
    fun removeWeakKeyword(word: String) = launchSetting { settingsRepo.removeWeakKeyword(word) }
    fun resetSettings() = launchSetting { settingsRepo.resetToDefaults() }

    private fun launchSetting(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
