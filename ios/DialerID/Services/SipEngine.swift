import Combine
import Foundation

#if canImport(linphonesw)
import linphonesw
#endif

@MainActor
final class SipEngine: ObservableObject {
    static let shared = SipEngine()

    @Published private(set) var registrationState = SipRegistrationState()
    @Published private(set) var isRegistered = false
    @Published private(set) var hasActiveCall = false

    var onInitializing: ((String) -> Void)?
    var onDialing: ((String) -> Void)?
    var onConnecting: ((String) -> Void)?
    var onRinging: ((String?) -> Void)?
    var onEarlyMedia: ((String) -> Void)?
    var onConnected: ((String) -> Void)?
    var onError: ((Int, String) -> Void)?
    var onEnded: ((String) -> Void)?

    private var currentConfig: SipConfig?
    #if canImport(linphonesw)
    private var core: Core?
    private var currentCall: Call?
    private var iterateTimer: Timer?
    private var coreListener: CoreDelegateStub?
    private var triedAlternateTransport = false
    #endif

    func register(sipConfig: SipConfig, force: Bool = false) {
        let sipConfig = sipConfig.resolvedForRegistration()
        if sipConfig.needsPassword() {
            currentConfig = sipConfig
            registrationState = SipRegistrationState(
                status: .failed,
                username: sipConfig.username,
                host: sipConfig.host,
                port: sipConfig.port,
                statusMessage: "SIP password required",
                needsPassword: true
            )
            isRegistered = false
            return
        }
        if !sipConfig.hasUsableCredentials() {
            registrationState = SipRegistrationState(
                status: .unregistered,
                statusMessage: "SIP credentials not configured"
            )
            isRegistered = false
            return
        }
        currentConfig = sipConfig
        registrationState = SipRegistrationState(
            status: .registering,
            username: sipConfig.username,
            host: sipConfig.host,
            port: sipConfig.port,
            statusMessage: "Registering..."
        )
        #if canImport(linphonesw)
        triedAlternateTransport = false
        ensureCore()
        applyAccount(sipConfig, transport: .Udp)
        #else
        registrationState.status = .failed
        registrationState.lastError = "Linphone SDK is not linked. Run pod install on a Mac."
        registrationState.statusMessage = "Linphone SDK missing"
        #endif
    }

    func refreshNow(force: Bool = false) {
        guard let config = currentConfig else { return }
        register(sipConfig: config, force: true)
    }

    func unregister() {
        currentConfig = nil
        isRegistered = false
        hasActiveCall = false
        registrationState = SipRegistrationState(status: .unregistered, statusMessage: "Unregistered")
        #if canImport(linphonesw)
        try? core?.clearAccounts()
        try? core?.clearAllAuthInfo()
        #endif
    }

    func startOutboundCall(sipConfig: SipConfig, destination: String, preferredCodec: G711CodecType?) {
        let sipConfig = sipConfig.resolvedForRegistration()
        #if canImport(linphonesw)
        if currentCall != nil { return }
        #endif
        guard let sanitized = PhoneNumberSanitizer.sanitizeDestination(destination) else {
            onError?(400, "Invalid destination number")
            return
        }
        if sipConfig.needsPassword() {
            onError?(0, "SIP password required")
            return
        }
        if !sipConfig.hasUsableCredentials() {
            onError?(0, "SIP credentials not configured")
            return
        }
        register(sipConfig: sipConfig)
        onInitializing?("Initializing...")
        #if canImport(linphonesw)
        ensureCore()
        guard let core else {
            onError?(500, "SIP engine failed to start")
            return
        }
        let address = try? Factory.Instance.createAddress(addr: "sip:\(sanitized)@\(sipConfig.host)")
        guard let address else {
            onError?(400, "Invalid destination number")
            return
        }
        do {
            let params = try core.createCallParams(call: nil)
            params.mediaEncryption = .SRTP
            params.avpfMode = .Disabled
            currentCall = try core.inviteAddressWithParams(addr: address, params: params)
            hasActiveCall = currentCall != nil
            onDialing?("Dialing...")
        } catch {
            onError?(500, error.localizedDescription)
        }
        #else
        onError?(500, "Linphone SDK is not linked. Run pod install on a Mac.")
        #endif
    }

    func stopCall(reason: String = "Call Ended") {
        #if canImport(linphonesw)
        try? currentCall?.terminate()
        currentCall = nil
        #endif
        hasActiveCall = false
        onEnded?(reason)
    }

    func sendDtmf(_ digit: Character) {
        #if canImport(linphonesw)
        try? currentCall?.sendDtmf(dtmf: digit)
        #endif
    }

    func setMicrophoneMuted(_ muted: Bool) {
        #if canImport(linphonesw)
        core?.micEnabled = !muted
        #endif
    }

