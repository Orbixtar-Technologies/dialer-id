package com.example.service

enum class CallPhase {
    IDLE,
    CONNECTING,
    RINGING,
    ACTIVE,
    ENDED
}

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
    val dtmfLog: String = "",
    val sipHost: String = "",
    val audioCodec: String = "G.711u HD",
    val latencyMs: Int = 22,
    val packetsSent: Long = 0L,
    val packetsReceived: Long = 0L
) {
    val formattedDuration: String
        get() {
            val minutes = durationSeconds / 60
            val seconds = durationSeconds % 60
            return String.format("%02d:%02d", minutes, seconds)
        }

    val isCallActive: Boolean
        get() = phase == CallPhase.CONNECTING || phase == CallPhase.RINGING || phase == CallPhase.ACTIVE
}
