package com.example.util

/**
 * Restricts dialed destinations and caller IDs to E.164-ish digit strings.
 * Letters, SIP URIs, and other characters are rejected before a SIP URI is built.
 */
object PhoneNumberSanitizer {
    private val allowedChars = Regex("^[+]?\\d+$")

    /**
     * Returns a destination containing only an optional leading '+' and digits,
     * or null if the input is empty or contains anything else.
     */
    fun sanitizeDestination(raw: String): String? {
        val compact = raw.trim().filter { !it.isWhitespace() && it != '-' && it != '(' && it != ')' }
        if (compact.isEmpty()) return null
        if (!allowedChars.matches(compact)) return null
        if (compact.count { it == '+' } > 1) return null
        if (compact.contains('+') && !compact.startsWith('+')) return null
        val digits = compact.trimStart('+')
        if (digits.isEmpty()) return null
        return compact
    }

    /**
     * Filters live keypad / paste input down to an optional leading '+' and digits.
     */
    fun filterDialInput(raw: String): String {
        val builder = StringBuilder()
        raw.forEach { ch ->
            if (ch == '+' && builder.isEmpty()) {
                builder.append(ch)
            } else if (ch.isDigit()) {
                builder.append(ch)
            }
        }
        return builder.toString()
    }

    fun isValidCallerId(raw: String): Boolean {
        val sanitized = sanitizeDestination(raw) ?: return false
        return sanitized.trimStart('+').length >= 6
    }
}
