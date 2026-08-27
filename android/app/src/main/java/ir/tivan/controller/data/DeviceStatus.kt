package ir.tivan.controller.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Last known state of a device, persisted so the UI can show real data
 * immediately on launch instead of blank placeholders.
 *
 * Every field carries its own `*At` timestamp because the device reports
 * different facts in different SMS messages — the antenna level may be an hour
 * old while the output states are seconds old. The UI renders each timestamp as
 * "۳ دقیقه پیش" underneath the value so stale data is never mistaken for live data.
 * A `*At` of 0 means "never received".
 */
@Entity(tableName = "device_status")
data class DeviceStatus(
    @PrimaryKey val deviceId: Long,

    /** "1" = on, "0" = off, "?" = unknown — one char per output. */
    val outputStates: String = "????",
    val outputsAt: Long = 0,

    /**
     * Live input state as reported by REPORT: "1" = closed, "0" = open,
     * "?" = unknown. Only REPORT writes here — a trigger notification is an
     * event, not a state, and belongs in [inputTriggeredAt].
     */
    val inputStates: String = "????",
    val inputsAt: Long = 0,

    /**
     * When each input last fired, one epoch-millis value per input (0 = never).
     * Kept separate from [inputStates] and per-input rather than shared, so one
     * input tripping cannot make the other three look like they just fired —
     * and so a past trigger cannot pin an input to "triggered" forever.
     */
    @ColumnInfo(defaultValue = "0,0,0,0")
    val inputTriggeredAt: List<Long> = listOf(0, 0, 0, 0),

    val securityArmed: Boolean? = null,
    val securityAt: Long = 0,

    /** ANTEN reply: "متوسط" / "خوب" / "عالی". */
    val antenna: String? = null,
    val antennaAt: Long = 0,

    /** ?temp reply, in Celsius. */
    val temperature: String? = null,
    val temperatureAt: Long = 0,

    /** TESTxx replies — slot number to stored phone number. */
    val adminNumbers: List<String> = emptyList(),
    val adminsAt: Long = 0,

    /** Raw body of the most recent REPORT reply. */
    val lastReport: String? = null,
    val lastReportAt: Long = 0,

    /** Any incoming SMS at all, used for the "last contact" line. */
    val lastContactAt: Long = 0
) {
    fun output(index: Int): Boolean? = outputStates.getOrNull(index).toTriState()
    fun input(index: Int): Boolean? = inputStates.getOrNull(index).toTriState()

    fun withOutput(index: Int, on: Boolean, at: Long): DeviceStatus =
        copy(outputStates = outputStates.replaceAt(index, if (on) '1' else '0'), outputsAt = at)

    /** Live state from a REPORT reply. */
    fun withInput(index: Int, closed: Boolean, at: Long): DeviceStatus =
        copy(inputStates = inputStates.replaceAt(index, if (closed) '1' else '0'), inputsAt = at)

    /** Records a trigger event without touching the live state. */
    fun withInputTriggered(index: Int, at: Long): DeviceStatus {
        if (index !in 0..3) return this
        val times = inputTriggeredAt.padTo4()
        return copy(inputTriggeredAt = times.toMutableList().also { it[index] = at })
    }

    fun triggeredAt(index: Int): Long = inputTriggeredAt.padTo4().getOrElse(index) { 0L }

    /**
     * True only while a trigger is fresh. Without a window the badge would stay
     * lit forever, since the controller never reports "this input went idle".
     */
    fun recentlyTriggered(index: Int, now: Long = System.currentTimeMillis()): Boolean {
        val at = triggeredAt(index)
        return at > 0L && now - at < TRIGGER_HIGHLIGHT_MS
    }

    companion object {
        /** How long an input keeps its "just triggered" highlight. */
        const val TRIGGER_HIGHLIGHT_MS = 5 * 60 * 1000L

        fun empty(deviceId: Long) = DeviceStatus(deviceId = deviceId)
    }
}

private fun List<Long>.padTo4(): List<Long> =
    if (size >= 4) this else this + List(4 - size) { 0L }

private fun Char?.toTriState(): Boolean? = when (this) {
    '1' -> true
    '0' -> false
    else -> null
}

private fun String.replaceAt(index: Int, c: Char): String {
    val padded = padEnd(4, '?')
    if (index !in 0..3) return padded
    return padded.toCharArray().also { it[index] = c }.concatToString()
}
