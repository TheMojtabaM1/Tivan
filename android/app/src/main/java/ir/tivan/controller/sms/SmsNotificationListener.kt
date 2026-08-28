package ir.tivan.controller.sms

import android.app.Notification
import android.provider.Telephony
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import ir.tivan.controller.TivanApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Reads controller replies out of the SMS app's notifications.
 *
 * The SMS permissions are hard restricted, so a sideloaded build cannot be
 * granted RECEIVE_SMS at all — Settings keeps "Allow" greyed out. Notification
 * access is a *special* permission instead, which the user can still turn on
 * (via App info → ⋮ → "Allow restricted settings" when Android blocks it), so
 * this is the one way to auto-confirm device state without a computer, a store
 * install, or taking over as the default SMS app.
 *
 * It is a fallback, not an equal: it only sees messages the SMS app actually
 * announces, so a muted conversation or a cleared notification means no update.
 * [SmsReceiver] is still preferred whenever RECEIVE_SMS is available, and
 * [TivanApp.onIncomingSms] discards whichever copy arrives second.
 */
class SmsNotificationListener : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val app = applicationContext as? TivanApp ?: return
        if (!isFromMessagingApp(sbn.packageName)) return

        val extras = sbn.notification?.extras ?: return
        // BIG_TEXT holds the untruncated body when the SMS app supplies it;
        // EXTRA_TEXT is the collapsed one-line version.
        val body = extras.textLines()
            ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()

        if (body.isBlank()) return

        scope.launch {
            app.onIncomingNotification(title = title, body = body)
        }
    }

    /**
     * Only the user's own SMS app is trusted as a source. Reading every app's
     * notifications would be both noisy and far more than this needs.
     */
    private fun isFromMessagingApp(packageName: String?): Boolean {
        if (packageName.isNullOrBlank()) return false
        val default = runCatching { Telephony.Sms.getDefaultSmsPackage(this) }.getOrNull()
        return packageName == default || packageName in KNOWN_SMS_APPS
    }

    /** Grouped notifications put each message in EXTRA_TEXT_LINES. */
    private fun android.os.Bundle.textLines(): String? =
        getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            ?.takeIf { it.isNotEmpty() }
            ?.joinToString("\n") { it.toString() }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private companion object {
        /** Used when the default SMS package cannot be resolved. */
        val KNOWN_SMS_APPS = setOf(
            "com.google.android.apps.messaging",
            "com.samsung.android.messaging",
            "com.android.messaging",
            "com.android.mms",
            "com.miui.smsextra",
            "com.xiaomi.mms"
        )
    }
}
