package ir.tivan.controller.util

import ir.tivan.controller.data.LogDirection
import ir.tivan.controller.data.MessageLog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** One day's total on-time for one output, for the usage-history chart. */
data class DayUsage(val label: String, val hours: Float, val isToday: Boolean)

/**
 * Reconstructs a rough on/off timeline for one output from the outgoing SMS
 * log and sums it into hours-per-day for the last [days] days. This is an
 * approximation from commands actually *sent*, not confirmed device state —
 * good enough for "about how long was this on today", not a precise meter,
 * and needs no extra hardware or protocol support from the controller.
 *
 * Recognizes plain on/off commands only ("<channel 1-8><0 or 1>", e.g. "11",
 * "40"), stripping the " (زمان‌بندی)" suffix schedule firings log with —
 * timer commands ("105", "1005", …) and everything else (REPORT, SEC1Z, …)
 * don't match this shape and are ignored.
 */
object UsageHistory {
    private val onOffPattern = Regex("^([1-8])([01])$")
    private val dayFormat = SimpleDateFormat("EEE", Locale("fa"))
    private val dateFormat = SimpleDateFormat("d", Locale("fa"))

    fun computeDailyHours(logs: List<MessageLog>, outputIndex: Int, days: Int = 7): List<DayUsage> {
        val channel = (outputIndex + 1).toString()
        val events = logs
            .asSequence()
            .filter { it.direction == LogDirection.OUT }
            .mapNotNull { log ->
                val body = log.body.substringBefore(" (").trim()
                val match = onOffPattern.matchEntire(body) ?: return@mapNotNull null
                if (match.groupValues[1] != channel) return@mapNotNull null
                log.timestamp to (match.groupValues[2] == "1")
            }
            .sortedBy { it.first }
            .toList()

        val now = System.currentTimeMillis()
        val dayStarts = (days - 1 downTo 0).map { offset ->
            (Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -offset)
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            })
        }
        val bucketMs = DoubleArray(days)

        var onSince: Long? = null
        for ((ts, turnedOn) in events) {
            if (turnedOn) {
                onSince = ts
            } else {
                onSince?.let { addInterval(it, ts, dayStarts, bucketMs) }
                onSince = null
            }
        }
        // Still on with no matching "off" yet — count up to now.
        onSince?.let { addInterval(it, now, dayStarts, bucketMs) }

        return dayStarts.mapIndexed { i, start ->
            val isToday = i == dayStarts.lastIndex
            DayUsage((if (days <= 7) dayFormat else dateFormat).format(start.time), (bucketMs[i] / 3_600_000.0).toFloat(), isToday)
        }
    }

    private fun addInterval(fromMs: Long, toMs: Long, dayStarts: List<Calendar>, bucketMs: DoubleArray) {
        if (toMs <= fromMs) return
        for (i in dayStarts.indices) {
            val dayStart = dayStarts[i].timeInMillis
            val dayEnd = dayStart + 24 * 3_600_000L
            val overlapStart = maxOf(fromMs, dayStart)
            val overlapEnd = minOf(toMs, dayEnd)
            if (overlapEnd > overlapStart) bucketMs[i] += (overlapEnd - overlapStart).toDouble()
        }
    }
}
