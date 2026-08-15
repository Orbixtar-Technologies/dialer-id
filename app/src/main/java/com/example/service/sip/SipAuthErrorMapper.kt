package com.example.service.sip

/**
 * Maps Linphone registration/call failures to a real SIP status and a readable reason.
 * Linphone often reports 503 / timeout as "io error" with a protocol code on ErrorInfo.
 */
object SipAuthErrorMapper {

    data class MappedError(
        val statusCode: Int,
        val message: String,
        val retryAfterSeconds: Int = 0
    )

    fun map(
        protocolCode: Int,
        linphoneMessage: String,
        reasonName: String = "",
        phrase: String = "",
        warnings: String = "",
        retryAfterSeconds: Int = 0
    ): MappedError {
        val raw = linphoneMessage.trim()
        val reason = reasonName.trim()
        val statusPhrase = phrase.trim()
        val warn = warnings.trim()
        val retryAfter = retryAfterSeconds.takeIf { it > 0 } ?: parseRetryAfter(raw, statusPhrase, warn, reason)
        val inferred = when {
            protocolCode > 0 -> protocolCode
            looksLike(raw, reason, statusPhrase, "503", "service unavailable") -> 503
            looksLike(raw, reason, statusPhrase, "401", "unauthorized", "badcredentials", "bad credentials") -> 401
            looksLike(raw, reason, statusPhrase, "403", "forbidden") -> 403
            looksLike(raw, reason, statusPhrase, "407", "proxy") -> 407
            looksLike(raw, reason, statusPhrase, "408", "timeout", "noresponse", "no response") -> 408
            looksLike(raw, reason, statusPhrase, "404", "not found") -> 404
            looksLike(raw, reason, statusPhrase, "480") -> 480
            looksLike(raw, reason, statusPhrase, "486", "busy") -> 486
            looksLike(raw, reason, statusPhrase, "488") -> 488
            looksLike(raw, reason, statusPhrase, "500", "server") -> 500
            else -> 0
        }

        val message = when (inferred) {
            401 -> "Unauthorized (401): digest authentication failed. Check username, auth username, password, and realm."
            403 -> "Forbidden (403): registrar rejected these credentials."
            404 -> "Not Found (404): SIP account does not exist on this registrar."
            407 -> "Proxy Authentication Required (407): digest challenge was not completed."
            408 -> "Request Timeout (408): no SIP response on UDP:5060."
            480 -> "Temporarily Unavailable (480)."
            486 -> "Busy Here (486)."
            488 -> "Not Acceptable Here (488)."
            500 -> "SIP server error (500)."
            503 -> format503(statusPhrase.ifBlank { raw }, retryAfter)
            else -> when {
                raw.contains("io error", ignoreCase = true) || reason.contains("IOError", ignoreCase = true) ->
                    "No final SIP response on UDP:5060 (Linphone io error). Often a timeout, 503 Service Unavailable, or the digest retry used the wrong transport."
                statusPhrase.isNotBlank() -> statusPhrase
                raw.isNotBlank() -> raw
                reason.isNotBlank() -> reason
                else -> "Registration failed"
            }
        }
        return MappedError(inferred, message, retryAfter)
    }

    fun parseRetryAfter(vararg texts: String): Int {
        val joined = texts.joinToString(" ")
        val match = Regex("""retry-after\s*[:=]?\s*(\d+)""", RegexOption.IGNORE_CASE).find(joined)
        return match?.groupValues?.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 300) ?: 0
    }

    private fun format503(detail: String, retryAfter: Int): String {
        val phrase = detail.ifBlank { "Service Unavailable" }
        val retry = if (retryAfter > 0) " Retry-After: ${retryAfter}s." else " Will retry with backoff."
        return "SIP 503 $phrase.$retry Not a generic I/O error — the registrar is overloaded, rate-limiting, or rejected this Contact/transport."
    }

    private fun looksLike(message: String, reason: String, phrase: String, vararg tokens: String): Boolean {
        val haystack = "$message $reason $phrase".lowercase()
        return tokens.any { haystack.contains(it.lowercase()) }
    }
}
