package ir.tivan.controller.sms

/**
 * Builds the exact SMS command strings defined in the TIVAN S44T manual.
 * All commands are English, no spaces, case-insensitive on the device side.
 */
object Commands {

    // --- Outputs: on/off ---
    fun outputOn(output: Int) = "${output}1"
    fun outputOff(output: Int) = "${output}0"

    // Timer in seconds (1..99): e.g. 101..199 for output 1
    fun outputTimerSeconds(output: Int, seconds: Int): String {
        val s = seconds.coerceIn(1, 99)
        return "$output${s.toString().padStart(2, '0')}"
    }

    // Timer in minutes (1..999): e.g. 1001..1999 for output 1
    fun outputTimerMinutes(output: Int, minutes: Int): String {
        val m = minutes.coerceIn(1, 999)
        return "$output${m.toString().padStart(3, '0')}"
    }

    // --- Missed call action config ---
    fun missedCallSet(command: String = "") = if (command.isBlank()) "MISSED:" else "MISSED:$command"

    // --- Input trigger mode: OFF / N.O / N.C ---
    enum class InputMode(val suffix: Int) { OFF(0), NO(1), NC(2) }
    fun inputMode(input: Int, mode: InputMode) = "MODE${input}${mode.suffix}"

    // --- Input trigger response: SETxy where x=input(1-4), y=0..9 ---
    fun inputResponse(input: Int, level: Int) = "SET${input}${level.coerceIn(0, 9)}"

    // --- Sub admins TEL01-04, regular users TEL05-40 ---
    fun saveNumber(slot: Int, phoneLocal0: String) = "TEL${slot.toString().padStart(2, '0')}$phoneLocal0"
    fun readSlot(slot: Int) = "TEST${slot.toString().padStart(2, '0')}"
    fun deleteSlot(slot: Int) = "DELTEL${slot.toString().padStart(2, '0')}"
    fun deleteAllNumbers() = "DELALLTEL"

    // --- Reports ---
    fun report() = "REPORT"
    fun antenna() = "ANTEN"

    enum class ReportMode(val code: Int) { NONE(0), USER_ONLY(1), USER_AND_ADMIN(2), ADMIN_ONLY(3) }
    fun autoReport(mode: ReportMode) = "REP${mode.code}"

    fun outputMemory(enabled: Boolean) = "MEM${if (enabled) 1 else 0}"
    fun buzzer(enabled: Boolean) = "BUZZER${if (enabled) 1 else 0}"

    fun nameOutput(output: Int, name: String) = "NAMEOUT$output:${name.take(14)}"
    fun triggerMessage(input: Int, message: String) = "PAYAMEIN$input:${message.take(24)}"

    fun listen() = "SHONOOD"

    // --- Delay-on timers (range e.g. 2001..2999 for output 2 = "2" + 3-digit minutes) ---
    fun outputDelayOnMinutes(output: Int, minutes: Int): String {
        val m = minutes.coerceIn(1, 999)
        return "ONDT$output${m.toString().padStart(3, '0')}"
    }
    fun outputDelayOnPulseMinutes(output: Int, minutes: Int): String {
        val m = minutes.coerceIn(1, 999)
        return "PONDT$output${m.toString().padStart(3, '0')}"
    }

    // --- Security ---
    enum class SecurityZones(val code: String) { OFF("SEC0Z"), ONE_ZONE("SEC1Z"), TWO_ZONE("SEC2Z") }
    fun securityZones(zones: SecurityZones) = zones.code
    fun securityOn() = "SECON"
    fun securityOff() = "SECOF"

    // --- Remote ---
    fun remoteMode(latch: Boolean) = if (latch) "REMOTEFF" else "REMOTEPL"
    fun remoteFunction(security: Boolean) = if (security) "RMODESE" else "RMODENO"

    // --- Temperature ---
    fun readTemperature() = "?temp"

    fun tempHighA(value: Int) = "TEMPHIA$value"
    fun tempLowA(value: Int) = "TEMPLOA$value"
    fun tempHighB(value: Int) = "TEMPHIB$value"
    fun tempLowB(value: Int) = "TEMPLOB$value"

    data class ThermostatConfig(
        val enabled: Boolean,
        val heating: Boolean, // true = heating, false = cooling
        val autoMode: Boolean,
        val smsOnTrigger: Boolean,
        val callOnTrigger: Boolean
    ) {
        fun toBits(): String = buildString {
            append(if (enabled) 1 else 0)
            append(if (heating) 1 else 0)
            append(if (autoMode) 1 else 0)
            append(if (smsOnTrigger) 1 else 0)
            append(if (callOnTrigger) 1 else 0)
        }
    }

    fun thermostatA(config: ThermostatConfig) = "TEMPSETA${config.toBits()}"
    fun thermostatB(config: ThermostatConfig) = "TEMPSETB${config.toBits()}"

    // --- USSD passthrough (charge balance etc.) ---
    fun raw(command: String) = command

    // Operator SIM-language codes (dialed via phone dialer, informational only)
    val simLangCodes = mapOf(
        "همراه اول" to "*198*2#",
        "ایرانسل" to "*555*4*3*2#",
        "رایتل" to "*720*7*1*3#"
    )
    val chargeUssdCodes = mapOf(
        "همراه اول" to "*140*11#",
        "ایرانسل" to "*141*1#",
        "رایتل" to "*140#"
    )
}
