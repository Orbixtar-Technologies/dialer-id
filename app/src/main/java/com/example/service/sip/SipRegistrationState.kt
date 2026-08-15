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
    val isKeepAliveActive: Boolean = false
) {
    val isRegistered: Boolean
        get() = status == RegistrationStatus.REGISTERED

    val formattedStatus: String
        get() = when (status) {
            RegistrationStatus.UNREGISTERED -> "Unregistered"
            RegistrationStatus.REGISTERING -> "Authenticating..."
            RegistrationStatus.REGISTERED -> "Registered (200 OK)"
            RegistrationStatus.FAILED -> "Registration Failed ($statusCode)"
            RegistrationStatus.EXPIRED -> "Registration Expired"
            RegistrationStatus.UNREGISTERING -> "Unregistering..."
        }
}
