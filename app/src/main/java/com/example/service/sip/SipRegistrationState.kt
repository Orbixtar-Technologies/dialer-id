package com.example.service.sip

/**
 * High-level SIP registration lifecycle state.
 */
enum class RegistrationStatus {
    UNREGISTERED,
    REGISTERING,
    REGISTERED,
    FAILED,
    EXPIRED,
    UNREGISTERING
}

/**
 * Immutable data state representing current SIP Trunk registration status,
 * granted expiry, server response, keep-alive telemetry, and scheduled refresh timing.
 */
data class SipRegistrationState(
    val status: RegistrationStatus = RegistrationStatus.UNREGISTERED,
    val username: String = "",
    val host: String = "",
    val port: Int = 5060,
    val localIp: String = "",
    val localPort: Int = 0,
    val expiresSeconds: Int = 300,
    val registeredAt: Long = 0L,
    val expiresAt: Long = 0L,
    val nextRefreshAt: Long = 0L,
    val secondsRemaining: Int = 0,
    val serverBanner: String = "",
    val statusCode: Int = 0,
    val statusMessage: String = "Idle",
    val lastError: String? = null,
    val lastKeepAliveAt: Long = 0L,
    val keepAlivePingsSent: Long = 0L,
    val roundTripLatencyMs: Long = 0L,
    val retryCount: Int = 0,
    val retryAfterSeconds: Int = 0,
    val isKeepAliveActive: Boolean = false,
    val needsPassword: Boolean = false
) {
    val isRegistered: Boolean
        get() = status == RegistrationStatus.REGISTERED

    val formattedStatus: String
        get() = when {
            needsPassword -> "SIP password required"
            status == RegistrationStatus.UNREGISTERED -> "Unregistered"
            status == RegistrationStatus.REGISTERING -> "Authenticating..."
            status == RegistrationStatus.REGISTERED -> "Registered (200 OK)"
            status == RegistrationStatus.FAILED && statusCode > 0 -> "Registration Failed ($statusCode)"
            status == RegistrationStatus.FAILED -> "Registration Failed"
            status == RegistrationStatus.EXPIRED -> "Registration Expired"
            status == RegistrationStatus.UNREGISTERING -> "Unregistering..."
            else -> "Unregistered"
        }
}
