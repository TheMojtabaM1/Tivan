package ir.tivan.controller.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import ir.tivan.controller.TivanApp
import ir.tivan.controller.data.LogDirection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (messages.isEmpty()) return

        val sender = messages[0].originatingAddress ?: return
        val body = messages.joinToString("") { it.messageBody ?: "" }
        val app = context.applicationContext as? TivanApp ?: return

        CoroutineScope(Dispatchers.IO).launch {
            val suffix = sender.filter { it.isDigit() }.takeLast(10)
            val matched = app.allDevicesSnapshot()
                .firstOrNull { it.phoneNumber.filter { c -> c.isDigit() }.takeLast(10) == suffix }
                ?: return@launch
            app.repository.addLog(matched.id, LogDirection.IN, body)
            app.reportBus.tryEmit(matched.id to body)
        }
    }
}
