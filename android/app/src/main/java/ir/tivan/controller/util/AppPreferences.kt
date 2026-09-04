package ir.tivan.controller.util

import android.content.Context
import androidx.core.content.edit
import ir.tivan.controller.ui.theme.AppTheme
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
 * Small app-wide settings: which of the three visual themes is active, and
 * whether the UI shows the simple or advanced tab set.
 *
 * Backed by [android.content.SharedPreferences] rather than DataStore — there
 * are exactly two values, both read once at process start and written rarely
 * from the main thread by a settings toggle, so the extra dependency and
 * async API surface of DataStore would buy nothing here.
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

    private val _theme = MutableStateFlow(readTheme())
    val theme: StateFlow<AppTheme> = _theme.asStateFlow()

    fun setUiMode(mode: UiMode) {
        prefs.edit { putString(KEY_MODE, mode.name) }
        _uiMode.value = mode
    }

    fun setTheme(theme: AppTheme) {
        prefs.edit { putString(KEY_THEME, theme.name) }
        _theme.value = theme
    }

    private fun readUiMode(): UiMode? =
        prefs.getString(KEY_MODE, null)?.let { runCatching { UiMode.valueOf(it) }.getOrNull() }

    private fun readTheme(): AppTheme =
        prefs.getString(KEY_THEME, null)
            ?.let { runCatching { AppTheme.valueOf(it) }.getOrNull() }
            ?: AppTheme.LINEN

    private companion object {
        const val KEY_MODE = "ui_mode"
        const val KEY_THEME = "app_theme"
    }
}
