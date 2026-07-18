package com.hayirlicumalarsil

import com.hayirlicumalarsil.data.DetectionSettings
import com.hayirlicumalarsil.detection.FridayScorer
import com.hayirlicumalarsil.detection.TextNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class FridayScorerTest {

    private val settings = DetectionSettings()

    // 2026-07-10 bir cuma günü
    private val fridayMillis: Long = ZonedDateTime
        .of(2026, 7, 10, 12, 0, 0, 0, ZoneId.systemDefault())
        .toInstant().toEpochMilli()

    // 2026-07-13 bir pazartesi günü
    private val mondayMillis: Long = ZonedDateTime
        .of(2026, 7, 13, 12, 0, 0, 0, ZoneId.systemDefault())
        .toInstant().toEpochMilli()

    @Test
    fun `tipik cuma mesaji gorseli yuksek puan alir`() {
        val result = FridayScorer.score(
            rawText = "HAYIRLI CUMALAR! Dualarınız kabul olsun. Amin.",
            fileName = "IMG-20260710-WA0012.jpg",
            dateMillis = fridayMillis,
            settings = settings,
        )
        // güçlü(70) + zayıf amin(10) + gün(15); ad bonusu whatsappOnly açıkken uygulanmaz
        assertTrue("beklenen >= eşik, gelen: ${result.score}", result.score >= settings.threshold)
        assertTrue(result.matchedKeywords.contains("hayırlı cumalar"))
    }

    @Test
    fun `turkce karakter farklari eslesmeyi bozmaz`() {
        val result = FridayScorer.score(
            rawText = "hayirli cumalar",
            fileName = "foto.jpg",
            dateMillis = mondayMillis,
            settings = settings,
        )
        assertEquals(settings.strongWeight, result.score)
    }

    @Test
    fun `metinsiz gorsel guclu kelime sarti ile elenir`() {
        // Beach selfie senaryosu: metin yok, cuma günü + WA adı. Güçlü kelime
        // şartı açıkken (varsayılan) skor 0 olmalı — aday olmamalı.
        val result = FridayScorer.score(
            rawText = "",
            fileName = "IMG-20260710-WA0001.jpg",
            dateMillis = fridayMillis,
            settings = settings,
        )
        assertEquals(0, result.score)
    }

    @Test
    fun `karikatur sadece zayif kelimeyle elenir`() {
        // Banka karikatürü senaryosu: sadece "allah" geçiyor, güçlü ifade yok.
        val result = FridayScorer.score(
            rawText = "namaz kiliyor musunuz allah'a borcunu odemeyen bize hic odemez",
            fileName = "IMG-20200817-WA0034.jpg",
            dateMillis = mondayMillis,
            settings = settings,
        )
        assertEquals(0, result.score)
    }

    @Test
    fun `cumartesi kelimesi cuma olarak sayilmaz`() {
        val result = FridayScorer.score(
            rawText = "cumartesi kahvaltısı",
            fileName = "foto.jpg",
            dateMillis = mondayMillis,
            settings = settings,
        )
        assertEquals(0, result.score)
        assertFalse(result.matchedKeywords.contains("cuma"))
    }

    @Test
    fun `guclu kelime sarti kapaliyken zayif kelimeler puanlanir`() {
        // Güçlü kelime şartı kapatılırsa eski davranış: en çok 3 zayıf kelime sayılır.
        val result = FridayScorer.score(
            rawText = "dua amin bismillah ayet hadis",
            fileName = "random.png",
            dateMillis = mondayMillis,
            settings = settings.copy(requireStrongKeyword = false),
        )
        assertEquals(3 * settings.weakWeight, result.score)
    }

    @Test
    fun `whatsapp filtresi acikken dosya adi bonusu uygulanmaz`() {
        val withName = FridayScorer.score(
            rawText = "hayırlı cumalar",
            fileName = "IMG-20260710-WA0012.jpg",
            dateMillis = mondayMillis,
            settings = settings.copy(whatsappOnly = true),
        )
        // whatsappOnly açık → yalnız güçlü kelime puanı (70), ad bonusu yok
        assertEquals(settings.strongWeight, withName.score)

        val withoutFilter = FridayScorer.score(
            rawText = "hayırlı cumalar",
            fileName = "IMG-20260710-WA0012.jpg",
            dateMillis = mondayMillis,
            settings = settings.copy(whatsappOnly = false),
        )
        // whatsappOnly kapalı → ad bonusu eklenir
        assertEquals(settings.strongWeight + settings.nameBonus, withoutFilter.score)
    }

    @Test
    fun `sadece persembe cuma filtresi acikken gun bonusu uygulanmaz`() {
        val result = FridayScorer.score(
            rawText = "hayırlı cumalar",
            fileName = "foto.jpg",
            dateMillis = fridayMillis,
            settings = settings.copy(thursdayFridayOnly = true),
        )
        // filtre açık → gün bonusu yok, yalnız güçlü kelime (70)
        assertEquals(settings.strongWeight, result.score)
    }

    @Test
    fun `puan 100 ile sinirlidir`() {
        val result = FridayScorer.score(
            rawText = "hayırlı cumalar cumanız mübarek olsun mübarek cumalar dua amin",
            fileName = "IMG-20260710-WA0044.jpg",
            dateMillis = fridayMillis,
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
        val result = FridayScorer.score(
            rawText = "hayırlı cumalar",
            fileName = "foto.jpg",
            dateMillis = mondayMillis,
            settings = strict,
        )
        assertTrue(result.score < strict.threshold)
    }
}
