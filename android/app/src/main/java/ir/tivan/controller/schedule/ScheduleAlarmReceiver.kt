package ir.tivan.controller.schedule

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ir.tivan.controller.TivanApp
import ir.tivan.controller.data.Device
import ir.tivan.controller.data.LogDirection
import ir.tivan.controller.data.Schedule
import ir.tivan.controller.sms.SmsSender
import ir.tivan.controller.tts.TivanSpeaker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Fires one edge (on or off) of a [Schedule]: sends the SMS command to the
 * device, speaks it if voice is enabled, then re-arms this same edge for its
 * next matching day so the schedule repeats weekly forever until the user
 * deletes or disables it.
 */
class ScheduleAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val scheduleId = intent.getLongExtra(ScheduleScheduler.EXTRA_SCHEDULE_ID, -1)
        val turnOn = intent.getBooleanExtra(ScheduleScheduler.EXTRA_TURN_ON, true)
        if (scheduleId < 0) return

        val pending = goAsync()
        val app = context.applicationContext as TivanApp
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val schedule = app.database.scheduleDao().getById(scheduleId)
                if (schedule != null && schedule.enabled) {
                    val device = app.database.deviceDao().getById(schedule.deviceId)
                    if (device != null) {
                        fire(context, device, schedule, turnOn)
                        // Re-arm just this edge for its next occurrence, one week out.
                        ScheduleScheduler.arm(context, schedule)
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun fire(context: Context, device: Device, schedule: Schedule, turnOn: Boolean) {
        val app = context.applicationContext as TivanApp
        if (!schedule.isOutput) return // inputs are read-only triggers, not schedulable
        val command = "${schedule.channelIndex + 1}${if (turnOn) 1 else 0}"
        val result = SmsSender.send(context, device.phoneNumber, command)
        if (result != SmsSender.Result.Failed) {
            app.database.messageLogDao().insert(
                ir.tivan.controller.data.MessageLog(
                    deviceId = device.id,
                    direction = LogDirection.OUT,
                    body = "$command (زمان‌بندی)"
                )
            )
        }

        if (app.preferences.voiceEnabled.value) {
            val name = device.outputName(schedule.channelIndex)
            val verb = if (turnOn) "روشن شد" else "خاموش شد"
            TivanSpeaker.speak("$name $verb")
        }
    }
}
