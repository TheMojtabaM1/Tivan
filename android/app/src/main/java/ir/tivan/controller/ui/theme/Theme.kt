package ir.tivan.controller.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Accent,
    secondary = AccentGreen,
    tertiary = Warn,
    background = BgDark,
    surface = Surface,
    surfaceVariant = Surface2,
    error = Danger,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    outline = Border
)

private val LightColors = lightColorScheme(
    primary = Accent,
    secondary = AccentGreen,
    tertiary = Warn,
    background = Color(0xFFF5F6F8),
    surface = Color.White,
    error = Danger
)

@Composable
fun TivanTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = MaterialTheme.typography,
        content = content
    )
}
