package com.hayirlicumalarsil.detection

import java.util.Locale

/**
 * OCR çıktısını ve anahtar kelimeleri karşılaştırılabilir hale getirir:
 * Türkçe karakterler sadeleştirilir, noktalama atılır, boşluklar teklenir.
 * Böylece OCR'ın "Hayirli" ya da "HAYIRLI" okuması fark yaratmaz.
 */
object TextNormalizer {

    private val turkishLocale = Locale("tr", "TR")

    fun normalize(input: String): String = input
        .lowercase(turkishLocale)
        .replace('ı', 'i')
        .replace('ğ', 'g')
        .replace('ş', 's')
        .replace('ç', 'c')
        .replace('ö', 'o')
        .replace('ü', 'u')
        .replace('â', 'a')
        .replace('î', 'i')
        .replace('û', 'u')
        .replace(Regex("[^a-z0-9 ]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}
