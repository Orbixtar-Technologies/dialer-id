import Foundation

#if canImport(FirebaseDatabase)
import FirebaseDatabase
#endif

enum FirebasePaths {
    static let users = "users"
    static let devices = "registeredDevices"
    static let deviceIndex = "deviceRegistrations"
    static let processedPayments = "processedPayments"
}

final class FirebaseDatabaseClient: @unchecked Sendable {
    static let shared = FirebaseDatabaseClient()

    #if canImport(FirebaseDatabase)
    private var database: Database {
        Database.database(url: AppConfig.firebaseDatabaseURL)
    }
    #endif

    func observeUser(uid: String, onChange: @escaping (UserProfile) -> Void) -> () -> Void {
        #if canImport(FirebaseDatabase)
        let ref = database.reference(withPath: "\(FirebasePaths.users)/\(uid)")
        let handle = ref.observe(.value) { snapshot in
            let map = snapshot.value as? [String: Any] ?? [:]
            onChange(UserProfile.fromMap(map, uid: uid))
        }
        return { ref.removeObserver(withHandle: handle) }
        #else
        return {}
        #endif
    }

    func updateUser(uid: String, values: [String: Any]) {
        #if canImport(FirebaseDatabase)
        database.reference(withPath: "\(FirebasePaths.users)/\(uid)").updateChildValues(values) { _, _ in }
        #endif
    }

    func getUser(uid: String) async -> UserProfile? {
        #if canImport(FirebaseDatabase)
        await withCheckedContinuation { continuation in
            database.reference(withPath: "\(FirebasePaths.users)/\(uid)").observeSingleEvent(of: .value) { snapshot in
                guard snapshot.exists(), let map = snapshot.value as? [String: Any] else {
                    continuation.resume(returning: nil)
                    return
                }
                continuation.resume(returning: UserProfile.fromMap(map, uid: uid))
            }
        }
        #else
        return nil
        #endif
    }

    func isDeviceRegistered(uid: String, deviceId: String) async -> Bool {
        #if canImport(FirebaseDatabase)
        await withCheckedContinuation { continuation in
            let key = Self.safeKey(deviceId)
            database.reference(withPath: "\(FirebasePaths.users)/\(uid)/\(FirebasePaths.devices)/\(key)")
                .observeSingleEvent(of: .value) { snapshot in
                    continuation.resume(returning: snapshot.exists())
                }
        }
        #else
        return false
        #endif
    }

    func registerDevice(
        uid: String,
        deviceId: String,
        label: String,
        paymentId: String,
        feeUsd: Double,
        exempt: Bool
    ) async {
        #if canImport(FirebaseDatabase)
        let now = Int64(Date().timeIntervalSince1970 * 1000)
        let record: [String: Any] = [
            "registeredAt": now,
            "label": label,
            "paymentId": paymentId,
            "feeUsd": feeUsd,
            "exempt": exempt
        ]
        let key = Self.safeKey(deviceId)
        try? await database.reference(withPath: "\(FirebasePaths.users)/\(uid)/\(FirebasePaths.devices)/\(key)")
            .updateChildValues(record)
        try? await database.reference(withPath: "\(FirebasePaths.deviceIndex)/\(key)").updateChildValues([
            "uid": uid,
            "registeredAt": now,
            "exempt": exempt
        ])
        #endif
    }

    func wasPaymentProcessed(uid: String, paymentId: String) async -> Bool {
        #if canImport(FirebaseDatabase)
        await withCheckedContinuation { continuation in
            let key = Self.safeKey(paymentId)
            database.reference(withPath: "\(FirebasePaths.users)/\(uid)/\(FirebasePaths.processedPayments)/\(key)")
                .observeSingleEvent(of: .value) { snapshot in
                    continuation.resume(returning: snapshot.exists())
                }
        }
        #else
        return false
        #endif
    }

    func markPaymentProcessed(uid: String, paymentId: String, kind: CheckoutKind, amountUsd: Double) {
        #if canImport(FirebaseDatabase)
        let key = Self.safeKey(paymentId)
        database.reference(withPath: "\(FirebasePaths.users)/\(uid)/\(FirebasePaths.processedPayments)/\(key)")
            .updateChildValues([
                "kind": kind.rawValue,
                "amountUsd": amountUsd,
                "appliedAt": Int64(Date().timeIntervalSince1970 * 1000)
            ]) { _, _ in }
        #endif
    }