    func testSipConnection(_ sipConfig: SipConfig) async -> SipTestResult {
        let start = Date()
        let sipConfig = sipConfig.resolvedForRegistration()
        if sipConfig.needsPassword() {
            return SipTestResult(isSuccess: false, statusCode: 0, message: "SIP password required", latencyMs: 0)
        }
        if !sipConfig.hasUsableCredentials() {
            return SipTestResult(isSuccess: false, statusCode: 0, message: "Host, username, and password are required", latencyMs: 0)
        }
        register(sipConfig: sipConfig, force: true)
        let deadline = Date().addingTimeInterval(20)
        while Date() < deadline {
            if registrationState.status == .registered {
                let ms = Int64(Date().timeIntervalSince(start) * 1000)
                return SipTestResult(isSuccess: true, statusCode: 200, message: "REGISTER succeeded", latencyMs: ms, serverBanner: "Linphone")
            }
            if registrationState.status == .failed {
                let ms = Int64(Date().timeIntervalSince(start) * 1000)
                return SipTestResult(
                    isSuccess: false,
                    statusCode: registrationState.statusCode,
                    message: registrationState.lastError ?? registrationState.statusMessage,
                    latencyMs: ms
                )
            }
            try? await Task.sleep(nanoseconds: 200_000_000)
        }
        return SipTestResult(isSuccess: false, statusCode: 408, message: "REGISTER timed out", latencyMs: 20_000)
    }

    #if canImport(linphonesw)
    private func ensureCore() {
        if core != nil { return }
        do {
            let created = try Factory.Instance.createCore(configPath: nil, factoryConfigPath: nil, systemContext: nil)
            created.mediaEncryptionMandatory = false
            let listener = CoreDelegateStub(
                onAccountRegistrationStateChanged: { [weak self] _, _, state, message in
                    Task { @MainActor in
                        self?.handleRegistration(state: state, message: message)
                    }
                },
                onCallStateChanged: { [weak self] _, call, state, message in
                    Task { @MainActor in
                        self?.handleCall(call: call, state: state, message: message)
                    }
                }
            )
            created.addDelegate(delegate: listener)
            try created.start()
            core = created
            coreListener = listener
            iterateTimer = Timer.scheduledTimer(withTimeInterval: 0.02, repeats: true) { [weak created] _ in
                created?.iterate()
            }
        } catch {
            registrationState.status = .failed
            registrationState.lastError = error.localizedDescription
        }
    }

    private func applyAccount(_ sipConfig: SipConfig, transport: TransportType) {
        guard let core else { return }
        try? core.clearAccounts()
        try? core.clearAllAuthInfo()
        let identity = try? Factory.Instance.createAddress(addr: "sip:\(sipConfig.username)@\(sipConfig.host)")
        let server = try? Factory.Instance.createAddress(addr: "sip:\(sipConfig.host):\(sipConfig.port)")
        server?.transport = transport
        guard let identity, let server else { return }
        let auth = try? Factory.Instance.createAuthInfo(
            username: sipConfig.username,
            userid: sipConfig.username,
            passwd: sipConfig.password,
            ha1: nil,
            realm: nil,
            domain: sipConfig.host
        )
        if let auth {
            core.addAuthInfo(info: auth)
        }
        if let params = try? core.createAccountParams() {
            params.identityAddress = identity
            params.serverAddress = server
            params.registerEnabled = true
            params.expires = 300
            if let account = try? core.createAccount(params: params) {
                try? core.addAccount(account: account)
                core.defaultAccount = account
            }
        }
        core.stunServer = "stun.linphone.org"
    }

    private func handleRegistration(state: RegistrationState, message: String) {
        switch state {
        case .Progress:
            registrationState.status = .registering
            registrationState.statusMessage = message
            isRegistered = false
        case .Ok:
            registrationState.status = .registered
            registrationState.statusCode = 200
            registrationState.statusMessage = message
            isRegistered = true
        case .Failed:
            if !triedAlternateTransport, let config = currentConfig {
                triedAlternateTransport = true
                registrationState.status = .retrying
                registrationState.statusMessage = "Retrying over TCP..."
                applyAccount(config, transport: .Tcp)
                return
            }
            registrationState.status = .failed
            registrationState.statusMessage = message
            registrationState.lastError = message
            isRegistered = false
        case .Cleared:
            registrationState.status = .unregistered
            isRegistered = false
        default:
            break
        }
    }

    private func handleCall(call: Call, state: Call.State, message: String) {
        currentCall = call
        hasActiveCall = true
        switch state {
        case .OutgoingInit:
            onInitializing?(message)
        case .OutgoingProgress:
            onDialing?(message)
        case .OutgoingRinging:
            onRinging?(nil)
        case .OutgoingEarlyMedia:
            onEarlyMedia?(message)
        case .Connected, .StreamsRunning:
            onConnected?(call.currentParams?.usedAudioPayloadType?.mimeType ?? "G.711")
        case .Error:
            hasActiveCall = false
            currentCall = nil
            onError?(Int(call.reason.rawValue), message)
        case .End, .Released:
            hasActiveCall = false
            currentCall = nil
            onEnded?(message)
        default:
            break
        }
    }
    #endif
}
