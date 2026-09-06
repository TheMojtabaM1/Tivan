package ir.tivan.controller.util

import android.content.Context
import androidx.core.content.edit
import ir.tivan.controller.ui.theme.TivanLayout
import ir.tivan.controller.ui.theme.TivanPalette
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** How much of the app's configuration surface is shown at once. */
enum class UiMode {
    /** Outputs + security only; everything else lives under Settings. */
    SIMPLE,

    /** All five tabs — outputs, inputs, security, status, settings. */
    ADVANCED
}

/**
 * Small app-wide settings: which layout shape and color palette are active
 * (two independent axes — see [TivanLayout] / [TivanPalette]), and whether the
 * UI shows the simple or advanced tab set.
 *
 * Backed by [android.content.SharedPreferences] rather than DataStore — there
 * are exactly a handful of values, all read once at process start and written
 * rarely from the main thread by a settings toggle, so the extra dependency
 * and async API surface of DataStore would buy nothing here.
 *
 * [uiMode] is `null` until the user answers the first-launch prompt; that is
 * what [ir.tivan.controller.MainActivity] uses to decide whether to show
 * onboarding before the rest of the app.
 */
class AppPreferences(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    private val _uiMode = MutableStateFlow(readUiMode())
    val uiMode: StateFlow<UiMode?> = _uiMode.asStateFlow()

    private val _layout = MutableStateFlow(readLayout())
    val layout: StateFlow<TivanLayout> = _layout.asStateFlow()

    private val _palette = MutableStateFlow(readPalette())
    val palette: StateFlow<TivanPalette> = _palette.asStateFlow()

    private val _voiceEnabled = MutableStateFlow(prefs.getBoolean(KEY_VOICE, true))
    val voiceEnabled: StateFlow<Boolean> = _voiceEnabled.asStateFlow()

    fun setVoiceEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_VOICE, enabled) }
        _voiceEnabled.value = enabled
    }

    fun setUiMode(mode: UiMode) {
        prefs.edit { putString(KEY_MODE, mode.name) }
        _uiMode.value = mode
    }

    fun setLayout(layout: TivanLayout) {
        prefs.edit { putString(KEY_LAYOUT, layout.name) }
        _layout.value = layout
    }

    fun setPalette(palette: TivanPalette) {
        prefs.edit { putString(KEY_PALETTE, palette.name) }
        _palette.value = palette
    }

    private fun readUiMode(): UiMode? =
        prefs.getString(KEY_MODE, null)?.let { runCatching { UiMode.valueOf(it) }.getOrNull() }

    private fun readLayout(): TivanLayout {
        prefs.getString(KEY_LAYOUT, null)
            ?.let { runCatching { TivanLayout.valueOf(it) }.getOrNull() }
            ?.let { return it }
        // Migrate from the old single three-way theme key, if present.
        return when (prefs.getString(KEY_LEGACY_THEME, null)) {
            "LINEN" -> TivanLayout.CARD
            "OBSIDIAN", "INSTRUMENT" -> TivanLayout.FLAT
            else -> TivanLayout.CARD
        }
    }

    private fun readPalette(): TivanPalette {
        prefs.getString(KEY_PALETTE, null)
            ?.let { runCatching { TivanPalette.valueOf(it) }.getOrNull() }
            ?.let { return it }
        // Migrate from the old single three-way theme key, if present.
        return when (prefs.getString(KEY_LEGACY_THEME, null)) {
            "LINEN" -> TivanPalette.CREAM
            "OBSIDIAN" -> TivanPalette.DARK
            "INSTRUMENT" -> TivanPalette.DARK
            else -> TivanPalette.CREAM
        }
    }

    private companion object {
        const val KEY_MODE = "ui_mode"
        const val KEY_LAYOUT = "tivan_layout"
        const val KEY_PALETTE = "tivan_palette"
        const val KEY_VOICE = "voice_enabled"
        /** Old single-axis key ("app_theme": OBSIDIAN / LINEN / INSTRUMENT), read only for migration. */
        const val KEY_LEGACY_THEME = "app_theme"
    }
}