    func assignLine(uid: String, preferFirst: Bool) async throws -> SipIdAssignmentRecord {
        #if canImport(FirebaseDatabase)
        let pool = await loadPool()
        if pool.isEmpty {
            throw SignupCapacityException()
        }
        return try await runAssignment(uid: uid, pool: pool, claimSpecific: nil, preferFirst: preferFirst)
        #else
        throw AuthServiceError.firebaseUnavailable
        #endif
    }

    func findAssignment(uid: String) async -> SipIdAssignmentRecord? {
        #if canImport(FirebaseDatabase)
        let assignments = await readAssignments()
        return SipIdAssignment.findByUid(assignments, uid: uid)
        #else
        return nil
        #endif
    }

    func claimSpecific(uid: String, identity: SipLineIdentity) async throws -> SipIdAssignmentRecord {
        #if canImport(FirebaseDatabase)
        return try await runAssignment(uid: uid, pool: [identity], claimSpecific: identity, preferFirst: false)
        #else
        throw AuthServiceError.firebaseUnavailable
        #endif
    }

    #if canImport(FirebaseDatabase)
    private func loadPool() async -> [SipLineIdentity] {
        await withCheckedContinuation { continuation in
            database.reference(withPath: SipIdAssignment.poolPath).observeSingleEvent(of: .value) { snapshot in
                let parsed = SipIdAssignment.parseIdentities(snapshot.value)
                continuation.resume(returning: parsed)
            }
        }
    }

    private func readAssignments() async -> [String: SipIdAssignmentRecord] {
        await withCheckedContinuation { continuation in
            database.reference(withPath: SipIdAssignment.assignmentsPath).observeSingleEvent(of: .value) { snapshot in
                continuation.resume(returning: SipIdAssignment.parseAssignments(snapshot.value))
            }
        }
    }

    private func runAssignment(
        uid: String,
        pool: [SipLineIdentity],
        claimSpecific: SipLineIdentity?,
        preferFirst: Bool
    ) async throws -> SipIdAssignmentRecord {
        try await withCheckedThrowingContinuation { continuation in
            database.reference(withPath: SipIdAssignment.assignmentsPath).runTransactionBlock { currentData in
                let assignments = SipIdAssignment.parseAssignments(currentData.value)
                let now = Int64(Date().timeIntervalSince1970 * 1000)
                let decision: SipIdAssignmentResult
                if let claimSpecific {
                    decision = SipIdAssignment.claimSpecific(
                        identity: claimSpecific,
                        assignments: assignments,
                        uid: uid,
                        nowMs: now
                    )
                } else if preferFirst {
                    decision = SipIdAssignment.decidePreferFirst(
                        pool: pool,
                        assignments: assignments,
                        uid: uid,
                        nowMs: now
                    )
                } else {
                    decision = SipIdAssignment.decide(
                        pool: pool,
                        assignments: assignments,
                        uid: uid,
                        nowMs: now
                    )
                }
                switch decision {
                case .alreadyAssigned:
                    return TransactionResult.success(withValue: currentData)
                case .assigned(let record):
                    var map = (currentData.value as? [String: Any]) ?? [:]
                    map[record.sipId] = record.toRemoteMap()
                    currentData.value = map
                    return TransactionResult.success(withValue: currentData)
                case .poolFull, .noUnusedId:
                    return TransactionResult.abort()
                }
            } andCompletionBlock: { error, committed, snapshot in
                if let error {
                    continuation.resume(throwing: error)
                    return
                }
                let assignments = SipIdAssignment.parseAssignments(snapshot?.value)
                if let record = SipIdAssignment.findByUid(assignments, uid: uid) {
                    continuation.resume(returning: record)
                } else {
                    continuation.resume(throwing: SignupCapacityException())
                }
            }
        }
    }
    #endif

    static func safeKey(_ raw: String) -> String {
        raw.trimmingCharacters(in: .whitespacesAndNewlines)
            .replacingOccurrences(of: "[.#$\\[\\]/]", with: "_", options: .regularExpression)
    }
}
