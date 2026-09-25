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
        "کاشی‌های بزرگ و گرد",
        22.dp
    )
}

/**
 * Color identity. The app now has one visual language — "کاشی" (tiles):
 * big full-width blocks whose fill color *is* the state. Two variants of it:
 * a bright daytime one and a dark one for night use.
 */
enum class TivanPalette(val label: String, val description: String) {
    KASHI(
        "کاشی روشن",
        "زمینه‌ی روشن، کاشی زرد برای روشن و خاکستری برای خاموش"
    ),
    KASHI_NIGHT(
        "کاشی شب",
        "همان کاشی‌ها روی زمینه‌ی تیره — برای شب و چشم خسته"
    )
}
