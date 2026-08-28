package ir.tivan.controller.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import ir.tivan.controller.sms.SmsNotificationListener

/**
 * Notification access — the fallback route for reading controller replies.
 *
 * Unlike the SMS permissions, which are hard restricted and simply cannot be
 * granted on a sideloaded install, this one is a *special* permission: the user
 * can always reach it in Settings. On Android 13+ a sideloaded app may first
 * need App info → ⋮ → "Allow restricted settings" before the toggle becomes
 * usable, which is the step that genuinely applies here.
 */
object NotificationAccess {

    fun isEnabled(context: Context): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        ).orEmpty()
        if (enabled.isBlank()) return false

        val component = ComponentName(context, SmsNotificationListener::class.java)
        val flat = component.flattenToString()
        val short = component.flattenToShortString()
        return enabled.split(':').any { it == flat || it == short }
    }

    /** Opens the notification-access list, falling back to app settings. */
    fun openSettings(context: Context) {
        val candidates = listOf(
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS),
            Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
        )
        for (intent in candidates) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (runCatching { context.startActivity(intent); true }.getOrDefault(false)) return
        }
        SmsPermissions.openAppSettings(context)
    }
}
