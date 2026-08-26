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

val TivanTypography = Typography(
    displaySmall = style(30, FontWeight.Black, 40),
    headlineMedium = style(22, FontWeight.Black, 32),
    headlineSmall = style(19, FontWeight.Black, 28),
    titleLarge = style(17, FontWeight.Black, 26),
    titleMedium = style(15, FontWeight.Black, 24),
    titleSmall = style(14, FontWeight.Black, 22),
    bodyLarge = style(15, FontWeight.Normal, 26),
    bodyMedium = style(13, FontWeight.Normal, 22),
    bodySmall = style(12, FontWeight.Normal, 20),
    labelLarge = style(13, FontWeight.Medium, 20),
    labelMedium = style(12, FontWeight.Medium, 18),
    labelSmall = style(11, FontWeight.Medium, 16)
)
