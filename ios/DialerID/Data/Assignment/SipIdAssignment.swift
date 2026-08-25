import Foundation

struct SipLineIdentity: Equatable {
    var sipId: String
    var username: String
    var deviceId: String
}

struct SipIdAssignmentRecord: Equatable {
    var sipId: String
    var firebaseUid: String
    var assignedAt: Int64
    var username: String = ""
    var deviceId: String = ""

    func toRemoteMap() -> [String: Any] {
        [
            "firebaseUid": firebaseUid,
            "assignedAt": assignedAt,
            "username": username,
            "deviceId": deviceId
        ]
    }

    func toPoolMap() -> [String: Any] {
        [
            "username": username,
            "deviceId": deviceId
        ]
    }

    func toLineIdentity() -> SipLineIdentity {
        SipLineIdentity(
            sipId: sipId,
            username: username.isEmpty ? sipId : username,
            deviceId: deviceId
        )
    }
}

enum SipIdAssignmentResult: Equatable {
    case assigned(SipIdAssignmentRecord)
    case alreadyAssigned(SipIdAssignmentRecord)
    case poolFull
    case noUnusedId
}

struct SignupCapacityException: Error, LocalizedError {
    var errorDescription: String? { SipIdAssignment.signupFullMessage }
}

enum SipIdAssignment {
    static let maxAssignedIds = 4
    static let assignmentsPath = "idAssignments"
    static let poolPath = "sipIdPool"
    static let signupFullMessage = "Sign-up is full. No lines are available."
    static let noLineMessage = "No line is available for this account."
    static let assignmentFailedMessage = "Couldn't finish creating your account. Try again."

    static func sipIdOf(username: String, deviceId: String) -> String {
        let trimmed = username.trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.isEmpty ? deviceId.trimmingCharacters(in: .whitespacesAndNewlines) : trimmed
    }

    static func identitiesFromDevices(_ devices: [SipUpDevice]) -> [SipLineIdentity] {
        var seen = Set<String>()
        var result: [SipLineIdentity] = []
        for device in devices {
            let username = device.username.trimmingCharacters(in: .whitespacesAndNewlines)
            let deviceId = device.id > 0 ? String(device.id) : ""
            let sipId = sipIdOf(username: username, deviceId: deviceId)
            if sipId.isEmpty || seen.contains(sipId) { continue }
            seen.insert(sipId)
            result.append(
                SipLineIdentity(
                    sipId: sipId,
                    username: username.isEmpty ? sipId : username,
                    deviceId: deviceId
                )
            )
            if result.count == maxAssignedIds { break }
        }
        return result
    }

    static func countAssigned(_ assignments: [String: SipIdAssignmentRecord]) -> Int {
        assignments.values.filter { !$0.firebaseUid.isEmpty }.count
    }

    static func findByUid(_ assignments: [String: SipIdAssignmentRecord], uid: String) -> SipIdAssignmentRecord? {
        let needle = uid.trimmingCharacters(in: .whitespacesAndNewlines)
        if needle.isEmpty { return nil }
        return assignments.values.first { $0.firebaseUid == needle }
    }

