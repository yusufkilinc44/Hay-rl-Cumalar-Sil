package com.hayirlicumalarsil.scan

import android.content.Context
import com.hayirlicumalarsil.data.MediaScanner
import com.hayirlicumalarsil.data.SettingsRepository
import com.hayirlicumalarsil.data.fingerprint
import com.hayirlicumalarsil.data.db.AppDatabase
import com.hayirlicumalarsil.data.db.ScanRecord
import com.hayirlicumalarsil.data.db.ScannedImageRecord
import com.hayirlicumalarsil.detection.FridayDetector
import com.hayirlicumalarsil.detection.FridayScorer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Taramanın asıl beyni; Android Service plumbing'inden bağımsız singleton.
 * Room'a görsel başına ham OCR metni yazar (tek doğruluk kaynağı). Uygulama
 * öldürülse bile bulunanlar Room'da kalır ve zaten taranan görsel yeniden
 * OCR'lanmaz. [ScanViewModel] bu [state]'i dinler.
 */
object ScanEngine {

    private val _state = MutableStateFlow<ScanState>(ScanState.Idle)
    val state: StateFlow<ScanState> = _state.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null

    val isScanning: Boolean get() = _state.value is ScanState.Scanning

    fun start(appContext: Context) {
        if (_state.value is ScanState.Scanning) return
        // Durumu SENKRON olarak Scanning'e al: aksi halde servis, coroutine daha
        // Scanning yazmadan state'i Idle görüp kendini hemen durdurabilir.
        _state.value = ScanState.Scanning(0, 0, 0)
        val detector = FridayDetector()
        job = scope.launch {
            val db = AppDatabase.get(appContext)
            val cacheDao = db.scanCacheDao()
            val settingsRepo = SettingsRepository(appContext)
            val settings = settingsRepo.settings.first()

            // Bu taramanın hangi ayarlarla yapıldığını kaydet; "ayarlar değişti mi"
            // uyarısı bununla karşılaştırılır.
            settingsRepo.setLastScanFingerprint(settings.fingerprint())

            val images = withContext(Dispatchers.IO) { MediaScanner(appContext).loadImages(settings) }
            val cached = withContext(Dispatchers.IO) { cacheDao.allOnce() }.associateBy { it.mediaId }
            _state.value = ScanState.Scanning(0, images.size, 0)

            var found = 0
            images.forEachIndexed { index, image ->
                ensureActive() // işbirlikçi iptal noktası

                val existing = cached[image.id]
                val text = if (existing != null && existing.dateModifiedMillis == image.dateModifiedMillis) {
                    existing.recognizedText
                } else {
                    val recognized = withContext(Dispatchers.IO) {
                        detector.recognizeText(appContext, image)
                    } ?: ""
                    cacheDao.upsert(
                        ScannedImageRecord(
                            mediaId = image.id,
                            uriString = image.uri.toString(),
                            name = image.name,
                            sizeBytes = image.sizeBytes,
                            dateMillis = image.dateMillis,
                            dateModifiedMillis = image.dateModifiedMillis,
                            recognizedText = recognized.take(4000),
                            scannedAt = System.currentTimeMillis(),
                            isWhatsapp = image.isWhatsapp,
                        )
                    )
                    recognized
                }

                if (FridayScorer.score(text, settings).score >= settings.threshold) {
                    found++
                }
                _state.value = ScanState.Scanning(index + 1, images.size, found)

                // CPU'yu sürekli %100'de tutmayıp kısa nefes aralıkları bırak:
                // Samsung "Cihaz bakımı" gibi pil yöneticilerinin "çok kaynak
                // kullanıyor" uyarısını tetiklemesini azaltır. Yalnızca gerçekten
                // OCR yapıldığında (yeni görsel) beklenir; cache'ten okunuyorsa hızlı geçer.
                if (existing == null || existing.dateModifiedMillis != image.dateModifiedMillis) {
                    delay(8)
                }
            }

            db.historyDao().insertScan(
                ScanRecord(
                    timestamp = System.currentTimeMillis(),
                    scannedCount = images.size,
                    foundCount = found,
                )
            )
            _state.value = ScanState.Results
        }
        job?.invokeOnCompletion {
            // İptal ya da hata durumunda Scanning'de takılı kalma; Room zaten
            // o ana kadarki sonuçları içeriyor, UI onları gösterecek.
            if (_state.value is ScanState.Scanning) _state.value = ScanState.Idle
        }
    }

    fun cancel() {
        job?.cancel()
    }
}
