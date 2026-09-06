package ir.tivan.controller.util

import org.junit.Assert.assertEquals
import org.junit.Test

class PhoneNumberTest {
    @Test
    fun `already local format is untouched`() {
        assertEquals("09123456789", PhoneNumber.normalizeIran("09123456789"))
    }

    @Test
    fun `plus country code is rewritten to leading zero`() {
        assertEquals("09123456789", PhoneNumber.normalizeIran("+98 912 345 6789"))
    }

    @Test
    fun `bare country code without plus is rewritten`() {
        assertEquals("09123456789", PhoneNumber.normalizeIran("98 912 345 6789"))
    }

    @Test
    fun `00 international prefix is rewritten`() {
        assertEquals("09123456789", PhoneNumber.normalizeIran("0098-912-345-6789"))
    }

    @Test
    fun `dashes and spaces from a contact entry are stripped`() {
        assertEquals("09123456789", PhoneNumber.normalizeIran("0912 345 6789"))
    }

    @Test
    fun `short unrelated numbers are left alone`() {
        assertEquals("1818", PhoneNumber.normalizeIran("1818"))
    }
}
