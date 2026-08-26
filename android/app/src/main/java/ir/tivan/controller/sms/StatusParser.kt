package ir.tivan.controller.sms

import ir.tivan.controller.data.Device
import ir.tivan.controller.data.DeviceStatus

/**
 * Turns an incoming SMS from a TIVAN S44T into state updates.
 *
 * The device does not speak a fixed protocol on the way back: per the manual it
 * echoes whatever labels the user configured on it.
 *
 *  - Outputs report as `<NAMEOUTx> ON` / `<NAMEOUTx> OFF`, where the name
 *    defaults to `OUT1`..`OUT4` but becomes e.g. `PUMP` after `NAMEOUT1:PUMP`.
 *  - Inputs report as the free text set by `PAYAMEINx:` (default `In1 Triggered`).
 *
 * So parsing is driven by the [Device] record rather than hardcoded patterns:
 * rename an output in the app and the parser follows it. Names are matched
 * longest-first so `LAMP OTAGH` wins over a hypothetical `LAMP`, and the
 * defaults stay active as a fallback in case the device was renamed outside the
 * app and our copy is stale.
 */
object StatusParser {

    data class Result(
        val status: DeviceStatus,
        val outputChanges: List<OutputChange> = emptyList(),
        val triggeredInputs: List<Int> = emptyList(),
        val recognized: Boolean = false
    )

    data class OutputChange(val index: Int, val on: Boolean)

    fun parse(
        device: Device,
        body: String,
        previous: DeviceStatus,
        now: Long = System.currentTimeMillis()
    ): Result {
        val text = body.trim()
        if (text.isEmpty()) return Result(previous.copy(lastContactAt = now))

        var status = previous.copy(lastContactAt = now)
        val outputChanges = mutableListOf<OutputChange>()
        val triggered = mutableListOf<Int>()
        var recognized = false

        // ---- outputs: "<name> ON" / "<name> OFF" -------------------------------
        // Candidate names: whatever the user configured, plus the factory defaults.
        val candidates = buildList {
            for (i in 0..3) {
                add(device.outputName(i) to i)
                add("OUT${i + 1}" to i)
            }
        }.distinctBy { it.first.uppercase() to it.second }
            .sortedByDescending { it.first.length } // longest name wins

        val upper = text.uppercase()
        val consumed = BooleanArray(upper.length)

        for ((name, index) in candidates) {
            if (name.isBlank()) continue
            val pattern = Regex(
                "${Regex.escape(name.uppercase())}\\s*[:\\-]?\\s*(ON|OFF)\\b",
                RegexOption.IGNORE_CASE
            )
            for (m in pattern.findAll(upper)) {
                // Skip a hit that overlaps a longer name already matched.
                if ((m.range).any { consumed[it] }) continue
                m.range.forEach { consumed[it] = true }
                val on = m.groupValues[1].equals("ON", ignoreCase = true)
                status = status.withOutput(index, on, now)
                outputChanges += OutputChange(index, on)
                recognized = true
            }
        }

        // ---- inputs: custom PAYAMEIN text, or the default "InX Triggered" -------
        for (i in 0..3) {
            val custom = device.inputMessage(i)
            val default = "IN${i + 1} TRIGGERED"
            val hit = (custom.isNotBlank() && upper.contains(custom.uppercase())) ||
                upper.contains(default)
            if (hit) {
                status = status.withInput(i, triggered = true, at = now)
                triggered += i
                recognized = true
            }
        }

        // ---- REPORT reply: positional 0/1 digits --------------------------------
        parseReport(text)?.let { (outs, ins) ->
            outs.forEachIndexed { i, v ->
                if (v != null) {
                    status = status.withOutput(i, v, now)
                    outputChanges += OutputChange(i, v)
                }
            }
            ins.forEachIndexed { i, v -> if (v != null) status = status.withInput(i, v, now) }
            status = status.copy(lastReport = text, lastReportAt = now)
            recognized = true
        }

        // ---- antenna -----------------------------------------------------------
        antennaOf(text)?.let {
            status = status.copy(antenna = it, antennaAt = now)
            recognized = true
        }

        // ---- temperature -------------------------------------------------------
        temperatureOf(text)?.let {
            status = status.copy(temperature = it, temperatureAt = now)
            recognized = true
        }

        // ---- security ----------------------------------------------------------
        securityOf(upper)?.let {
            status = status.copy(securityArmed = it, securityAt = now)
            recognized = true
        }

        return Result(status, outputChanges, triggered, recognized)
    }

    /**
     * A REPORT reply lists each output and input as 1 (on/closed) or 0.
     * Formats vary between firmware revisions, so this only accepts a line that
     * clearly pairs a known label with a single digit.
     */
    private fun parseReport(text: String): Pair<Array<Boolean?>, Array<Boolean?>>? {
        val outs = arrayOfNulls<Boolean>(4)
        val ins = arrayOfNulls<Boolean>(4)
        var any = false

        Regex("OUT\\s*([1-4])\\s*[:=\\-]?\\s*([01])\\b", RegexOption.IGNORE_CASE)
            .findAll(text).forEach {
                outs[it.groupValues[1].toInt() - 1] = it.groupValues[2] == "1"
                any = true
            }
        Regex("IN\\s*([1-4])\\s*[:=\\-]?\\s*([01])\\b", RegexOption.IGNORE_CASE)
            .findAll(text).forEach {
                ins[it.groupValues[1].toInt() - 1] = it.groupValues[2] == "1"
                any = true
            }
        return if (any) outs to ins else null
    }

    private fun antennaOf(text: String): String? {
        val t = text.lowercase()
        return when {
            "عالی" in text || "excellent" in t -> "عالی"
            "خوب" in text || "good" in t -> "خوب"
            "متوسط" in text || "medium" in t || "normal" in t -> "متوسط"
            "ضعیف" in text || "weak" in t || "low" in t -> "ضعیف"
            else -> null
        }
    }

    private fun temperatureOf(text: String): String? {
        val m = Regex(
            "(?:TEMP|TEMPERATURE|دما)\\D{0,12}?(-?\\d{1,3})",
            RegexOption.IGNORE_CASE
        ).find(text) ?: return null
        val v = m.groupValues[1].toIntOrNull() ?: return null
        return if (v in -25..125) "$v" else null
    }

    private fun securityOf(upper: String): Boolean? = when {
        Regex("\\bSEC(URITY)?\\s*(IS\\s*)?(ON|ENABLE[D]?|ACTIVE)\\b").containsMatchIn(upper) -> true
        Regex("\\bSEC(URITY)?\\s*(IS\\s*)?(OFF|DISABLE[D]?|DEACTIVE)\\b").containsMatchIn(upper) -> false
        upper.contains("دزدگیر فعال") -> true
        upper.contains("دزدگیر غیرفعال") -> false
        else -> null
    }

    /**
     * True when [body] confirms the output at [index] reached [expectedOn].
     * Used to settle a pending command instead of trusting the send itself.
     */
    fun confirmsOutput(device: Device, body: String, index: Int, expectedOn: Boolean): Boolean {
        val names = listOf(device.outputName(index), "OUT${index + 1}")
        val want = if (expectedOn) "ON" else "OFF"
        return names.any { n ->
            n.isNotBlank() && Regex(
                "${Regex.escape(n)}\\s*[:\\-]?\\s*$want\\b",
                RegexOption.IGNORE_CASE
            ).containsMatchIn(body)
        }
    }

    fun confirmsSecurity(body: String, expectedArmed: Boolean): Boolean =
        securityOf(body.uppercase()) == expectedArmed
}
