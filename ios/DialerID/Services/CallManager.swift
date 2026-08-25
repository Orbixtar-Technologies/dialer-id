import Combine
import Foundation

@MainActor
final class CallManager: ObservableObject {
    static let shared = CallManager()

    @Published private(set) var callState = ActiveCallInfo()

    private let sip = SipEngine.shared
    private let callKit = CallKitController.shared
    private var timer: Timer?
    private var billingTimer: Timer?
    private var minutesDebited = 0
    private var billedUsd = 0.0
    private var connectedAt: Date?
    private var pendingDestination = ""
    private var pendingCallerId = ""
    private var pendingCountry = ""
    private var pendingRate = 0.0

    private init() {
        callKit.delegate = self
        sip.onInitializing = { [weak self] message in self?.update(phase: .initializing, message: message) }
        sip.onDialing = { [weak self] message in self?.update(phase: .dialing, message: message) }
        sip.onConnecting = { [weak self] message in self?.update(phase: .connecting, message: message) }
        sip.onRinging = { [weak self] _ in self?.update(phase: .ringing, message: "Ringing…") }
        sip.onEarlyMedia = { [weak self] message in self?.update(phase: .earlyMedia, message: message) }
        sip.onConnected = { [weak self] codec in self?.handleConnected(codec: codec) }
        sip.onError = { [weak self] _, message in self?.finish(reason: message, status: .failed) }
        sip.onEnded = { [weak self] reason in self?.finish(reason: reason, status: .completed) }
    }

    func startCall(destinationNumber: String, callerId: String, countryName: String, rate: Double) -> Bool {
        let repository = DialerRepository.shared
        guard let sanitized = PhoneNumberSanitizer.sanitizeDestination(destinationNumber) else { return false }
        let e164 = E164.format(
            sanitized,
            defaultRegion: E164.defaultRegion(),
            candidateRegions: Array(repository.rateCatalog.countryCallingCodes)
        )
        let decision = OutboundBillingPolicy.evaluate(
            destination: e164,
            balanceUsd: repository.userProfile.creditBalance,
            catalog: repository.rateCatalog
        )
        guard case .allowed = decision else { return false }
        guard let sipConfig = repository.userProfile.sipConfig?.withResolvedPassword(
            KeychainStore.shared.password(uid: repository.userProfile.uid)
        ), sipConfig.hasUsableCredentials() else {
            return false
        }

        pendingDestination = e164
        pendingCallerId = callerId
        pendingCountry = countryName
        pendingRate = rate
        minutesDebited = 0
        billedUsd = 0
        connectedAt = nil
        callState = ActiveCallInfo(
            destinationNumber: e164,
            callerIdUsed: callerId,
            countryName: countryName,
            phase: .initializing,
            billingRate: rate,
            statusMessage: "Initializing..."
        )
        callKit.startOutgoing(handle: e164)
        return true
    }

    func endCall() {
        callKit.endCall()
        sip.stopCall()
    }

    func toggleMute() {
        callState.isMuted.toggle()
        sip.setMicrophoneMuted(callState.isMuted)
    }

    func toggleSpeaker() {
        callState.isSpeakerOn.toggle()
        AudioSessionController.shared.setSpeaker(callState.isSpeakerOn)
    }

    func sendDtmf(_ digit: Character) {
        sip.sendDtmf(digit)
        callState.dtmfLog.append(digit)
    }

    private func update(phase: CallPhase, message: String) {
        callState.phase = phase
        callState.statusMessage = message
        if phase == .dialing || phase == .connecting {
            callKit.reportOutgoingStarted()
        }
    }

    private func handleConnected(codec: String) {
        callState.phase = .active
        callState.statusMessage = "Connected"
        callState.audioCodec = codec
        callKit.reportOutgoingConnected()
        connectedAt = Date()
        debitIfNeeded()
        timer?.invalidate()
        timer = Timer.scheduledTimer(withTimeInterval: 1, repeats: true) { [weak self] _ in
            Task { @MainActor in
                self?.tick()
            }
        }
        billingTimer?.invalidate()
        billingTimer = Timer.scheduledTimer(withTimeInterval: 1, repeats: true) { [weak self] _ in
            Task { @MainActor in
                self?.debitIfNeeded()
            }
        }
    }

    private func tick() {
        guard let connectedAt else { return }
        callState.durationSeconds = Int(Date().timeIntervalSince(connectedAt))
    }

    private func debitIfNeeded() {
        guard connectedAt != nil else { return }
        let expected = CallChargeCalculator.expectedMinutes(elapsedSeconds: callState.durationSeconds)
        if expected > minutesDebited {
            let increment = expected - minutesDebited
            let amount = CallChargeCalculator.roundTrip(
                Double(increment) * callState.billingRate
            )
            DialerRepository.shared.deductCredit(amount)
            minutesDebited = expected
            billedUsd += amount
        }
    }

    private func finish(reason: String, status: CallStatus) {
        timer?.invalidate()
        billingTimer?.invalidate()
        timer = nil
        billingTimer = nil
        callState.phase = .ended
        callState.endReason = reason
        AudioSessionController.shared.deactivate()
        let duration = callState.durationSeconds
        let item = CallLogItem(
            id: UUID().uuidString,
            destinationNumber: callState.destinationNumber,
            callerIdUsed: callState.callerIdUsed,
            countryName: callState.countryName,
            status: duration > 0 ? status : .cancelled,
            durationSeconds: duration,
            billingRatePerMin: callState.billingRate,
            totalCost: billedUsd
        )
        DialerRepository.shared.recordCall(item)
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.2) { [weak self] in
            self?.callState = ActiveCallInfo()
        }
    }
}

extension CallManager: CallKitControllerDelegate {
    func callKitDidStart(_ uuid: UUID) {
        let repository = DialerRepository.shared
        guard let sipConfig = repository.userProfile.sipConfig else { return }
        let codec = G711CodecType(rawValue: repository.userProfile.preferredCodec)
        sip.startOutboundCall(
            sipConfig: sipConfig,
            destination: pendingDestination,
            preferredCodec: codec
        )
    }

    func callKitDidEnd(_ uuid: UUID) {
        sip.stopCall()
    }

    func callKitDidMute(_ muted: Bool) {
        callState.isMuted = muted
        sip.setMicrophoneMuted(muted)
    }
}

private extension CallChargeCalculator {
    static func roundTrip(_ amount: Double) -> Double {
        (amount * 10_000).rounded() / 10_000
    }
}
