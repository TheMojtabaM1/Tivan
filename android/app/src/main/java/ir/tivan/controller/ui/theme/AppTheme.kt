package ir.tivan.controller.ui.theme

/**
 * The three selectable visual identities. Each one fixes its own light/dark
 * character — Obsidian and Instrument are always dark, Linen is always
 * light — rather than layering a separate system dark-mode toggle on top,
 * since that combination was never part of what was designed or tested.
 */
enum class AppTheme(val label: String, val description: String) {
    OBSIDIAN(
        "اُبسیدین",
        "مشکی و شامپاینی، خطوط مویی، بدون کارت — مینیمال و شیک"
    ),
    LINEN(
        "لینن",
        "کاغذ گرم و روشن، کارت‌های نرم گرد — آرام و دوستانه"
    ),
    INSTRUMENT(
        "اینسترومنت",
        "پنل فنی تیره با لهجه‌ی فیروزه‌ای — دقیق و تکنیکال"
    )
}
