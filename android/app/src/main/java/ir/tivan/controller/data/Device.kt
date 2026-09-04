package ir.tivan.controller.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A TIVAN S44T controller the user has paired with.
 *
 * [outputNames] and [inputMessages] mirror whatever the user configured on the
 * device itself via NAMEOUTx / PAYAMEINx. They are what the device echoes back
 * in its reports, so the SMS parser keys off them — see [ir.tivan.controller.sms.StatusParser].
 */
@Entity(tableName = "devices")
data class Device(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phoneNumber: String,
    val icon: String = "🏠",

    /** How many output/input channels this controller has: 2, 4, or 8. */
    @ColumnInfo(defaultValue = "4")
    val channelCount: Int = 4,

    /** NAMEOUT1..4 — max 14 chars each, as sent back in reports ("PUMP ON"). */
    val outputNames: List<String> = DEFAULT_OUTPUT_NAMES,

    /** Local-only emoji per output, picked by the user alongside the name. */
    val outputIcons: List<String> = DEFAULT_OUTPUT_ICONS,

    /** Local-only emoji per input. */
    val inputIcons: List<String> = DEFAULT_INPUT_ICONS,

    /** PAYAMEIN1..4 — max 24 chars each, the text the device sends when an input trips. */
    val inputMessages: List<String> = DEFAULT_INPUT_MESSAGES,

    /** MODEx0/1/2 per input: 0 = OFF, 1 = N.O, 2 = N.C. */
    val inputModes: List<Int> = listOf(1, 1, 1, 1),

    /** SETxy response level per input (0..9). */
    val inputResponses: List<Int> = listOf(0, 0, 0, 0),

    /** SEC0Z / SEC1Z / SEC2Z — number of armed zones. */
    val securityZones: Int = 2,

    val autoReportMode: Int = 1,   // REP0..REP3
    val outputMemory: Boolean = false, // MEM0/MEM1
    val buzzer: Boolean = true,        // BUZZER0/BUZZER1
    val remoteLatch: Boolean = true,   // REMOTEFF / REMOTEPL
    val remoteSecurityMode: Boolean = false, // RMODESE / RMODENO

    val isSelected: Boolean = false
) {
    fun outputName(index: Int): String =
        outputNames.getOrNull(index)?.takeIf { it.isNotBlank() } ?: "OUT${index + 1}"

    fun inputMessage(index: Int): String =
        inputMessages.getOrNull(index)?.takeIf { it.isNotBlank() } ?: "In${index + 1} Triggered"

    fun outputIcon(index: Int): String =
        outputIcons.getOrNull(index)?.takeIf { it.isNotBlank() }
            ?: DEFAULT_OUTPUT_ICONS.getOrElse(index) { "🔌" }

    fun inputIcon(index: Int): String =
        inputIcons.getOrNull(index)?.takeIf { it.isNotBlank() }
            ?: DEFAULT_INPUT_ICONS.getOrElse(index) { "📥" }

    companion object {
        /** The only channel counts the S44T family ships in. */
        val CHANNEL_OPTIONS = listOf(2, 4, 8)

        val DEFAULT_OUTPUT_NAMES = listOf("OUT1", "OUT2", "OUT3", "OUT4")
        val DEFAULT_INPUT_MESSAGES =
            listOf("In1 Triggered", "In2 Triggered", "In3 Triggered", "In4 Triggered")
        val DEFAULT_OUTPUT_ICONS = listOf("💡", "🔌", "💧", "🚪")
        val DEFAULT_INPUT_ICONS = listOf("🚪", "👁", "💨", "📥")

        /** Emoji offered in the rename sheet for outputs and inputs. */
        val ICON_CHOICES = listOf(
            "💡", "🔌", "💧", "🚪", "🌀", "🔥", "❄️", "🌱",
            "🚜", "🏭", "🅿️", "🔔", "📷", "👁", "💨", "🛗",
            "⚡", "🪟", "🚿", "🧊", "☀️", "🌡", "🔒", "📡",
            "🔓", "🚨", "🛡", "🌊", "🚰", "🛁", "🧯", "🪣",
            "🔋", "🪫", "🔦", "🕯", "🚧", "🚦", "🚗", "🚲",
            "🏠", "🏡", "🏢", "🏪", "🏭", "🏗", "⛩", "🌳",
            "🌴", "🌵", "🍃", "🐝", "🐟", "🦟", "🐛", "🌸",
            "❤️", "⚠️", "🔊", "🔇", "📶", "🌡️", "💦", "☔",
            "🌤", "⛅", "🌩", "❄", "🧭", "⏱", "⏲", "🔧",
            "🔩", "🛠", "⚙️", "🧰", "🪛", "🧲", "🔬", "📡"
        )
    }
}

class Converters {
    @androidx.room.TypeConverter
    fun fromList(list: List<String>): String = list.joinToString("§")

    @androidx.room.TypeConverter
    fun toList(data: String): List<String> = if (data.isBlank()) emptyList() else data.split("§")

    @androidx.room.TypeConverter
    fun fromIntList(list: List<Int>): String = list.joinToString(",")

    @androidx.room.TypeConverter
    fun toIntList(data: String): List<Int> =
        if (data.isBlank()) emptyList() else data.split(",").mapNotNull { it.trim().toIntOrNull() }

    @androidx.room.TypeConverter
    fun fromLongList(list: List<Long>): String = list.joinToString(",")

    @androidx.room.TypeConverter
    fun toLongList(data: String): List<Long> =
        if (data.isBlank()) emptyList() else data.split(",").mapNotNull { it.trim().toLongOrNull() }
}
