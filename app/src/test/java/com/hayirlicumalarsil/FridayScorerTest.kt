package com.hayirlicumalarsil

import com.hayirlicumalarsil.data.DetectionSettings
import com.hayirlicumalarsil.detection.FridayScorer
import com.hayirlicumalarsil.detection.TextNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FridayScorerTest {

    private val settings = DetectionSettings()

    @Test
    fun `tipik cuma mesaji yuksek puan alir`() {
        val result = FridayScorer.score(
            rawText = "HAYIRLI CUMALAR! Dualarınız kabul olsun. Amin.",
            settings = settings,
        )
        // güçlü(70) + zayıf amin(10) = 80
        assertTrue("beklenen >= eşik, gelen: ${result.score}", result.score >= settings.threshold)
        assertTrue(result.matchedKeywords.contains("hayırlı cumalar"))
    }

    @Test
    fun `turkce karakter farklari eslesmeyi bozmaz`() {
        val result = FridayScorer.score(rawText = "hayirli cumalar", settings = settings)
        assertEquals(settings.strongWeight, result.score)
    }

    @Test
    fun `metinsiz gorsel elenir`() {
        val result = FridayScorer.score(rawText = "", settings = settings)
        assertEquals(0, result.score)
    }

    @Test
    fun `karikatur sadece zayif kelimeyle elenir`() {
        // Banka karikatürü: sadece "allah" geçiyor, güçlü ifade yok → aday değil.
        val result = FridayScorer.score(
            rawText = "namaz kiliyor musunuz allah'a borcunu odemeyen bize hic odemez",
            settings = settings,
        )
        assertEquals(0, result.score)
    }

    @Test
    fun `guclu ifade yoksa zayif kelimeler tek basina yetmez`() {
        val result = FridayScorer.score(
            rawText = "dua amin bismillah ayet hadis",
            settings = settings,
        )
        assertEquals(0, result.score)
    }

    @Test
    fun `cumartesi kelimesi cuma olarak sayilmaz`() {
        // Güçlü ifade var; zayıf "cuma" yalnız tam kelime eşleşir, "cumartesi" saymaz.
        val result = FridayScorer.score(
            rawText = "hayırlı cumalar cumartesi",
            settings = settings,
        )
        assertEquals(settings.strongWeight, result.score) // ek zayıf yok
        assertFalse(result.matchedKeywords.contains("cuma"))
    }

    @Test
    fun `birden fazla guclu eslesme ek puan getirir`() {
        val result = FridayScorer.score(
            rawText = "hayırlı cumalar mübarek cumalar",
            settings = settings,
        )
        // iki güçlü eşleşme: 70 + 10 = 80
        assertEquals(80, result.score)
    }

    @Test
    fun `puan 100 ile sinirlidir`() {
        val result = FridayScorer.score(
            rawText = "hayırlı cumalar cumanız mübarek olsun mübarek cumalar dua amin",
            settings = settings,
        )
        assertEquals(100, result.score)
    }

    @Test
    fun `normalizasyon turkce karakterleri sadelestirir`() {
        assertEquals(
            "hayirli cumalar mubarek olsun",
            TextNormalizer.normalize("  HAYIRLI   Cumalar,, MÜBAREK -- olsun!  "),
        )
    }

    @Test
    fun `esik ayari degisince tespit degisir`() {
        val strict = settings.copy(threshold = 90)
        val result = FridayScorer.score(rawText = "hayırlı cumalar", settings = strict)
        assertTrue(result.score < strict.threshold)
    }
}
