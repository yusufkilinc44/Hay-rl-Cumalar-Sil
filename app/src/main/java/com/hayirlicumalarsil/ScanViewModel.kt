package com.hayirlicumalarsil

import android.app.Application
import android.content.Intent
import android.content.IntentSender
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hayirlicumalarsil.data.DetectionSettings
import com.hayirlicumalarsil.data.SettingsRepository
import com.hayirlicumalarsil.data.fingerprint
import com.hayirlicumalarsil.data.db.AppDatabase
import com.hayirlicumalarsil.data.db.DeleteRecord
import com.hayirlicumalarsil.detection.Candidate
import com.hayirlicumalarsil.detection.buildCandidate
import com.hayirlicumalarsil.scan.ScanEngine
import com.hayirlicumalarsil.scan.ScanService
import com.hayirlicumalarsil.scan.ScanState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ScanViewModel(app: Application) : AndroidViewModel(app) {

    private val settingsRepo = SettingsRepository(app)
    private val db = AppDatabase.get(app)
    private val dao = db.historyDao()
    private val scanCacheDao = db.scanCacheDao()

    val settings = settingsRepo.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, DetectionSettings())

    /** "Silme tamamlandı" kutlaması gibi yalnızca UI'a ait geçici durum. */
    private val _localOverlay = MutableStateFlow<ScanState?>(null)

    /** Tarama motorunun durumu ile UI-overlay'in birleşimi. */
    val state = combine(ScanEngine.state, _localOverlay) { engine, overlay ->
        overlay ?: engine
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ScanState.Idle)

    /**
     * Adaylar Room'dan türetiliyor: tarama sırasında canlı güncellenir ve
     * eşik/kelime ayarı değişince yeniden skorlanır.
     *
     * PERFORMANS: Ağır skorlama (her kayıt için metin normalizasyonu) UI
     * thread'ini kastırmasın diye [Dispatchers.Default]'a taşınır. Tarama
     * sırasında Room her görselde emit ettiğinden [conflate] ile patlamalar
     * birleştirilir — hesap meşgulken araya giren emisyonlar atlanır, yalnız
     * en güncel durum işlenir. Böylece tarama arka planda sürerken menüler
     * akıcı kalır.
     */
    val candidates = combine(scanCacheDao.observeAll(), settings) { rows, s -> rows to s }
        .conflate()
        .map { (rows, s) -> rows.mapNotNull { buildCandidate(it, s) }.sortedByDescending { it.score } }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds = _selectedIds.asStateFlow()

    val stats = combine(dao.deletes(), dao.scans()) { deletes, scans ->
        buildStats(deletes, scans)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatsUi())

    val cacheCount = scanCacheDao.countFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    /** Son taramadan bu yana sonucu etkileyen bir ayar değişti mi? */
    val settingsChangedSinceLastScan = combine(
        settings,
        settingsRepo.lastScanFingerprint,
    ) { s, last -> last != null && last != s.fingerprint() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val themeDark = settingsRepo.themeDark
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val onboardingDone = settingsRepo.onboardingDone
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    fun completeOnboarding() = launchSetting { settingsRepo.setOnboardingDone() }

    /** Sonuçlar ızgarası sütun sayısı; 0 = otomatik. */
    val gridColumns = settingsRepo.gridColumns
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    /** Sonuçlar ekranı sıralaması. */
    enum class SortKey { SCORE, DATE }

    private val _sortKey = MutableStateFlow(SortKey.SCORE)
    val sortKey = _sortKey.asStateFlow()
    private val _sortAscending = MutableStateFlow(false)
    val sortAscending = _sortAscending.asStateFlow()

    /** Sonuçlar ekranında gösterilecek minimum skor (yüzde). */
    private val _minScoreFilter = MutableStateFlow(0)
    val minScoreFilter = _minScoreFilter.asStateFlow()

    private var pendingDelete: List<Candidate> = emptyList()

    init {
        // Silme/eşik değişikliğiyle listeden düşen adayların seçimini temizle
        // (kullanıcının kalan seçimleri korunur, ekranlar arası kaybolmaz).
        candidates.onEach { list ->
            val ids = list.map { it.image.id }.toSet()
            _selectedIds.value = _selectedIds.value intersect ids
        }.launchIn(viewModelScope)

        // Yeni bir tarama tamamlandığında görünür adayların hepsini seçili yap.
        var wasScanning = false
        ScanEngine.state.onEach { s ->
            if (s is ScanState.Scanning) {
                wasScanning = true
            } else if (wasScanning && s is ScanState.Results) {
                wasScanning = false
                selectVisible()
            }
        }.launchIn(viewModelScope)
    }

    /** Filtreyi geçen (skor >= minScoreFilter) adaylar. */
    private fun visibleNow(): List<Candidate> =
        candidates.value.filter { it.score >= _minScoreFilter.value }

    /** Görünür (filtreyi geçen) tüm adayları seçili yapar. */
    fun selectVisible() {
        _selectedIds.value = visibleNow().map { it.image.id }.toSet()
    }

    /** Skor filtresi değişince, görünürlerin tümü otomatik seçilir. */
    fun setMinScoreFilter(v: Int) {
        _minScoreFilter.value = v
        selectVisible()
    }

    /** Sadece daha önce taranmamış (veya değişmiş) görselleri tarar. */
    fun startScan() {
        if (ScanEngine.isScanning) return
        _localOverlay.value = null
        ContextCompat.startForegroundService(
            getApplication(),
            Intent(getApplication(), ScanService::class.java),
        )
    }

    /** Önbelleği temizleyip her görseli baştan tarar (ayarlar değiştiğinde). */
    fun rescanAll() {
        if (ScanEngine.isScanning) return
        viewModelScope.launch {
            scanCacheDao.clearAll()
            startScan()
        }
    }

    fun cancelScan() {
        ScanEngine.cancel()
    }

    fun setSortKey(key: SortKey) { _sortKey.value = key }
    fun toggleSortDirection() { _sortAscending.value = !_sortAscending.value }

    fun toggleSelection(id: Long) {
        _selectedIds.value =
            if (id in _selectedIds.value) _selectedIds.value - id else _selectedIds.value + id
    }

    /** "Tümü" = filtreyi geçen görünür adayların tümü. */
    fun selectAll() = selectVisible()

    fun clearSelection() {
        _selectedIds.value = emptySet()
    }

    fun selectedCandidates(): List<Candidate> =
        candidates.value.filter { it.image.id in _selectedIds.value }

    fun setThemeDark(v: Boolean) = launchSetting { settingsRepo.setThemeDark(v) }
    fun setGridColumns(v: Int) = launchSetting { settingsRepo.setGridColumns(v) }

    /** Önbelleği temizler; sonraki tarama her görseli baştan OCR'lar. */
    fun clearScanCache() {
        viewModelScope.launch { scanCacheDao.clearAll() }
    }

    /**
     * Silme akışını başlatır. Android 11+ üzerinde sistem onay penceresi için
     * [IntentSender] üretir; eski sürümlerde doğrudan siler.
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
        // Silinen görseller önbellekten de düşer; reaktif candidates akışı kendini günceller.
        val deletedIds = deleted.map { it.image.id }
        scanCacheDao.deleteByIds(deletedIds)
        _selectedIds.value = _selectedIds.value - deletedIds.toSet()
        _localOverlay.value = ScanState.Deleted(deleted.size, deleted.sumOf { it.image.sizeBytes })
    }

    fun dismissDeletedCelebration() {
        _localOverlay.value = null
    }

    // --- Ayar güncellemeleri ---

    fun setThreshold(v: Int) = launchSetting { settingsRepo.setThreshold(v) }
    fun setStrongWeight(v: Int) = launchSetting { settingsRepo.setStrongWeight(v) }
    fun setWeakWeight(v: Int) = launchSetting { settingsRepo.setWeakWeight(v) }
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
