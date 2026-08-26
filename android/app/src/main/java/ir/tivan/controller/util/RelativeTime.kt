package ir.tivan.controller.util

import java.util.concurrent.TimeUnit

/**
 * Persian "how long ago" labels for cached device data, e.g. "۳ دقیقه پیش".
 * A timestamp of 0 means the value was never received.
 */
object RelativeTime {

    fun ago(timestamp: Long, now: Long = System.currentTimeMillis()): String {
        if (timestamp <= 0L) return "هنوز دریافت نشده"
        val delta = (now - timestamp).coerceAtLeast(0L)

        val seconds = TimeUnit.MILLISECONDS.toSeconds(delta)
        if (seconds < 45) return "همین الان"

        val minutes = TimeUnit.MILLISECONDS.toMinutes(delta)
        if (minutes < 60) return "${fa(minutes)} دقیقه پیش"

        val hours = TimeUnit.MILLISECONDS.toHours(delta)
        if (hours < 24) return "${fa(hours)} ساعت پیش"

        val days = TimeUnit.MILLISECONDS.toDays(delta)
        if (days < 30) return "${fa(days)} روز پیش"

        val months = days / 30
        if (months < 12) return "${fa(months)} ماه پیش"

        return "${fa(months / 12)} سال پیش"
    }

    /** True when the value is old enough that the UI should visually de-emphasise it. */
    fun isStale(timestamp: Long, now: Long = System.currentTimeMillis()): Boolean =
        timestamp <= 0L || now - timestamp > TimeUnit.HOURS.toMillis(6)

    /** Latin digits to Persian digits, so numbers match the Arad typeface. */
    fun fa(value: Long): String = value.toString().map { c ->
        if (c in '0'..'9') PERSIAN_DIGITS[c - '0'] else c
    }.joinToString("")

    fun fa(value: Int): String = fa(value.toLong())

    fun fa(text: String): String = text.map { c ->
        if (c in '0'..'9') PERSIAN_DIGITS[c - '0'] else c
    }.joinToString("")

    private val PERSIAN_DIGITS = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
}
