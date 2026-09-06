package ir.tivan.controller.util

/**
 * Normalizes a phone number pulled from the contacts picker into the local
 * `09xxxxxxxxx` shape the rest of the app expects (that's what gets compared
 * digit-by-digit against the number the controller texts back — see
 * [ir.tivan.controller.TivanApp.onIncomingSms]).
 *
 * Contacts commonly store Iranian numbers as `+98 912 345 6789`,
 * `0098912...`, or already as `0912...`. This strips everything but digits
 * and rewrites the `98`/`0098` country-code forms to a leading zero, leaving
 * anything else (short codes, numbers from a different country) untouched
 * rather than guessing at it.
 */
object PhoneNumber {
    fun normalizeIran(raw: String): String {
        val digits = raw.filter { it.isDigit() }
        return when {
            digits.startsWith("0098") -> "0" + digits.removePrefix("0098")
            digits.startsWith("98") && digits.length in 11..13 -> "0" + digits.removePrefix("98")
            else -> digits
        }
    }
}
