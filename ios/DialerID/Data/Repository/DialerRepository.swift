import Foundation
import Combine

@MainActor
final class DialerRepository: ObservableObject {
    static let shared = DialerRepository()

    @Published private(set) var userProfile = UserProfile()
    @Published private(set) var callerIds: [CallerIdItem] = []
    @Published private(set) var callLogs: [CallLogItem] = []
    @Published private(set) var sipUpError: String?
    @Published private(set) var rateCatalog = CallRateCatalog.fromBundle()

    private let database = FirebaseDatabaseClient.shared
    private let sipUp = SipUpClient()
    private let keychain = KeychainStore.shared
    private var stopUserObserver: (() -> Void)?

    private init() {
        callLogs = LocalCallLogStore.shared.load()
    }

    func bindUser(uid: String, displayName: String, email: String, isNewUser: Bool) async {
        stopUserObserver?()
        if let existing = await database.getUser(uid: uid) {
            apply(profile: existing.withResolvedLocalPassword(uid: uid, keychain: keychain))
        } else {
            var profile = UserProfile()
            profile.uid = uid
            profile.displayName = displayName.isEmpty ? "Operator" : displayName
            profile.email = email
            profile.createdAt = Int64(Date().timeIntervalSince1970 * 1000)
            apply(profile: profile)
            database.updateUser(uid: uid, values: profile.toMap())
        }
        if isNewUser {
            await assignLineIfNeeded(uid: uid, forceNew: true)
        } else {
            await ensureExistingLine(uid: uid)
        }
        stopUserObserver = database.observeUser(uid: uid) { [weak self] profile in
            Task { @MainActor in
                self?.apply(profile: profile.withResolvedLocalPassword(uid: uid, keychain: self?.keychain ?? .shared))
            }
        }
    }

    func unbindUser() {
        stopUserObserver?()
        stopUserObserver = nil
        userProfile = UserProfile()
        callerIds = []
        sipUpError = nil
    }

    func deductCredit(_ amount: Double) {
        let newBalance = max(0, userProfile.creditBalance - amount)
        writeCreditBalance(newBalance)
    }

    func applyTopUp(amountUsd: Double) {
        writeCreditBalance(WalletCredit.applyIntendedTopUp(
            currentBalanceUsd: userProfile.creditBalance,
            intendedTopUpUsd: amountUsd
        ))
    }

    func setSelectedCallerId(_ number: String) {
        userProfile.selectedCallerId = number
        sync(["selectedCallerId": number])
    }

    func setPreferredCodec(_ codec: String) {
        userProfile.preferredCodec = codec
        sync(["preferredCodec": codec])
    }

    func updateProfile(displayName: String, organization: String, role: String, phone: String) {
        userProfile.displayName = displayName
        userProfile.organization = organization
        userProfile.accountRole = role
        userProfile.phoneNumber = phone
        sync([
            "displayName": displayName,
            "organization": organization,
            "accountRole": role,
            "phoneNumber": phone,
            "profile": [
                "display_name": displayName,
                "displayName": displayName,
                "email": userProfile.email,
                "createdAt": userProfile.createdAt
            ]
        ])
    }

    func saveSipConfig(_ config: SipConfig) {
        userProfile.sipConfig = config
        if !config.password.isEmpty {
            keychain.savePassword(uid: userProfile.uid, password: config.password)
        }
        sync(["sip": config.toRemoteMap()])
    }

    func addCallerId(number: String, label: String, makePrimary: Bool) {
        let item = CallerIdItem(
            id: UUID().uuidString,
            phoneNumber: number,
            label: label,
            isPrimary: makePrimary || callerIds.isEmpty
        )
        if item.isPrimary {
            callerIds = callerIds.map {
                var copy = $0
                copy.isPrimary = false
                return copy
            }
            userProfile.selectedCallerId = number
        }
        callerIds.append(item)
        persistCallerIds()
    }

    func deleteCallerId(_ id: String) {
        callerIds.removeAll { $0.id == id }
        persistCallerIds()
    }

    func setPrimaryCallerId(_ id: String) {
        callerIds = callerIds.map { item in
            var copy = item
            copy.isPrimary = item.id == id
            if copy.isPrimary {
                userProfile.selectedCallerId = copy.phoneNumber
            }
            return copy
        }
        persistCallerIds()
    }

    func recordCall(_ item: CallLogItem) {
        callLogs.insert(item, at: 0)
        LocalCallLogStore.shared.save(callLogs)
        userProfile.callsCount += 1
        userProfile.totalMinutes += CallChargeCalculator.billedMinutes(durationSeconds: item.durationSeconds)
        sync([
            "callsCount": userProfile.callsCount,
            "totalMinutes": userProfile.totalMinutes
        ])
    }

    func clearCallLogs() {
        callLogs = []
        LocalCallLogStore.shared.save([])
    }

    func isDeviceRegistered() async -> Bool {
        await database.isDeviceRegistered(uid: userProfile.uid, deviceId: DeviceIdentity.stableDeviceId())
    }

