package ir.tivan.controller.schedule

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ir.tivan.controller.TivanApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Alarms don't survive a reboot — re-arm every enabled schedule once the device is back up. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        val app = context.applicationContext as TivanApp
        CoroutineScope(Dispatchers.IO).launch {
            try {
                app.database.scheduleDao().getAllEnabled().forEach { ScheduleScheduler.arm(context, it) }
            } finally {
                pending.finish()
            }
        }
    }
}
