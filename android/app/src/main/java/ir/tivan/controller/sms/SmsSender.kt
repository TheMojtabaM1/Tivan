package ir.tivan.controller.sms

import android.content.Context
import android.telephony.SmsManager

object SmsSender {
    /** Sends [command] as a plain-text SMS to [phoneNumber]. Caller must hold SEND_SMS permission. */
    fun send(context: Context, phoneNumber: String, command: String) {
        val manager = context.getSystemService(SmsManager::class.java) ?: SmsManager.getDefault()
        val parts = manager.divideMessage(command)
        if (parts.size > 1) {
            manager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
        } else {
            manager.sendTextMessage(phoneNumber, null, command, null, null)
        }
    }
}
