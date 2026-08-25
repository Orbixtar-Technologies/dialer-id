import Foundation

struct BalanceCache: Equatable {
    var balance: Double = 0
    var currency: String = "USD"
    var updatedAt: Int64 = Int64(Date().timeIntervalSince1970 * 1000)
}

struct SipConfig: Equatable {
    var callerId: String = ""
    var deviceId: String = ""
    var host: String = ""
    var password: String = ""
    var port: Int = 5060
    var username: String = ""
    var updatedAt: Int64 = Int64(Date().timeIntervalSince1970 * 1000)

    func hasIdentity() -> Bool {
        !host.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
            && !username.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    func needsPassword() -> Bool {
        hasIdentity() && password.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    func hasUsableCredentials() -> Bool {
        hasIdentity() && !password.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    func registrationFingerprint() -> String {
        "\(host)|\(port)|\(username)|\(password)|\(deviceId)"
    }

    func withResolvedPassword(_ localPassword: String) -> SipConfig {
        let resolved = password.isEmpty ? localPassword : password
        if resolved == password { return self }
        var copy = self
        copy.password = resolved
        return copy
    }

    func toRemoteMap() -> [String: Any] {
        [
            "callerId": callerId,
            "deviceId": deviceId,
            "host": host,
            "password": password,
            "port": port,
            "username": username,
            "updatedAt": updatedAt
        ]
    }
}

struct UserProfile: Equatable {
    static let guestUID = "guest_operator_001"

    var uid: String = guestUID
    var displayName: String = "Guest Operator"
    var email: String = "operator@dialerid.secure"
    var phoneNumber: String = ""
    var photoUrl: String?
    var creditBalance: Double = 0
    var currency: String = "USD"
    var selectedCallerId: String = ""
    var accountType: String = "Enterprise VoIP Trunk"
    var accountRole: String = ""
    var organization: String = ""
    var presence: String = "Online & Ready"
    var networkStatus: String = "Realtime DB Connected"
    var isVerified = false
    var isEncrypted = false
    var audioQuality: String = "Standard Line"
    var preferredCodec: String = "G711_AUTO"
    var callsCount: Int = 0
    var totalMinutes: Int = 0
    var assignedSipId: String = ""
    var sipConfig: SipConfig?
    var isCloudSynced = false
    var createdAt: Int64 = Int64(Date().timeIntervalSince1970 * 1000)
    var lastSyncTimestamp: Int64 = 0
    var lastUpdated: Int64 = Int64(Date().timeIntervalSince1970 * 1000)

    var isGuest: Bool {
        uid.isEmpty || uid == Self.guestUID
    }

    func toMap() -> [String: Any] {
        var root: [String: Any] = [
            "uid": uid,
            "profile": [
                "display_name": displayName,
                "displayName": displayName,
                "email": email,
                "createdAt": createdAt
            ],
            "balanceCache": [
                "balance": creditBalance,
                "currency": currency,
                "updatedAt": Int64(Date().timeIntervalSince1970 * 1000)
            ],
            "selectedCallerId": selectedCallerId,
            "displayName": displayName,
            "email": email,
            "phoneNumber": phoneNumber,
            "photoUrl": photoUrl as Any,
            "creditBalance": creditBalance,
            "accountType": accountType,
            "accountRole": accountRole,
            "organization": organization,
            "presence": presence,
            "networkStatus": networkStatus,
            "isVerified": isVerified,
            "isEncrypted": isEncrypted,
            "audioQuality": audioQuality,
            "preferredCodec": preferredCodec,
            "callsCount": callsCount,
            "totalMinutes": totalMinutes,
            "isCloudSynced": isCloudSynced,
            "createdAt": createdAt,
            "lastSyncTimestamp": lastSyncTimestamp,
            "lastUpdated": Int64(Date().timeIntervalSince1970 * 1000),
            "assignedSipId": assignedSipId
        ]
        if let deviceId = sipConfig?.deviceId, !deviceId.isEmpty {
            root["deviceId"] = deviceId
        }
        if let sip = sipConfig {
            root["sip"] = sip.toRemoteMap()
        }
        return root
    }

    static func fromMap(_ map: [String: Any], uid: String = "") -> UserProfile {
        let resolvedUid: String
        if !uid.isEmpty {
            resolvedUid = uid
        } else if let value = map["uid"] as? String {
            resolvedUid = value
        } else {
            resolvedUid = guestUID
        }

        let nestedProfile = map["profile"] as? [String: Any]
        let displayName = (nestedProfile?["display_name"] as? String)
            ?? (nestedProfile?["displayName"] as? String)
            ?? (map["displayName"] as? String)
            ?? (map["display_name"] as? String)
            ?? "Operator"
        let email = (nestedProfile?["email"] as? String)
            ?? (map["email"] as? String)
            ?? "operator@dialerid.secure"
        var rawCreatedAt = int64(nestedProfile?["createdAt"]) ?? int64(map["createdAt"])
            ?? Int64(Date().timeIntervalSince1970 * 1000)
        if (1...9_999_999_999).contains(rawCreatedAt) {
            rawCreatedAt *= 1000
        }

        let nestedBalance = map["balanceCache"] as? [String: Any]
        let rawBalance = nestedBalance?["balance"] ?? map["creditBalance"] ?? map["balance"]
        let balance = double(rawBalance)
        let currency = (nestedBalance?["currency"] as? String) ?? (map["currency"] as? String) ?? "USD"
        let selectedCallerId = (map["selectedCallerId"] as? String)
            ?? (map["selected_caller_id"] as? String)
            ?? ""
        let assignedSipId = (map["assignedSipId"] as? String)
            ?? (map["assigned_sip_id"] as? String)
            ?? ""
        let rootDeviceId = "\(map["deviceId"] ?? "")"

        var sipConfig: SipConfig?
        if let nestedSip = map["sip"] as? [String: Any] {
            let port: Int
            if let number = nestedSip["port"] as? NSNumber {
                port = number.intValue
            } else if let string = nestedSip["port"] as? String {
                port = Int(string) ?? 5060
            } else {
                port = 5060
            }
            let password: String
            if let string = nestedSip["password"] as? String {
                password = string
            } else if let number = nestedSip["password"] as? NSNumber {
                password = number.stringValue
            } else {
                password = ""
            }
            sipConfig = SipConfig(
                callerId: (nestedSip["callerId"] as? String) ?? selectedCallerId,
                deviceId: {
                    let value = "\(nestedSip["deviceId"] ?? "")"
                    return value.isEmpty ? rootDeviceId : value
                }(),
                host: nestedSip["host"] as? String ?? "",
                password: password,
                port: port,
                username: {
                    let value = nestedSip["username"] as? String ?? ""
                    return value.isEmpty ? assignedSipId : value
                }(),
                updatedAt: int64(nestedSip["updatedAt"]) ?? Int64(Date().timeIntervalSince1970 * 1000)
            )
        } else if !assignedSipId.isEmpty || !rootDeviceId.isEmpty {
            sipConfig = SipConfig(
                deviceId: rootDeviceId,
                username: assignedSipId,
                updatedAt: Int64(Date().timeIntervalSince1970 * 1000)
            )
        }

        return UserProfile(
            uid: resolvedUid,
            displayName: displayName,
            email: email,
            phoneNumber: map["phoneNumber"] as? String ?? "",
            photoUrl: map["photoUrl"] as? String,
            creditBalance: balance,
            currency: currency,
            selectedCallerId: selectedCallerId,
            accountType: map["accountType"] as? String ?? "Enterprise VoIP Trunk",
            accountRole: map["accountRole"] as? String ?? "",
            organization: map["organization"] as? String ?? "",
            presence: map["presence"] as? String ?? "Online & Ready",
            networkStatus: map["networkStatus"] as? String ?? "Realtime DB Synced",
            isVerified: map["isVerified"] as? Bool ?? false,
            isEncrypted: map["isEncrypted"] as? Bool ?? false,
            audioQuality: map["audioQuality"] as? String ?? "Standard Line",
            preferredCodec: map["preferredCodec"] as? String ?? "G711_AUTO",
            callsCount: int(map["callsCount"]),
            totalMinutes: int(map["totalMinutes"]),
            assignedSipId: assignedSipId,
            sipConfig: sipConfig,
            isCloudSynced: true,
            createdAt: rawCreatedAt,
            lastSyncTimestamp: Int64(Date().timeIntervalSince1970 * 1000),
            lastUpdated: int64(map["lastUpdated"]) ?? Int64(Date().timeIntervalSince1970 * 1000)
        )
    }

    private static func double(_ value: Any?) -> Double {
        switch value {
        case let number as NSNumber: return number.doubleValue
        case let string as String: return Double(string) ?? 0
        default: return 0
        }
    }

    private static func int(_ value: Any?) -> Int {
        switch value {
        case let number as NSNumber: return number.intValue
        case let string as String: return Int(string) ?? 0
        default: return 0
        }
    }

    private static func int64(_ value: Any?) -> Int64? {
        switch value {
        case let number as NSNumber: return number.int64Value
        case let string as String: return Int64(string)
        default: return nil
        }
    }
}

enum CallStatus: String, Codable, CaseIterable {
    case completed = "COMPLETED"
    case cancelled = "CANCELLED"
    case failed = "FAILED"
    case noAnswer = "NO_ANSWER"
    case disconnected = "DISCONNECTED"
}

struct CallerIdItem: Equatable, Identifiable {
    var id: String
    var phoneNumber: String
    var label: String
    var isPrimary = false
    var isVerified = false
    var countryCode: String = "US"
    var host: String = ""
    var port: String = ""
    var username: String = ""
    var createdAt: Int64 = Int64(Date().timeIntervalSince1970 * 1000)
}

struct CallLogItem: Equatable, Identifiable {
    var id: String
    var destinationNumber: String
    var callerIdUsed: String
    var countryName: String
    var status: CallStatus
    var timestamp: Int64 = Int64(Date().timeIntervalSince1970 * 1000)
    var durationSeconds: Int
    var billingRatePerMin: Double
    var totalCost: Double
}
