package ir.tivan.controller.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import ir.tivan.controller.R

/** Arad — the app-wide Persian typeface. Medium for body, Black for titles. */
val Arad = FontFamily(
    Font(R.font.arad_medium, FontWeight.Normal),
    Font(R.font.arad_medium, FontWeight.Medium),
    Font(R.font.arad_black, FontWeight.Bold),
    Font(R.font.arad_black, FontWeight.Black)
)

private fun style(size: Int, weight: FontWeight, lineHeight: Int, spacing: Double = 0.0) =
    TextStyle(
        fontFamily = Arad,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = lineHeight.sp,
        letterSpacing = spacing.sp
    )

// Sized up from the original scale — most users are older adults with
// weaker eyesight, so legibility beats density everywhere in this app.
val TivanTypography = Typography(
    displaySmall = style(34, FontWeight.Black, 44),
    headlineMedium = style(25, FontWeight.Black, 35),
    headlineSmall = style(22, FontWeight.Black, 31),
    titleLarge = style(20, FontWeight.Black, 29),
    titleMedium = style(18, FontWeight.Black, 27),
    titleSmall = style(16, FontWeight.Black, 24),
    bodyLarge = style(18, FontWeight.Normal, 29),
    bodyMedium = style(16, FontWeight.Normal, 26),
    bodySmall = style(14, FontWeight.Normal, 22),
    labelLarge = style(16, FontWeight.Medium, 23),
    labelMedium = style(15, FontWeight.Medium, 21),
    labelSmall = style(13, FontWeight.Medium, 19)
)
