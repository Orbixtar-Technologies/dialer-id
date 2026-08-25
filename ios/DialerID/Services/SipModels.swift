import Foundation

enum RegistrationStatus: String {
    case unregistered
    case registering
    case authenticating
    case registered
    case failed
    case retrying
    case expired
    case unregistering
}

struct SipRegistrationState: Equatable {
    var status: RegistrationStatus = .unregistered
    var username: String = ""
    var host: String = ""
    var port: Int = 5060
    var statusCode: Int = 0
    var statusMessage: String = "Idle"
    var lastError: String?
    var needsPassword = false
    var retryAfterSeconds: Int = 0

    var isRegistered: Bool { status == .registered }

    var formattedStatus: String {
        if needsPassword { return "SIP password required" }
        switch status {
        case .unregistered: return "Unregistered"
        case .registering: return "Registering..."
        case .authenticating: return "Authenticating..."
        case .registered: return "Registered (200 OK)"
        case .retrying: return retryAfterSeconds > 0 ? "Retrying in \(retryAfterSeconds)s..." : "Retrying..."
        case .failed: return statusCode > 0 ? "Registration Failed (\(statusCode))" : "Registration Failed"
        case .expired: return "Registration Expired"
        case .unregistering: return "Unregistering..."
        }
    }
}

struct SipTestResult: Equatable {
    var isSuccess: Bool
    var statusCode: Int
    var message: String
    var latencyMs: Int64
    var serverBanner: String = ""
}

enum CallPhase: String {
    case idle, initializing, dialing, connecting, ringing, earlyMedia
    case connected, active, onHold, ending, ended
}

struct ActiveCallInfo: Equatable {
    var destinationNumber: String = ""
    var callerIdUsed: String = ""
    var countryName: String = "United States"
    var phase: CallPhase = .idle
    var durationSeconds: Int = 0
    var isMuted = false
    var isSpeakerOn = false
    var isEncrypted = false
    var billingRate: Double = 0.015
    var endReason: String = "Call Ended"
    var statusMessage: String = ""
    var dtmfLog: String = ""
    var audioCodec: String = "G.711"

    var formattedDuration: String {
        String(format: "%02d:%02d", durationSeconds / 60, durationSeconds % 60)
    }

    var isCallActive: Bool {
        phase != .idle && phase != .ended
    }

    var isTalking: Bool {
        phase == .active || phase == .connected
    }

    var displayStatus: String {
        if !statusMessage.isEmpty && phase != .active { return statusMessage }
        switch phase {
        case .idle: return "Idle"
        case .initializing: return "Initializing Line..."
        case .dialing: return "Dialing…"
        case .connecting: return "Connecting…"
        case .ringing: return "Ringing…"
        case .earlyMedia: return "Early Media"
        case .connected: return "Connected"
        case .active: return isEncrypted ? "Encrypted" : "Connected"
        case .onHold: return "On hold"
        case .ending: return "Ending…"
        case .ended: return endReason.isEmpty ? "Call ended" : endReason
        }
    }
}

enum G711CodecType: String {
    case auto = "G711_AUTO"
    case pcmu = "PCMU"
    case pcma = "PCMA"
}
