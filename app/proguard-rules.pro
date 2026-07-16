# ML Kit
-keep class com.google.mlkit.** { *; }

# Room (KSP ile üretilen kod, güvenlik için genişletilmiş kural)
-keep class androidx.room.** { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class *

# Kotlin coroutines dahili sınıfları için R8 uyarılarını bastır
-dontwarn kotlinx.coroutines.**
