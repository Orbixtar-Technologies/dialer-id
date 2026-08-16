package com.example.service

/**
 * Rich lifecycle phases for outbound/inbound SIP VoIP calls.
 */
enum class CallPhase {
    IDLE,
    INITIALIZING,
    DIALING,
    CONNECTING,
    RINGING,
    EARLY_MEDIA,
    CONNECTED,
    ACTIVE,
    ON_HOLD,
    ENDING,
    ENDED
}

/**
 * Real-time active call telemetry and status model.
 */
data class ActiveCallInfo(
    val destinationNumber: String = "",
    val callerIdUsed: String = "",
    val countryName: String = "United States",
    val phase: CallPhase = CallPhase.IDLE,
    val durationSeconds: Int = 0,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = false,
    val isEncrypted: Boolean = false,
    val billingRate: Double = 0.015,
    val endReason: String = "Call Ended",
    val statusMessage: String = "",
    val dtmfLog: String = "",
    val sipHost: String = "",
    val audioCodec: String = "G.711u HD",
    val latencyMs: Int = 22,
    val packetsSent: Long = 0L,
    val packetsReceived: Long = 0L,
    val sipResponseCode: Int = 0,
    val isOnHold: Boolean = false
) {
    val formattedDuration: String
        get() {
            val minutes = durationSeconds / 60
            val seconds = durationSeconds % 60
            return String.format("%02d:%02d", minutes, seconds)
        }

    val isCallActive: Boolean
        get() = phase != CallPhase.IDLE && phase != CallPhase.ENDED

    val isTalking: Boolean
        get() = phase == CallPhase.ACTIVE || phase == CallPhase.CONNECTED

    val displayStatus: String
        get() {
            if (statusMessage.isNotBlank() && phase != CallPhase.ACTIVE) {
                return statusMessage
            }
            return when (phase) {
                CallPhase.IDLE -> "Idle"
                CallPhase.INITIALIZING -> "Initializing Line..."
                CallPhase.DIALING -> "Dialing Destination..."
                CallPhase.CONNECTING -> "Connecting (100 Trying)..."
                CallPhase.RINGING -> "Ringing Destination (180)..."
                CallPhase.EARLY_MEDIA -> "Session Progress (183)..."
                CallPhase.CONNECTED -> "Establishing Audio Streams..."
                CallPhase.ACTIVE -> if (isEncrypted) "Secured Line Connected (SRTP)" else "HD Line Connected"
                CallPhase.ON_HOLD -> "Call On Hold"
                CallPhase.ENDING -> "Terminating Call..."
                CallPhase.ENDED -> if (endReason.isNotBlank()) endReason else "Call Ended"
            }
        }
}
