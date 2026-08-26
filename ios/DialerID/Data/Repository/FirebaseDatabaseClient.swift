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
        let handle = ref.observe(.value, with: { snapshot in
            let map = snapshot.value as? [String: Any] ?? [:]
            onChange(UserProfile.fromMap(map, uid: uid))
        }, withCancel: { _ in })
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
        do {
            let snapshot = try await database.reference(withPath: "\(FirebasePaths.users)/\(uid)").getData()
            guard snapshot.exists(), let map = snapshot.value as? [String: Any] else {
                return nil
            }
            return UserProfile.fromMap(map, uid: uid)
        } catch {
            return nil
        }
        #else
        return nil
        #endif
    }

    func isDeviceRegistered(uid: String, deviceId: String) async -> Bool {
        #if canImport(FirebaseDatabase)
        do {
            let key = Self.safeKey(deviceId)
            let snapshot = try await database
                .reference(withPath: "\(FirebasePaths.users)/\(uid)/\(FirebasePaths.devices)/\(key)")
                .getData()
            return snapshot.exists()
        } catch {
            return false
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
        do {
            let key = Self.safeKey(paymentId)
            let snapshot = try await database
                .reference(withPath: "\(FirebasePaths.users)/\(uid)/\(FirebasePaths.processedPayments)/\(key)")
                .getData()
            return snapshot.exists()
        } catch {
            return false
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
        await snapshotValue(at: SipIdAssignment.poolPath) { snapshot, error in
            guard error == nil else { return [] }
            return SipIdAssignment.parseIdentities(snapshot?.value)
        }
    }

    private func readAssignments() async -> [String: SipIdAssignmentRecord] {
        await snapshotValue(at: SipIdAssignment.assignmentsPath) { snapshot, error in
            SipIdAssignment.parseAssignments(snapshot?.value, error: error)
        }
    }

    private func snapshotValue<T>(
        at path: String,
        map: @escaping (DataSnapshot?, Error?) -> T
    ) async -> T {
        await withCheckedContinuation { continuation in
            let once = OnceResume(continuation)
            let ref = database.reference(withPath: path)
            ref.observeSingleEvent(of: .value, with: { snapshot in
                once.resume(returning: map(snapshot, nil))
            }, withCancel: { error in
                once.resume(returning: map(nil, error))
            })
        }
    }

    private func runAssignment(
        uid: String,
        pool: [SipLineIdentity],
        claimSpecific: SipLineIdentity?,
        preferFirst: Bool
    ) async throws -> SipIdAssignmentRecord {
        try await withCheckedThrowingContinuation { continuation in
            let once = OnceThrowingResume(continuation)
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
                    once.resume(throwing: error)
                    return
                }
                let assignments = SipIdAssignment.parseAssignments(snapshot?.value)
                if let record = SipIdAssignment.findByUid(assignments, uid: uid) {
                    once.resume(returning: record)
                } else {
                    once.resume(throwing: SignupCapacityException())
                }
            }
        }
    }
    #endif

    private final class OnceResume<T>: @unchecked Sendable {
        private let lock = NSLock()
        private var continuation: CheckedContinuation<T, Never>?

        init(_ continuation: CheckedContinuation<T, Never>) {
            self.continuation = continuation
        }

        func resume(returning value: T) {
            lock.lock()
            let pending = continuation
            continuation = nil
            lock.unlock()
            pending?.resume(returning: value)
        }
    }

    private final class OnceThrowingResume<T>: @unchecked Sendable {
        private let lock = NSLock()
        private var continuation: CheckedContinuation<T, Error>?

        init(_ continuation: CheckedContinuation<T, Error>) {
            self.continuation = continuation
        }

        func resume(returning value: T) {
            lock.lock()
            let pending = continuation
            continuation = nil
            lock.unlock()
            pending?.resume(returning: value)
        }

        func resume(throwing error: Error) {
            lock.lock()
            let pending = continuation
            continuation = nil
            lock.unlock()
            pending?.resume(throwing: error)
        }
    }

    static func safeKey(_ raw: String) -> String {
        raw.trimmingCharacters(in: .whitespacesAndNewlines)
            .replacingOccurrences(of: "[.#$\\[\\]/]", with: "_", options: .regularExpression)
    }
}
