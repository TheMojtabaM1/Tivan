package ir.tivan.controller.sms

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.telephony.SmsManager
import ir.tivan.controller.util.SmsPermissions

object SmsSender {

    enum class Result {
        /** Sent silently in the background. */
        Sent,

        /** Handed to the user's SMS app; they still have to press send. */
        Handoff,

        /** Neither path worked. */
        Failed
    }

    /**
     * Sends [command] to [phoneNumber].
     *
     * With SEND_SMS the message goes out silently. Without it — the normal case
     * for a sideloaded build, where the SMS permissions are hard restricted and
     * cannot be granted from Settings — the command is handed to the user's own
     * SMS app instead, so the app stays usable rather than failing silently.
     */
    fun send(context: Context, phoneNumber: String, command: String): Result {
        if (SmsPermissions.canSendDirectly(context)) {
            val sent = runCatching {
                val manager = context.getSystemService(SmsManager::class.java)
                    ?: @Suppress("DEPRECATION") SmsManager.getDefault()
                val parts = manager.divideMessage(command)
                if (parts.size > 1) {
                    manager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
                } else {
                    manager.sendTextMessage(phoneNumber, null, command, null, null)
                }
            }.isSuccess
            if (sent) return Result.Sent
        }
        return if (handOffToSmsApp(context, phoneNumber, command)) Result.Handoff else Result.Failed
    }

    private fun handOffToSmsApp(context: Context, phoneNumber: String, command: String): Boolean =
        runCatching {
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phoneNumber"))
                .putExtra("sms_body", command)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        }.getOrDefault(false)
}
