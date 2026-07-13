package com.hayirlicumalarsil.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val VividPurple = Color(0xFF6C3DF4)
val VividPink = Color(0xFFE91E63)
val VividOrange = Color(0xFFFF6D00)
val SunnyYellow = Color(0xFFFFC107)

/** Ana sayfa ve kutlama kartlarında kullanılan neşeli gradyan. */
val HeroGradient = listOf(VividPurple, VividPink, VividOrange)

private val LightColors = lightColorScheme(
    primary = VividPurple,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE9DDFF),
    onPrimaryContainer = Color(0xFF22005D),
    secondary = Color(0xFFE65100),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE0B2),
    onSecondaryContainer = Color(0xFF4A2800),
    tertiary = Color(0xFFC2185B),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFD9E2),
    onTertiaryContainer = Color(0xFF3E001D),
    background = Color(0xFFFDF7FF),
    onBackground = Color(0xFF1D1B20),
    surface = Color(0xFFFDF7FF),
    onSurface = Color(0xFF1D1B20),
    surfaceVariant = Color(0xFFE9E0F0),
    onSurfaceVariant = Color(0xFF49454F),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFCFBCFF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFE9DDFF),
    secondary = Color(0xFFFFB77C),
    onSecondary = Color(0xFF4A2800),
    secondaryContainer = Color(0xFF6B3B00),
    onSecondaryContainer = Color(0xFFFFE0B2),
    tertiary = Color(0xFFFFB1C8),
    onTertiary = Color(0xFF5E1133),
    tertiaryContainer = Color(0xFF7B2949),
    onTertiaryContainer = Color(0xFFFFD9E2),
    background = Color(0xFF141218),
    onBackground = Color(0xFFE6E0E9),
    surface = Color(0xFF141218),
    onSurface = Color(0xFFE6E0E9),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

@Composable
fun HcsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