    static func isAssignedToOther(
        _ assignments: [String: SipIdAssignmentRecord],
        sipId: String,
        uid: String
    ) -> Bool {
        let holder = assignments[sipId]?.firebaseUid ?? ""
        return !holder.isEmpty && holder != uid.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    static func decide(
        pool: [SipLineIdentity],
        assignments: [String: SipIdAssignmentRecord],
        uid: String,
        nowMs: Int64,
        maxAssigned: Int = maxAssignedIds
    ) -> SipIdAssignmentResult {
        let trimmedUid = uid.trimmingCharacters(in: .whitespacesAndNewlines)
        if trimmedUid.isEmpty { return .poolFull }
        if let existing = findByUid(assignments, uid: trimmedUid) {
            return .alreadyAssigned(existing)
        }
        if countAssigned(assignments) >= maxAssigned { return .poolFull }
        guard let unused = pool.first(where: { (assignments[$0.sipId]?.firebaseUid ?? "").isEmpty }) else {
            return .noUnusedId
        }
        return .assigned(
            SipIdAssignmentRecord(
                sipId: unused.sipId,
                firebaseUid: trimmedUid,
                assignedAt: nowMs,
                username: unused.username,
                deviceId: unused.deviceId
            )
        )
    }

    static func decidePreferFirst(
        pool: [SipLineIdentity],
        assignments: [String: SipIdAssignmentRecord],
        uid: String,
        nowMs: Int64,
        maxAssigned: Int = maxAssignedIds
    ) -> SipIdAssignmentResult {
        let trimmedUid = uid.trimmingCharacters(in: .whitespacesAndNewlines)
        if trimmedUid.isEmpty { return .poolFull }
        if let existing = findByUid(assignments, uid: trimmedUid) {
            return .alreadyAssigned(existing)
        }
        if let first = pool.first, !isAssignedToOther(assignments, sipId: first.sipId, uid: trimmedUid) {
            return .assigned(
                SipIdAssignmentRecord(
                    sipId: first.sipId,
                    firebaseUid: trimmedUid,
                    assignedAt: nowMs,
                    username: first.username.isEmpty ? first.sipId : first.username,
                    deviceId: first.deviceId
                )
            )
        }
        return decide(pool: pool, assignments: assignments, uid: trimmedUid, nowMs: nowMs, maxAssigned: maxAssigned)
    }

    static func claimSpecific(
        identity: SipLineIdentity,
        assignments: [String: SipIdAssignmentRecord],
        uid: String,
        nowMs: Int64,
        maxAssigned: Int = maxAssignedIds
    ) -> SipIdAssignmentResult {
        let trimmedUid = uid.trimmingCharacters(in: .whitespacesAndNewlines)
        if trimmedUid.isEmpty || identity.sipId.isEmpty { return .noUnusedId }
        if let existing = findByUid(assignments, uid: trimmedUid) {
            return .alreadyAssigned(existing)
        }
        if isAssignedToOther(assignments, sipId: identity.sipId, uid: trimmedUid) {
            return .noUnusedId
        }
        let holder = assignments[identity.sipId]?.firebaseUid ?? ""
        if holder.isEmpty && countAssigned(assignments) >= maxAssigned {
            return .poolFull
        }
        return .assigned(
            SipIdAssignmentRecord(
                sipId: identity.sipId,
                firebaseUid: trimmedUid,
                assignedAt: nowMs,
                username: identity.username.isEmpty ? identity.sipId : identity.username,
                deviceId: identity.deviceId
            )
        )
    }

    static func applyToSipConfig(_ existing: SipConfig?, record: SipIdAssignmentRecord) -> SipConfig {
        var base = existing ?? SipConfig()
        let username = record.username.isEmpty ? base.username : record.username
        let deviceId = record.deviceId.isEmpty ? base.deviceId : record.deviceId
        if base.username != username || base.deviceId != deviceId {
            base.updatedAt = Int64(Date().timeIntervalSince1970 * 1000)
        }
        base.username = username
        base.deviceId = deviceId
        return base
    }

    static func parseAssignments(_ raw: Any?) -> [String: SipIdAssignmentRecord] {
        guard let map = raw as? [String: Any] else { return [:] }
        var result: [String: SipIdAssignmentRecord] = [:]
        for (sipId, value) in map {
            guard !sipId.isEmpty, let child = value as? [String: Any] else { continue }
            result[sipId] = recordFromMap(sipId: sipId, map: child)
        }
        return result
    }

    static func parseIdentities(_ raw: Any?) -> [SipLineIdentity] {
        guard let map = raw as? [String: Any] else { return [] }
        var result: [SipLineIdentity] = []
        var seen = Set<String>()
        for (sipId, value) in map {
            if sipId.isEmpty || seen.contains(sipId) { continue }
            let child = value as? [String: Any]
            let username = child?["username"] as? String ?? ""
            let deviceId = child?["deviceId"] as? String ?? ""
            seen.insert(sipId)
            result.append(
                SipLineIdentity(
                    sipId: sipId,
                    username: username.isEmpty ? sipId : username,
                    deviceId: deviceId
                )
            )
            if result.count == maxAssignedIds { break }
        }
        return result
    }

    static func recordFromMap(sipId: String, map: [String: Any]) -> SipIdAssignmentRecord {
        let username = map["username"] as? String ?? ""
        let deviceId = "\(map["deviceId"] ?? "")"
        let assignedAt: Int64
        if let number = map["assignedAt"] as? NSNumber {
            assignedAt = number.int64Value
        } else {
            assignedAt = 0
        }
        return SipIdAssignmentRecord(
            sipId: sipId,
            firebaseUid: map["firebaseUid"] as? String ?? "",
            assignedAt: assignedAt,
            username: username.isEmpty ? sipId : username,
            deviceId: deviceId
        )
    }
}
