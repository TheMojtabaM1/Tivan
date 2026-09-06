package ir.tivan.controller.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Structural shape of the UI — cards vs. flat hairline-divided blocks. This is
 * one of the two independent axes the user picks in Settings; the other one
 * ([TivanPalette]) owns color only. Kept to exactly two values on purpose:
 * every screen's `when (CurrentLayout) { ... }` branch is meant to stay a
 * simple binary choice, not grow a third structural shape over time.
 */
enum class TivanLayout(val label: String, val description: String, val cardCorner: Dp) {
    FLAT(
        "تخت",
        "بدون کارت، خطوط مویی — مینیمال و فشرده",
        8.dp
    ),
    CARD(
        "کارتی",
        "کارت‌های نرم و گرد — آرام و شفاف",
        24.dp
    )
}

/**
 * Color identity — completely independent of [TivanLayout]. Any palette can be
 * paired with either layout.
 */
enum class TivanPalette(val label: String, val description: String) {
    CREAM(
        "کرمی روشن",
        "کاغذ گرم و روشن — آرام و دوستانه"
    ),
    LIGHT(
        "روشن",
        "خاکستری و سفید خنثی — ساده و تمیز"
    ),
    DARK(
        "دارک",
        "مشکی و شامپاینی — مینیمال و شیک"
    ),
    LIQUID_GLASS(
        "لیکویید گلس",
        "شیشه‌ای مات و روشن با لهجه‌ی آبی — مدرن و شفاف"
    )
}
