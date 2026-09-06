package ir.tivan.controller.ui.theme

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
 * Extra tokens Material3 has no slot for — the translucent "glass" fills and
 * the state accents. Shape (corner radius) lives on [TivanLayout] instead,
 * since color and shape are independent axes now. Held in a
 * [staticCompositionLocalOf] so reading them never triggers recomposition.
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

// ── Cream: warm paper, calm ──────────────────────────────────────────────────
private val CreamTokens = TivanColors(
    dark = false,
    bg = Color(0xFFF4F1EC),
    bg2 = Color(0xFFE8E3D9),
    glass = Color(0xB2FFFFFF),
    glassStrong = Color(0xE6FFFFFF),
    stroke = Color(0x14181713),
    strokeStrong = Color(0x22181713),
    text = Color(0xFF181713),
    dim = Color(0xFF8A857A),
    dim2 = Color(0xFFA9A296),
    on = Color(0xFF00A98A),
    pending = Color(0xFFC98A00),
    alarm = Color(0xFFE03357),
    primary = Color(0xFF12362E)
)

// ── Light: plain neutral, clean ─────────────────────────────────────────────
private val LightTokens = TivanColors(
    dark = false,
    bg = Color(0xFFFAFAFA),
    bg2 = Color(0xFFF0F0F0),
    glass = Color(0xB2FFFFFF),
    glassStrong = Color(0xE6FFFFFF),
    stroke = Color(0x14000000),
    strokeStrong = Color(0x22000000),
    text = Color(0xFF1A1A1A),
    dim = Color(0xFF6B6B6B),
    dim2 = Color(0xFF9A9A9A),
    on = Color(0xFF0E8A6E),
    pending = Color(0xFFC98A00),
    alarm = Color(0xFFD93655),
    primary = Color(0xFF1F6FEB)
)

// ── Dark: near-black + champagne, minimal ───────────────────────────────────
private val DarkTokens = TivanColors(
    dark = true,
    bg = Color(0xFF08080A),
    bg2 = Color(0xFF0C0F1A),
    glass = Color(0x0EFFFFFF),
    glassStrong = Color(0x17FFFFFF),
    stroke = Color(0x1FFFFFFF),
    strokeStrong = Color(0x2EFFFFFF),
    text = Color(0xFFEDEDF0),
    dim = Color(0xFF9AA3BD),
    dim2 = Color(0xFF6F7893),
    on = Color(0xFF4ECBA5),
    pending = Color(0xFFE8A33D),
    alarm = Color(0xFFFF5470),
    primary = Color(0xFFC9A96A)
)

// ── Liquid Glass: frosted, translucent, iOS-26-style ────────────────────────
private val LiquidGlassTokens = TivanColors(
    dark = false,
    bg = Color(0xFFE7EEF5),
    bg2 = Color(0xFFD8E3EE),
    glass = Color(0x59FFFFFF),
    glassStrong = Color(0x99FFFFFF),
    stroke = Color(0x80FFFFFF),
    strokeStrong = Color(0xB3FFFFFF),
    text = Color(0xFF14202B),
    dim = Color(0xFF51606E),
    dim2 = Color(0xFF7C8A97),
    on = Color(0xFF12B886),
    pending = Color(0xFFFF9F1C),
    alarm = Color(0xFFFF375F),
    primary = Color(0xFF0A84FF)
)

fun tokensFor(palette: TivanPalette): TivanColors = when (palette) {
    TivanPalette.CREAM -> CreamTokens
    TivanPalette.LIGHT -> LightTokens
    TivanPalette.DARK -> DarkTokens
    TivanPalette.LIQUID_GLASS -> LiquidGlassTokens
}

val LocalTivanColors = staticCompositionLocalOf { CreamTokens }

/** Which layout is active — screens read this to switch their whole structural shape. */
val LocalTivanLayout = staticCompositionLocalOf { TivanLayout.CARD }

/** Shorthand: `Tivan.on`, `Tivan.glass`, … inside composables. */
val Tivan: TivanColors
    @Composable @ReadOnlyComposable get() = LocalTivanColors.current

/** Shorthand: `CurrentLayout` inside composables — branch on this to pick a screen's structural shape. */
val CurrentLayout: TivanLayout
    @Composable @ReadOnlyComposable get() = LocalTivanLayout.current

private fun materialScheme(c: TivanColors) = if (c.dark) {
    darkColorScheme(
        primary = c.primary,
        secondary = c.on,
        tertiary = c.pending,
        background = c.bg,
        surface = c.bg2,
        error = c.alarm,
        onPrimary = Color.White,
        onBackground = c.text,
        onSurface = c.text,
        onSurfaceVariant = c.dim,
        outline = c.strokeStrong
    )
} else {
    lightColorScheme(
        primary = c.primary,
        secondary = c.on,
        tertiary = c.pending,
        background = c.bg,
        surface = Color.White,
        error = c.alarm,
        onPrimary = Color.White,
        onBackground = c.text,
        onSurface = c.text,
        onSurfaceVariant = c.dim,
        outline = c.strokeStrong
    )
}

@Composable
fun TivanTheme(
    layout: TivanLayout = TivanLayout.CARD,
    palette: TivanPalette = TivanPalette.CREAM,
    content: @Composable () -> Unit
) {
    val tokens = tokensFor(palette)
    CompositionLocalProvider(LocalTivanColors provides tokens, LocalTivanLayout provides layout) {
        MaterialTheme(
            colorScheme = materialScheme(tokens),
            typography = TivanTypography,
            content = content
        )
    }
}
