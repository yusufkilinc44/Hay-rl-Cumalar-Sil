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
Actions da APK üretir; **GitHub Releases** altında doğrudan indirilebilir `.apk`
olarak yayınlar (zip'e gerek yok), ayrıca artifact olarak da yükler ve
`hayirli-cumalar-sil` ntfy konusuna indirme linkiyle bildirim gönderir.

En güncel APK'yı doğrudan indirmek için deponun **Releases** sayfasına bakın veya
ntfy bildirimindeki linke dokunun.

## Google Drive'a otomatik yükleme kurulumu (tek seferlik)

CI, her build'de APK'yı Drive'daki **Android Apps** klasörüne yükleyebilir. Bunun
için iki secret gerekir:

1. [Google Cloud Console](https://console.cloud.google.com)'da bir proje açın ve
   **Google Drive API**'yi etkinleştirin.
2. **IAM → Service Accounts**'tan bir servis hesabı oluşturun, **JSON anahtar**
   indirin.
3. Drive'da "Android Apps" klasörünü servis hesabının e-posta adresiyle
   (`...@...iam.gserviceaccount.com`) **Düzenleyici** yetkisiyle paylaşın.
4. Klasör ID'sini kopyalayın: `https://drive.google.com/drive/folders/<ID>`
5. GitHub'da depo → **Settings → Secrets and variables → Actions** altına ekleyin:
   - `GDRIVE_SA_KEY`: indirdiğiniz JSON dosyasının tüm içeriği
   - `GDRIVE_FOLDER_ID`: klasör ID'si

Secrets eklenene kadar Drive yüklemesi sessizce atlanır; derleme ve ntfy bildirimi
çalışmaya devam eder.

## Bildirimler (ntfy)

Telefonunuza [ntfy](https://ntfy.sh) uygulamasını kurup `hayirli-cumalar-sil`
konusuna abone olun. Her başarılı build'de sürüm/boyut/Drive linki içeren, hatalı
build'de uyarı içeren bildirim gelir. Not: ntfy.sh konuları herkese açıktır —
konu adını bilen herkes bildirimleri görebilir.

## Gereksinimler

- minSdk 26 (Android 8.0), targetSdk 35
- Android 11+ cihazlarda silme işlemi sistem onay diyaloğu ile yapılır

## Sürüm

Sürüm numarası `app/build.gradle.kts` içindeki `versionName` / `versionCode` alanlarında
tutulur ve uygulamanın **Ayarlar → Hakkında** bölümünde gösterilir.
