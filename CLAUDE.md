# Hayırlı Cumalar Sil — Proje Notları

## Kullanıcı tercihleri (kalıcı)

- **Kod veya içerik üretirken onay isteme.** Kullanıcı her zaman önceden onay
  vermiş kabul edilir; doğrudan yaz ve uygula. Yalnızca geri döndürülmesi zor
  veya dışa dönük işlemler (kalıcı silme, dışarıya yayınlama vb.) için kısa teyit
  alınır.
- Arayüz ve tüm kullanıcıya dönük metinler **Türkçe**.
- Her yeni düzeltme/build'de `app/build.gradle.kts` içindeki `versionCode` ve
  `versionName` artırılır.

## Mimari özet

- Kotlin + Jetpack Compose (Material 3), min SDK 26, target SDK 35.
- Tespit: ML Kit Text Recognition (cihaz üzerinde OCR) + `FridayScorer` puanlama.
- Tarama, foreground `ScanService` içinde `ScanEngine` (singleton) tarafından
  yürütülür; sonuçlar Room'a (`scanned_image` tablosu, ham OCR metni) yazılır ve
  tek doğruluk kaynağıdır. Böylece tarama yarıda kesilse/uygulama öldürülse bile
  bulunanlar kaybolmaz ve daha önce taranan görsel yeniden OCR'lanmaz.
- Adaylar Room'dan türetilir (`ScanViewModel.candidates`), eşik/kelime ayarı
  değişince yeniden tarama olmadan anında yeniden skorlanır.
- CI (GitHub Actions) her push'ta test + APK üretir, `hayirli-cumalar-sil` ntfy
  konusuna bildirir; secrets tanımlıysa APK'yı Google Drive'a yükler.

## Derleme doğrulaması

Ortamda Android SDK yok; derleme GitHub Actions üzerinde `testDebugUnitTest` +
`assembleDebug` ile doğrulanır.
