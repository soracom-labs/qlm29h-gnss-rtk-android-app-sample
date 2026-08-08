package jp.co.soracom.qlm29hrtk.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF34CDD7),
    onPrimary = Color(0xFF1E1D21),
    primaryContainer = Color(0xFF005F65),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF464055),
    onSecondary = Color.White,
    tertiary = Color(0xFF096CFF),
    background = Color.White,
    onBackground = Color(0xFF1E1D21),
    surface = Color.White,
    onSurface = Color(0xFF1E1D21),
    errorContainer = Color(0xFFFFB2A6),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF34CDD7),
    onPrimary = Color(0xFF1E1D21),
    primaryContainer = Color(0xFF005F65),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFFBEB6D0),
    tertiary = Color(0xFF8DB7FF),
    background = Color(0xFF1E1D21),
    onBackground = Color.White,
    surface = Color(0xFF29272E),
    onSurface = Color.White,
    errorContainer = Color(0xFF7D2E25),
)

@Composable
fun Qlm29hTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, content = content)
}
