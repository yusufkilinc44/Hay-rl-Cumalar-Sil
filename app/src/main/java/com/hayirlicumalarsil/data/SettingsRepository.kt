package com.hayirlicumalarsil.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
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

/** Tespit algoritmasının ayarlanabilir tüm parametreleri. */
data class DetectionSettings(
    val threshold: Int = 60,
    val strongWeight: Int = 70,
    val weakWeight: Int = 10,
    val dayBonus: Int = 15,
    val nameBonus: Int = 10,
    val whatsappOnly: Boolean = true,
    val thursdayFridayOnly: Boolean = false,
    val minSizeKb: Int = 0,
    val strongKeywords: Set<String> = DEFAULT_STRONG_KEYWORDS,
    val weakKeywords: Set<String> = DEFAULT_WEAK_KEYWORDS,
)

private val Context.dataStore by preferencesDataStore(name = "hcs_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val THRESHOLD = intPreferencesKey("threshold")
        val STRONG_WEIGHT = intPreferencesKey("strong_weight")
        val WEAK_WEIGHT = intPreferencesKey("weak_weight")
        val DAY_BONUS = intPreferencesKey("day_bonus")
        val NAME_BONUS = intPreferencesKey("name_bonus")
        val WHATSAPP_ONLY = booleanPreferencesKey("whatsapp_only")
        val THU_FRI_ONLY = booleanPreferencesKey("thu_fri_only")
        val MIN_SIZE_KB = intPreferencesKey("min_size_kb")
        val STRONG_KEYWORDS = stringSetPreferencesKey("strong_keywords")
        val WEAK_KEYWORDS = stringSetPreferencesKey("weak_keywords")
    }

    val settings: Flow<DetectionSettings> = context.dataStore.data.map { p ->
        DetectionSettings(
            threshold = p[Keys.THRESHOLD] ?: 60,
            strongWeight = p[Keys.STRONG_WEIGHT] ?: 70,
            weakWeight = p[Keys.WEAK_WEIGHT] ?: 10,
            dayBonus = p[Keys.DAY_BONUS] ?: 15,
            nameBonus = p[Keys.NAME_BONUS] ?: 10,
            whatsappOnly = p[Keys.WHATSAPP_ONLY] ?: true,
            thursdayFridayOnly = p[Keys.THU_FRI_ONLY] ?: false,
            minSizeKb = p[Keys.MIN_SIZE_KB] ?: 0,
            strongKeywords = p[Keys.STRONG_KEYWORDS] ?: DEFAULT_STRONG_KEYWORDS,
            weakKeywords = p[Keys.WEAK_KEYWORDS] ?: DEFAULT_WEAK_KEYWORDS,
        )
    }

    suspend fun setThreshold(v: Int) = setInt(Keys.THRESHOLD, v)
    suspend fun setStrongWeight(v: Int) = setInt(Keys.STRONG_WEIGHT, v)
    suspend fun setWeakWeight(v: Int) = setInt(Keys.WEAK_WEIGHT, v)
    suspend fun setDayBonus(v: Int) = setInt(Keys.DAY_BONUS, v)
    suspend fun setNameBonus(v: Int) = setInt(Keys.NAME_BONUS, v)
    suspend fun setMinSizeKb(v: Int) = setInt(Keys.MIN_SIZE_KB, v)

    suspend fun setWhatsappOnly(v: Boolean) {
        context.dataStore.edit { it[Keys.WHATSAPP_ONLY] = v }
    }

    suspend fun setThursdayFridayOnly(v: Boolean) {
        context.dataStore.edit { it[Keys.THU_FRI_ONLY] = v }
    }

    suspend fun addStrongKeyword(word: String) = editKeywords(Keys.STRONG_KEYWORDS, DEFAULT_STRONG_KEYWORDS) { it + word.trim() }
    suspend fun removeStrongKeyword(word: String) = editKeywords(Keys.STRONG_KEYWORDS, DEFAULT_STRONG_KEYWORDS) { it - word }
    suspend fun addWeakKeyword(word: String) = editKeywords(Keys.WEAK_KEYWORDS, DEFAULT_WEAK_KEYWORDS) { it + word.trim() }
    suspend fun removeWeakKeyword(word: String) = editKeywords(Keys.WEAK_KEYWORDS, DEFAULT_WEAK_KEYWORDS) { it - word }

    suspend fun resetToDefaults() {
        context.dataStore.edit { it.clear() }
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
