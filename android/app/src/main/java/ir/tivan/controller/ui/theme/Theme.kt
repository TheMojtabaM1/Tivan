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
    val primary: Color,
    /** Fill of a tile whose output is on — the Kashi signature yellow. */
    val tileOn: Color,
    /** Text/icons on [tileOn]. */
    val tileOnInk: Color,
    /** Fill of a tile whose output is off. */
    val tileOff: Color,
    /** Fill of the high-contrast "ink" tile (security, primary actions). */
    val ink: Color,
    /** Text on [ink]. */
    val onInk: Color
)

// ── Kashi: bright ground, yellow = on, grey = off ──────────────────────────
private val KashiTokens = TivanColors(
    dark = false,
    bg = Color(0xFFF6F7F2),
    bg2 = Color(0xFFECEEE5),
    glass = Color(0xFFFFFFFF),
    glassStrong = Color(0xFFFFFFFF),
    stroke = Color(0x1A1B1F16),
    strokeStrong = Color(0x331B1F16),
    text = Color(0xFF1B1F16),
    dim = Color(0xFF5E6354),
    dim2 = Color(0xFF878C7B),
    on = Color(0xFF2E8B57),
    pending = Color(0xFFD9822B),
    alarm = Color(0xFFE5483A),
    primary = Color(0xFF1B1F16),
    tileOn = Color(0xFFFFD23F),
    tileOnInk = Color(0xFF1B1F16),
    tileOff = Color(0xFFE4E6DC),
    ink = Color(0xFF1B1F16),
    onInk = Color(0xFFF6F7F2)
)

// ── Kashi Night: same tiles on a dark ground ────────────────────────────────
private val KashiNightTokens = TivanColors(
    dark = true,
    bg = Color(0xFF15171A),
    bg2 = Color(0xFF1C1F23),
    glass = Color(0xFF22262B),
    glassStrong = Color(0xFF2A2F35),
    stroke = Color(0x1FFFFFFF),
    strokeStrong = Color(0x33FFFFFF),
    text = Color(0xFFEEF0EA),
    dim = Color(0xFFA7AC9E),
    dim2 = Color(0xFF7E8377),
    on = Color(0xFF5BC98A),
    pending = Color(0xFFF0A04B),
    alarm = Color(0xFFFF6B5C),
    primary = Color(0xFFFFD23F),
    tileOn = Color(0xFFFFD23F),
    tileOnInk = Color(0xFF1B1F16),
    tileOff = Color(0xFF2C3036),
    ink = Color(0xFFEEF0EA),
    onInk = Color(0xFF15171A)
)

fun tokensFor(palette: TivanPalette): TivanColors = when (palette) {
    TivanPalette.KASHI -> KashiTokens
    TivanPalette.KASHI_NIGHT -> KashiNightTokens
}

val LocalTivanColors = staticCompositionLocalOf { KashiTokens }

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
    palette: TivanPalette = TivanPalette.KASHI,
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
