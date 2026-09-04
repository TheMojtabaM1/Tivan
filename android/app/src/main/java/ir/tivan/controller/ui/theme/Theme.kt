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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Extra tokens Material3 has no slot for — the translucent "glass" fills, the
 * state accents, and the corner radius that gives each theme its shape
 * identity (near-flat for Obsidian, very round for Linen, crisp for
 * Instrument). Held in a [staticCompositionLocalOf] so reading them never
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
    val primary: Color,
    /** Default corner radius for [ir.tivan.controller.ui.components.GlassCard]. */
    val cardCorner: Dp
)

// ── Obsidian: black + champagne, near-flat, minimal ────────────────────────
private val ObsidianTokens = TivanColors(
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
    primary = Color(0xFFC9A96A),
    cardCorner = 8.dp
)

// ── Linen: warm paper, very round, calm ─────────────────────────────────────
private val LinenTokens = TivanColors(
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
    primary = Color(0xFF12362E),
    cardCorner = 24.dp
)

// ── Instrument: technical dark panel, teal accent, crisp ───────────────────
private val InstrumentTokens = TivanColors(
    dark = true,
    bg = Color(0xFF0B0D10),
    bg2 = Color(0xFF10141A),
    glass = Color(0x0AFFFFFF),
    glassStrong = Color(0x14FFFFFF),
    stroke = Color(0x1EFFFFFF),
    strokeStrong = Color(0x2CFFFFFF),
    text = Color(0xFFDDE3E8),
    dim = Color(0xFF8A94A2),
    dim2 = Color(0xFF5A6472),
    on = Color(0xFF3ED8B4),
    pending = Color(0xFFE8A33D),
    alarm = Color(0xFFF2555A),
    primary = Color(0xFF3ED8B4),
    cardCorner = 4.dp
)

fun tokensFor(theme: AppTheme): TivanColors = when (theme) {
    AppTheme.OBSIDIAN -> ObsidianTokens
    AppTheme.LINEN -> LinenTokens
    AppTheme.INSTRUMENT -> InstrumentTokens
}

val LocalTivanColors = staticCompositionLocalOf { LinenTokens }

/** Which theme is active — screens read this to switch their whole layout shape, not just colors. */
val LocalAppTheme = staticCompositionLocalOf { AppTheme.LINEN }

/** Shorthand: `Tivan.on`, `Tivan.glass`, … inside composables. */
val Tivan: TivanColors
    @Composable @ReadOnlyComposable get() = LocalTivanColors.current

/** Shorthand: `TivanLayout` inside composables — branch on this to pick a screen's layout shape. */
val TivanLayout: AppTheme
    @Composable @ReadOnlyComposable get() = LocalAppTheme.current

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
    appTheme: AppTheme = AppTheme.LINEN,
    content: @Composable () -> Unit
) {
    val tokens = tokensFor(appTheme)
    CompositionLocalProvider(LocalTivanColors provides tokens, LocalAppTheme provides appTheme) {
        MaterialTheme(
            colorScheme = materialScheme(tokens),
            typography = TivanTypography,
            content = content
        )
    }
}
