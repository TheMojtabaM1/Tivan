package ir.tivan.controller.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Extra tokens Material3 has no slot for — the translucent "glass" fills and the
 * state accents. Held in a [staticCompositionLocalOf] so reading them never
 * triggers recomposition.
 */
@Immutable
data class TivanColors(
    val dark: Boolean,
    val bg: Color,
    val bg2: Color,
    val glass: Color,
    val glassStrong: Color,
    val stroke: Color,
    val strokeStrong: Color,
    val text: Color,
    val dim: Color,
    val dim2: Color,
    val on: Color,
    val pending: Color,
    val alarm: Color,
    val primary: Color
)

private val DarkTokens = TivanColors(
    dark = true,
    bg = BgDark,
    bg2 = BgDark2,
    glass = Color(0x0EFFFFFF),
    glassStrong = Color(0x17FFFFFF),
    stroke = StrokeDark,
    strokeStrong = Stroke2Dark,
    text = TextDark,
    dim = DimDark,
    dim2 = Dim2Dark,
    on = AccentOn,
    pending = AccentPending,
    alarm = AccentAlarm,
    primary = AccentPrimary
)

private val LightTokens = TivanColors(
    dark = false,
    bg = BgLight,
    bg2 = BgLight2,
    glass = Color(0xB2FFFFFF),
    glassStrong = Color(0xE6FFFFFF),
    stroke = StrokeLight,
    strokeStrong = Stroke2Light,
    text = TextLight,
    dim = DimLight,
    dim2 = Dim2Light,
    on = Color(0xFF00A98A),
    pending = Color(0xFFC98A00),
    alarm = Color(0xFFE03357),
    primary = Color(0xFF5566E8)
)

val LocalTivanColors = staticCompositionLocalOf { DarkTokens }

/** Shorthand: `Tivan.on`, `Tivan.glass`, … inside composables. */
val Tivan: TivanColors
    @Composable @ReadOnlyComposable get() = LocalTivanColors.current

private val DarkScheme = darkColorScheme(
    primary = AccentPrimary,
    secondary = AccentOn,
    tertiary = AccentPending,
    background = BgDark,
    surface = SurfaceDark,
    error = AccentAlarm,
    onPrimary = Color.White,
    onBackground = TextDark,
    onSurface = TextDark,
    onSurfaceVariant = DimDark,
    outline = Stroke2Dark
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF5566E8),
    secondary = Color(0xFF00A98A),
    tertiary = Color(0xFFC98A00),
    background = BgLight,
    surface = SurfaceLight,
    error = Color(0xFFE03357),
    onPrimary = Color.White,
    onBackground = TextLight,
    onSurface = TextLight,
    onSurfaceVariant = DimLight,
    outline = Stroke2Light
)

@Composable
fun TivanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalTivanColors provides if (darkTheme) DarkTokens else LightTokens) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkScheme else LightScheme,
            typography = TivanTypography,
            content = content
        )
    }
}
