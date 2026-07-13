# Hayırlı Cumalar Sil 🕌🧹

WhatsApp üzerinden her hafta gelen "Hayırlı Cumalar / Mübarek Cumalar" temalı kutlama
görsellerini otomatik tespit eden ve **sizin onayınızla** silen, tamamen cihaz üzerinde
çalışan native Android uygulaması.

## Nasıl çalışır?

1. **Tara** butonuna bastığınızda uygulama WhatsApp Images klasöründeki (isteğe bağlı tüm
   galerideki) görselleri listeler.
2. Her görseldeki metin, cihaz üzerinde çalışan **ML Kit OCR** ile okunur — hiçbir görsel
   telefondan dışarı çıkmaz.
3. Okunan metin, dosya tarihi (perşembe/cuma) ve WhatsApp dosya adı deseni birleştirilerek
   0–100 arası bir **Cuma Skoru** hesaplanır.
4. Eşiği geçen görseller ızgara halinde gösterilir; istediklerinizi seçersiniz.
5. **Sil** dediğinizde Android'in sistem onay penceresi açılır — son karar her zaman sizde.
6. Silinen adet ve kazanılan alan istatistiklere işlenir.

## Özellikler

- 🎨 Jetpack Compose + Material 3, canlı renkler, animasyonlar, koyu/açık tema
- 🔍 Ayarlanabilir tespit algoritması: eşik, kelime puanları, gün bonusu, dosya adı bonusu
- 📝 Güçlü/zayıf anahtar kelime listeleri (ekleyip çıkarabilirsiniz)
- 📊 İstatistikler: toplam/haftalık/aylık silinen, kazanılan alan, en büyük dosya,
  haftalık grafik
- 🔒 Tam gizlilik: internet izni yok, tüm analiz cihazda

## Derleme

```bash
./gradlew assembleDebug
```

APK `app/build/outputs/apk/debug/app-debug.apk` yoluna üretilir. Her push'ta GitHub
Actions da APK üretip artifact olarak yükler.

## Gereksinimler

- minSdk 26 (Android 8.0), targetSdk 35
- Android 11+ cihazlarda silme işlemi sistem onay diyaloğu ile yapılır

## Sürüm

Sürüm numarası `app/build.gradle.kts` içindeki `versionName` / `versionCode` alanlarında
tutulur ve uygulamanın **Ayarlar → Hakkında** bölümünde gösterilir.
