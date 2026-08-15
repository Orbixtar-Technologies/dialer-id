package com.example.service.sip

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Encapsulates a detailed diagnostic snapshot of outgoing SIP SDP offers and incoming
 * SIP responses (such as 488 Not Acceptable Here, 415 Unsupported Media Type, etc.).
 *
 * Provides a formatted report for debugging carrier/SBC media negotiation errors.
 */
data class SdpDiagnosticDump(
    val timestamp: Long = System.currentTimeMillis(),
    val callId: String = "",
    val cSeq: Int = 0,
    val statusCode: Int = 488,
    val statusText: String = "Not Acceptable Here",
    val remoteHost: String = "sip.sipup.org",
    val remotePort: Int = 5060,
    val localIp: String = "127.0.0.1",
    val localPort: Int = 5060,
    val outgoingSdp: String = "",
    val incomingSipResponse: String = "",
    val warningHeader: String = "",
    val reasonHeader: String = "",
    val serverHeader: String = "",
    val attemptNumber: Int = 1
) {
    val formattedTime: String
        get() = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date(timestamp))

    val formattedReport: String
        get() = buildString {
            appendLine("================================================================================")
            appendLine("🚨🚨🚨 SIP $statusCode $statusText — SDP DIAGNOSTIC DUMP (SMOKING GUN) 🚨🚨🚨")
            appendLine("================================================================================")
            appendLine("Time: $formattedTime ($timestamp)")
            appendLine("Attempt: #$attemptNumber")
            appendLine("Call-ID: $callId")
            appendLine("CSeq: $cSeq")
            appendLine("Gateway: $remoteHost:$remotePort")
            appendLine("Local Endpoint: $localIp:$localPort")
            appendLine("Server / SBC: ${serverHeader.ifEmpty { "sipswitch / Asterisk (sip.sipup.org)" }}")
            if (warningHeader.isNotBlank()) appendLine("Warning Header: $warningHeader")
            if (reasonHeader.isNotBlank()) appendLine("Reason Header: $reasonHeader")
            appendLine()
            appendLine("----------------- OUTGOING INVITE SDP OFFER (REJECTED) -----------------")
            appendLine(outgoingSdp.trimEnd().ifEmpty { "[No SDP content captured]" })
            appendLine("------------------------------------------------------------------------")
            appendLine()
            appendLine("----------------- INCOMING SIP $statusCode RESPONSE HEADERS -----------------")
            appendLine(incomingSipResponse.trimEnd().ifEmpty { "[No response headers captured]" })
            appendLine("================================================================================")
        }
}
