package ir.tivan.controller.sms

import ir.tivan.controller.data.Device
import ir.tivan.controller.data.DeviceStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StatusParserTest {

    private val now = 1_700_000_000_000L

    private fun device(
        outputNames: List<String> = Device.DEFAULT_OUTPUT_NAMES,
        inputMessages: List<String> = Device.DEFAULT_INPUT_MESSAGES
    ) = Device(
        id = 1,
        name = "test",
        phoneNumber = "09123456789",
        outputNames = outputNames,
        inputMessages = inputMessages
    )

    private fun parse(d: Device, body: String) =
        StatusParser.parse(d, body, DeviceStatus.empty(1), now)

    @Test
    fun `factory output names are recognised`() {
        val r = parse(device(), "OUT2 ON")
        assertEquals(true, r.status.output(1))
        assertTrue(r.recognized)
    }

    @Test
    fun `renamed output is recognised by its custom name`() {
        val d = device(outputNames = listOf("PUMP", "OUT2", "OUT3", "OUT4"))
        val r = parse(d, "PUMP ON")
        assertEquals(true, r.status.output(0))
    }

    /** Regression: the old parser only knew `OUT<n>`, so renaming blinded it. */
    @Test
    fun `renaming an output does not break parsing`() {
        val d = device(outputNames = listOf("PUMP", "OUT2", "OUT3", "OUT4"))
        assertFalse(parse(d, "PUMP OFF").outputChanges.isEmpty())
    }

    @Test
    fun `multi word names win over shorter overlapping ones`() {
        val d = device(outputNames = listOf("LAMP", "LAMP OTAGH", "OUT3", "OUT4"))
        val r = parse(d, "LAMP OTAGH ON")
        assertEquals(true, r.status.output(1))
        // Output 1 ("LAMP") must not also flip from the same text.
        assertEquals(null, r.status.output(0))
    }

    @Test
    fun `a report line updates several outputs at once`() {
        val d = device(outputNames = listOf("PUMP", "OUT2", "OUT3", "OUT4"))
        val r = parse(d, "PUMP ON  OUT2 OFF  OUT3 OFF")
        assertEquals(true, r.status.output(0))
        assertEquals(false, r.status.output(1))
        assertEquals(false, r.status.output(2))
        assertEquals(null, r.status.output(3))
    }

    @Test
    fun `default input trigger text is recognised`() {
        val r = parse(device(), "In3 Triggered")
        assertEquals(listOf(2), r.triggeredInputs)
        assertEquals(true, r.status.input(2))
    }

    @Test
    fun `custom input trigger text is recognised`() {
        val d = device(
            inputMessages = listOf(
                "DAR VORODI BAZ SHOD", "In2 Triggered", "In3 Triggered", "In4 Triggered"
            )
        )
        val r = parse(d, "DAR VORODI BAZ SHOD")
        assertEquals(listOf(0), r.triggeredInputs)
    }

    @Test
    fun `matching is case insensitive`() {
        val d = device(outputNames = listOf("Pump", "OUT2", "OUT3", "OUT4"))
        assertEquals(true, parse(d, "PUMP on").status.output(0))
    }

    @Test
    fun `REPORT digits set output and input state`() {
        val r = parse(device(), "OUT1:1 OUT2:0 IN1:1 IN2:0")
        assertEquals(true, r.status.output(0))
        assertEquals(false, r.status.output(1))
        assertEquals(true, r.status.input(0))
        assertEquals(false, r.status.input(1))
    }

    @Test
    fun `antenna level is captured`() {
        assertEquals("عالی", parse(device(), "ANTEN: عالی").status.antenna)
    }

    @Test
    fun `temperature is captured and out of range values ignored`() {
        assertEquals("24", parse(device(), "TEMP: 24").status.temperature)
        assertEquals(null, parse(device(), "TEMP: 999").status.temperature)
    }

    @Test
    fun `security state is captured both ways`() {
        assertEquals(true, parse(device(), "SECURITY ON").status.securityArmed)
        assertEquals(false, parse(device(), "SECURITY OFF").status.securityArmed)
    }

    @Test
    fun `unrelated sms is not treated as a device report`() {
        val r = parse(device(), "سلام، اعتبار شما ۱۰ هزار تومان است")
        assertFalse(r.recognized)
        assertEquals(now, r.status.lastContactAt)
    }

    @Test
    fun `confirmsOutput only accepts the expected direction`() {
        val d = device(outputNames = listOf("PUMP", "OUT2", "OUT3", "OUT4"))
        assertTrue(StatusParser.confirmsOutput(d, "PUMP ON", 0, expectedOn = true))
        assertFalse(StatusParser.confirmsOutput(d, "PUMP OFF", 0, expectedOn = true))
        assertFalse(StatusParser.confirmsOutput(d, "OUT2 ON", 0, expectedOn = true))
    }

    @Test
    fun `confirmsSecurity matches the requested state`() {
        assertTrue(StatusParser.confirmsSecurity("SECURITY ON", expectedArmed = true))
        assertFalse(StatusParser.confirmsSecurity("SECURITY ON", expectedArmed = false))
    }
}
