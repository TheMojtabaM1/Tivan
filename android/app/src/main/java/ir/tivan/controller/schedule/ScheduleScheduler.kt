package ir.tivan.controller.schedule

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import ir.tivan.controller.data.Schedule
import java.util.Calendar

/**
 * Arms/disarms the OS alarm for one edge (on-time or off-time) of a
 * [Schedule] row. Each row owns two independent alarms — one that fires the
 * "on" command at [Schedule.startHour]:[Schedule.startMinute], one that
 * fires "off" at the end time — so a schedule crossing midnight or covering
 * only some days of the week still behaves correctly for each edge on its
 * own.
 */
object ScheduleScheduler {
    const val EXTRA_SCHEDULE_ID = "schedule_id"
    const val EXTRA_TURN_ON = "turn_on"

    private fun requestCode(scheduleId: Long, turnOn: Boolean) =
        (scheduleId * 2 + if (turnOn) 1 else 0).toInt()

    private fun pendingIntent(context: Context, schedule: Schedule, turnOn: Boolean): PendingIntent {
        val intent = Intent(context, ScheduleAlarmReceiver::class.java).apply {
            putExtra(EXTRA_SCHEDULE_ID, schedule.id)
            putExtra(EXTRA_TURN_ON, turnOn)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode(schedule.id, turnOn),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** Arms both edges for the next matching day, at or after "now". */
    fun arm(context: Context, schedule: Schedule) {
        if (!schedule.enabled) return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        armEdge(context, alarmManager, schedule, turnOn = true, schedule.startHour, schedule.startMinute)
        armEdge(context, alarmManager, schedule, turnOn = false, schedule.endHour, schedule.endMinute)
    }

    fun disarm(context: Context, schedule: Schedule) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent(context, schedule, true))
        alarmManager.cancel(pendingIntent(context, schedule, false))
    }

    private fun armEdge(
        context: Context,
        alarmManager: AlarmManager,
        schedule: Schedule,
        turnOn: Boolean,
        hour: Int,
        minute: Int
    ) {
        val next = nextOccurrence(schedule.days, hour, minute) ?: return
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            next.timeInMillis,
            pendingIntent(context, schedule, turnOn)
        )
    }

    /** The next moment, today or up to 7 days out, that matches [days] and the given clock time. */
    fun nextOccurrence(days: Int, hour: Int, minute: Int): Calendar? {
        if (days == 0) return null
        val now = Calendar.getInstance()
        for (offset in 0..7) {
            val candidate = (now.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, offset)
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val dayMatches = (days and Schedule.dayBit(candidate.get(Calendar.DAY_OF_WEEK))) != 0
            if (dayMatches && candidate.after(now)) return candidate
        }
        return null
    }
}
