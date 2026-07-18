package com.hayirlicumalarsil.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val DEFAULT_STRONG_KEYWORDS = setOf(
    "hayırlı cumalar",
    "hayırlı nurlu cumalar",
    "cumanız mübarek",
    "mübarek cumalar",
    "cumamız mübarek",
    "cuma gününüz mübarek",
    "cumanız hayırlara vesile olsun",
    "hayırlı kandiller",
)

val DEFAULT_WEAK_KEYWORDS = setOf(
    "cuma",
    "dua",
    "amin",
    "bismillah",
    "ayet",
    "hadis",
    "selamün aleyküm",
    "mübarek",
    "allah",
    "rabbim",
)

/**
 * Tespit algoritmasının ayarlanabilir parametreleri.
 *
 * Puanlama yalnızca metin (güçlü + zayıf kelime) üzerinden yapılır; gün/dosya-adı
 * bonusları ve minimum boyut kaldırıldı. Bir görselin aday olması için her zaman
 * en az bir güçlü ifade ("hayırlı cumalar" vb.) gerekir. WhatsApp ve perşembe/cuma
 * yalnızca tarama KAPSAMI filtreleridir, puana etki etmezler.
 */
data class DetectionSettings(
    val threshold: Int = 60,
    val strongWeight: Int = 70,
    val weakWeight: Int = 10,
    val whatsappOnly: Boolean = true,
    val thursdayFridayOnly: Boolean = false,
    val strongKeywords: Set<String> = DEFAULT_STRONG_KEYWORDS,
    val weakKeywords: Set<String> = DEFAULT_WEAK_KEYWORDS,
)

/** Sonuçları etkileyen ayarların imzası — "son taramadan beri değişti mi" için. */
fun DetectionSettings.fingerprint(): String = listOf(
    threshold, strongWeight, weakWeight, whatsappOnly, thursdayFridayOnly,
    strongKeywords.sorted(), weakKeywords.sorted(),
).toString()

private val Context.dataStore by preferencesDataStore(name = "hcs_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val THRESHOLD = intPreferencesKey("threshold")
        val STRONG_WEIGHT = intPreferencesKey("strong_weight")
        val WEAK_WEIGHT = intPreferencesKey("weak_weight")
        val WHATSAPP_ONLY = booleanPreferencesKey("whatsapp_only")
        val THU_FRI_ONLY = booleanPreferencesKey("thu_fri_only")
        val STRONG_KEYWORDS = stringSetPreferencesKey("strong_keywords")
        val WEAK_KEYWORDS = stringSetPreferencesKey("weak_keywords")
        val LAST_SCAN_FINGERPRINT = stringPreferencesKey("last_scan_fingerprint")
        val THEME_DARK = booleanPreferencesKey("theme_dark")
        val GRID_COLUMNS = intPreferencesKey("grid_columns")
    }

    /** Koyu tema mı? Varsayılan: koyu (true). */
    val themeDark: Flow<Boolean> = context.dataStore.data.map { it[Keys.THEME_DARK] ?: true }

    /** Sonuçlar ızgarasında satır başına sütun sayısı; 0 = otomatik (ekrana göre). */
    val gridColumns: Flow<Int> = context.dataStore.data.map { it[Keys.GRID_COLUMNS] ?: 0 }

    suspend fun setThemeDark(v: Boolean) {
        context.dataStore.edit { it[Keys.THEME_DARK] = v }
    }

    suspend fun setGridColumns(v: Int) {
        context.dataStore.edit { it[Keys.GRID_COLUMNS] = v }
    }

    val settings: Flow<DetectionSettings> = context.dataStore.data.map { p ->
        DetectionSettings(
            threshold = p[Keys.THRESHOLD] ?: 60,
            strongWeight = p[Keys.STRONG_WEIGHT] ?: 70,
            weakWeight = p[Keys.WEAK_WEIGHT] ?: 10,
            whatsappOnly = p[Keys.WHATSAPP_ONLY] ?: true,
            thursdayFridayOnly = p[Keys.THU_FRI_ONLY] ?: false,
            strongKeywords = p[Keys.STRONG_KEYWORDS] ?: DEFAULT_STRONG_KEYWORDS,
            weakKeywords = p[Keys.WEAK_KEYWORDS] ?: DEFAULT_WEAK_KEYWORDS,
        )
    }

    /** En son taramanın yapıldığı ayar imzası (yoksa null = hiç tarama yapılmamış). */
    val lastScanFingerprint: Flow<String?> = context.dataStore.data.map { it[Keys.LAST_SCAN_FINGERPRINT] }

    suspend fun setThreshold(v: Int) = setInt(Keys.THRESHOLD, v)
    suspend fun setStrongWeight(v: Int) = setInt(Keys.STRONG_WEIGHT, v)
    suspend fun setWeakWeight(v: Int) = setInt(Keys.WEAK_WEIGHT, v)

    suspend fun setWhatsappOnly(v: Boolean) {
        context.dataStore.edit { it[Keys.WHATSAPP_ONLY] = v }
    }

    suspend fun setThursdayFridayOnly(v: Boolean) {
        context.dataStore.edit { it[Keys.THU_FRI_ONLY] = v }
    }

    suspend fun setLastScanFingerprint(value: String) {
        context.dataStore.edit { it[Keys.LAST_SCAN_FINGERPRINT] = value }
    }

    suspend fun addStrongKeyword(word: String) = editKeywords(Keys.STRONG_KEYWORDS, DEFAULT_STRONG_KEYWORDS) { it + word.trim() }
    suspend fun removeStrongKeyword(word: String) = editKeywords(Keys.STRONG_KEYWORDS, DEFAULT_STRONG_KEYWORDS) { it - word }
    suspend fun addWeakKeyword(word: String) = editKeywords(Keys.WEAK_KEYWORDS, DEFAULT_WEAK_KEYWORDS) { it + word.trim() }
    suspend fun removeWeakKeyword(word: String) = editKeywords(Keys.WEAK_KEYWORDS, DEFAULT_WEAK_KEYWORDS) { it - word }

    /** Yalnızca tespit ayarlarını sıfırlar; tema/sütun/tarama imzası korunur. */
    suspend fun resetToDefaults() {
        context.dataStore.edit { prefs ->
            val fp = prefs[Keys.LAST_SCAN_FINGERPRINT]
            val theme = prefs[Keys.THEME_DARK]
            val grid = prefs[Keys.GRID_COLUMNS]
            prefs.clear()
            if (fp != null) prefs[Keys.LAST_SCAN_FINGERPRINT] = fp
            if (theme != null) prefs[Keys.THEME_DARK] = theme
            if (grid != null) prefs[Keys.GRID_COLUMNS] = grid
        }
    }

    private suspend fun setInt(key: Preferences.Key<Int>, v: Int) {
        context.dataStore.edit { it[key] = v }
    }

    private suspend fun editKeywords(
        key: Preferences.Key<Set<String>>,
        defaults: Set<String>,
        transform: (Set<String>) -> Set<String>,
    ) {
        context.dataStore.edit { p ->
            val current = p[key] ?: defaults
            p[key] = transform(current).filter { it.isNotBlank() }.toSet()
        }
    }
}
