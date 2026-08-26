package ir.tivan.controller.data

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

    /** "1" = triggered/closed, "0" = idle, "?" = unknown. */
    val inputStates: String = "????",
    val inputsAt: Long = 0,

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

    fun withInput(index: Int, triggered: Boolean, at: Long): DeviceStatus =
        copy(inputStates = inputStates.replaceAt(index, if (triggered) '1' else '0'), inputsAt = at)

    companion object {
        fun empty(deviceId: Long) = DeviceStatus(deviceId = deviceId)
    }
}

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