    func registerCurrentDevice(paymentId: String, feeUsd: Double) async {
        await database.registerDevice(
            uid: userProfile.uid,
            deviceId: DeviceIdentity.stableDeviceId(),
            label: DeviceIdentity.deviceLabel(),
            paymentId: paymentId,
            feeUsd: feeUsd,
            exempt: false
        )
    }

    func applyConfirmedPayment(paymentId: String, kind: CheckoutKind, amountUsd: Double) async {
        if await database.wasPaymentProcessed(uid: userProfile.uid, paymentId: paymentId) {
            return
        }
        switch kind {
        case .topUp:
            applyTopUp(amountUsd: amountUsd)
        case .deviceRegistration:
            await registerCurrentDevice(paymentId: paymentId, feeUsd: amountUsd)
        }
        database.markPaymentProcessed(
            uid: userProfile.uid,
            paymentId: paymentId,
            kind: kind,
            amountUsd: amountUsd
        )
    }

    private func assignLineIfNeeded(uid: String, forceNew: Bool) async {
        do {
            if !forceNew, let existing = await database.findAssignment(uid: uid) {
                applyAssignment(uid: uid, record: existing)
                return
            }
            let record = try await database.assignLine(uid: uid, preferFirst: false)
            applyAssignment(uid: uid, record: record)
        } catch is SignupCapacityException {
            sipUpError = SipIdAssignment.signupFullMessage
        } catch {
            sipUpError = SipIdAssignment.assignmentFailedMessage
        }
    }

    private func ensureExistingLine(uid: String) async {
        let profileRecord = SipIdAssignment.recordFromUserFields(
            assignedSipId: userProfile.assignedSipId,
            username: userProfile.sipConfig?.username ?? "",
            deviceId: userProfile.sipConfig?.deviceId ?? "",
            uid: uid
        )
        if let profileRecord {
            applyAssignment(uid: uid, record: profileRecord)
        }

        if let ledger = await database.findAssignment(uid: uid) {
            applyAssignment(uid: uid, record: ledger)
            return
        }

        if let profileRecord {
            if let claimed = try? await database.claimSpecific(uid: uid, identity: profileRecord.toLineIdentity()) {
                applyAssignment(uid: uid, record: claimed)
            }
            return
        }

        await assignLineIfNeeded(uid: uid, forceNew: false)
    }

    private func applyAssignment(uid: String, record: SipIdAssignmentRecord) {
        let sip = SipIdAssignment.applyToSipConfig(userProfile.sipConfig, record: record)
        if !sip.password.isEmpty {
            keychain.savePassword(uid: uid, password: sip.password)
        }
        userProfile.uid = uid
        userProfile.assignedSipId = record.sipId
        userProfile.sipConfig = sip
        sipUpError = nil
        var remote: [String: Any] = [
            "assignedSipId": record.sipId,
            "deviceId": record.deviceId
        ]
        if !sip.host.isEmpty || !sip.password.isEmpty {
            remote["sip"] = sip.toRemoteMap()
        }
        sync(remote)
        Task {
            if let snapshot = try? await sipUp.fetchSnapshot(
                sipUsername: sip.username,
                sipDeviceId: sip.deviceId
            ), let device = snapshot.device, !device.callerIdNumber.isEmpty {
                if userProfile.selectedCallerId.isEmpty {
                    setSelectedCallerId(device.callerIdNumber)
                }
                if !callerIds.contains(where: { $0.phoneNumber == device.callerIdNumber }) {
                    addCallerId(number: device.callerIdNumber, label: device.callerIdName, makePrimary: callerIds.isEmpty)
                }
            }
        }
    }

    private func apply(profile: UserProfile) {
        userProfile = profile
        if callerIds.isEmpty, !profile.selectedCallerId.isEmpty {
            callerIds = [
                CallerIdItem(
                    id: "primary",
                    phoneNumber: profile.selectedCallerId,
                    label: "",
                    isPrimary: true
                )
            ]
        }
    }

    private func writeCreditBalance(_ newBalance: Double) {
        userProfile.creditBalance = newBalance
        sync([
            "creditBalance": newBalance,
            "balanceCache": [
                "balance": newBalance,
                "currency": userProfile.currency,
                "updatedAt": Int64(Date().timeIntervalSince1970)
            ]
        ])
    }

    private func persistCallerIds() {
        if let primary = callerIds.first(where: \.isPrimary) ?? callerIds.first {
            userProfile.selectedCallerId = primary.phoneNumber
            sync(["selectedCallerId": primary.phoneNumber])
        }
    }

    private func sync(_ values: [String: Any]) {
        guard !userProfile.isGuest else { return }
        database.updateUser(uid: userProfile.uid, values: values)
    }
}

private extension UserProfile {
    func withResolvedLocalPassword(uid: String, keychain: KeychainStore) -> UserProfile {
        guard var sip = sipConfig else { return self }
        sip = sip.withResolvedPassword(keychain.password(uid: uid))
        var copy = self
        copy.sipConfig = sip
        return copy
    }
}
