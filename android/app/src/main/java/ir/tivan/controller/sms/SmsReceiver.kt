package ir.tivan.controller.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import ir.tivan.controller.TivanApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Receives SMS when the app holds RECEIVE_SMS but is not the default SMS app.
 *
 * When it *is* the default app both this and [SmsDeliverReceiver] fire for the
 * same message; [TivanApp.onIncomingSms] drops the duplicate. Unlike the deliver
 * receiver this one must not write to the SMS provider — the real default app
 * is already doing that, and a second insert would duplicate every message in
 * the user's inbox.
 */
class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (messages.isEmpty()) return

        val sender = messages[0].originatingAddress ?: return
        val body = messages.joinToString("") { it.messageBody.orEmpty() }
        val app = context.applicationContext as? TivanApp ?: return

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                app.onIncomingSms(sender, body)
            } finally {
                pending.finish()
            }
        }
    }
}
