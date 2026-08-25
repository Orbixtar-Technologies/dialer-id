import Foundation

struct SipUpDevice: Equatable {
    var id: Int64
    var username: String
    var description: String = ""
    var callerIdNumber: String = ""
    var callerIdName: String = ""
}

struct SipUpSnapshot: Equatable {
    var device: SipUpDevice?
}

struct SipUpException: Error, LocalizedError {
    var statusCode: Int
    var message: String
    var errorDescription: String? { message }
}

enum SipUpDeviceMatcher {
    static func digitsOnly(_ raw: String) -> String {
        raw.filter(\.isNumber)
    }

    static func findDevice(devices: [SipUpDevice], sipUsername: String, sipDeviceId: String) -> SipUpDevice? {
        if devices.isEmpty { return nil }
        let username = sipUsername.trimmingCharacters(in: .whitespacesAndNewlines)
        let storedId = sipDeviceId.trimmingCharacters(in: .whitespacesAndNewlines)
        if let numericId = Int64(storedId), let match = devices.first(where: { $0.id == numericId }) {
            return match
        }
        if !username.isEmpty, let match = devices.first(where: { $0.username.caseInsensitiveCompare(username) == .orderedSame }) {
            return match
        }
        if !storedId.isEmpty {
            if let match = devices.first(where: { $0.username.caseInsensitiveCompare(storedId) == .orderedSame }) {
                return match
            }
            if let match = devices.first(where: { String($0.id) == storedId }) {
                return match
            }
        }
        if devices.count == 1 && username.isEmpty && storedId.isEmpty {
            return devices.first
        }
        return nil
    }

    static func toCallerIdNumber(_ raw: String) -> String {
        digitsOnly(raw)
    }

    static func isValidCallerIdNumber(_ raw: String) -> Bool {
        (3...32).contains(digitsOnly(raw).count)
    }
}

enum SipUpErrorMapper {
    static func userMessage(_ error: Error) -> String {
        if let typed = error as? SipUpException {
            return redact(typed.message.isEmpty ? messageForStatus(typed.statusCode) : typed.message)
        }
        let raw = error.localizedDescription
        if raw.localizedCaseInsensitiveContains("resolve") || raw.localizedCaseInsensitiveContains("offline") {
            return "Couldn't reach the service. Check your connection."
        }
        if raw.localizedCaseInsensitiveContains("timeout") {
            return "The request timed out. Try again."
        }
        return "Couldn't update your number. Try again."
    }

    static func messageForStatus(_ statusCode: Int, serverMessage: String? = nil) -> String {
        let fromServer = serverMessage
            .map(redact)
            .flatMap { $0.isEmpty || $0.count > 180 || $0.contains("{") ? nil : $0 }
        switch statusCode {
        case 401: return "This build is not authorized for that action."
        case 403: return "This app does not have permission for that action."
        case 402: return "This action isn't available right now."
        case 404: return "Couldn't find a line for this account."
        case 422: return fromServer ?? "This number was rejected. Try a different one."
        case 429: return "Too many requests. Try again shortly."
        case 503: return "The service is temporarily unavailable."
        case 400...499: return fromServer ?? "The request was rejected (\(statusCode))."
        case 500...599: return "The service is temporarily unavailable (\(statusCode))."
        default: return fromServer ?? "Couldn't reach the service."
        }
    }

    static func redact(_ text: String) -> String {
        var result = text.replacingOccurrences(of: #"sipup_[A-Za-z0-9_]+"#, with: "[redacted]", options: .regularExpression)
        result = result.replacingOccurrences(of: #"(?i)sip:[^\s<>]+"#, with: "", options: .regularExpression)
        result = result.replacingOccurrences(
            of: #"(?i)(?:[\w.+-]+@)?(?:sip\.)?sipup\.org\b"#,
            with: "",
            options: .regularExpression
        )
        result = result.replacingOccurrences(of: #"(?i)\bsipup\b"#, with: "", options: .regularExpression)
        return result.replacingOccurrences(of: "\\s{2,}", with: " ", options: .regularExpression)
            .trimmingCharacters(in: .whitespacesAndNewlines)
    }
}

final class SipUpClient: @unchecked Sendable {
    static let baseURL = URL(string: "https://sipup.org/")!

    private let apiKey: String
    private let session: URLSession

    init(apiKey: String = AppConfig.sipUpAPIKey, session: URLSession = .shared) {
        self.apiKey = apiKey
        self.session = session
    }

    func isConfigured() -> Bool {
        !apiKey.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    func listDevices() async throws -> [SipUpDevice] {
        try requireConfigured()
        let object = try await send(path: "api/v1/external/devices", method: "GET")
        let rows = (object["data"] as? [[String: Any]]) ?? []
        return rows.map(Self.device(from:))
    }

    func fetchSnapshot(sipUsername: String, sipDeviceId: String) async throws -> SipUpSnapshot {
        let devices = try await listDevices()
        let matched = SipUpDeviceMatcher.findDevice(
            devices: devices,
            sipUsername: sipUsername,
            sipDeviceId: sipDeviceId
        )
        return SipUpSnapshot(device: matched)
    }

    func updateCallerId(
        sipUsername: String,
        sipDeviceId: String,
        number: String,
        name: String
    ) async throws -> SipUpDevice {
        try requireConfigured()
        let digits = SipUpDeviceMatcher.toCallerIdNumber(number)
        if !SipUpDeviceMatcher.isValidCallerIdNumber(digits) {
            throw SipUpException(statusCode: 422, message: "Caller ID must be 3 to 32 digits after formatting is removed.")
        }
        let devices = try await listDevices()
        guard let matched = SipUpDeviceMatcher.findDevice(
            devices: devices,
            sipUsername: sipUsername,
            sipDeviceId: sipDeviceId
        ) else {
            throw SipUpException(statusCode: 404, message: "Couldn't find a line for this account.")
        }
        let object = try await send(
            path: "api/v1/external/devices/\(matched.id)/caller-id",
            method: "PATCH",
            body: ["number": digits, "name": name.trimmingCharacters(in: .whitespacesAndNewlines)]
        )
        if let data = object["data"] as? [String: Any] {
            return Self.device(from: data)
        }
        return SipUpDevice(
            id: matched.id,
            username: matched.username,
            description: matched.description,
            callerIdNumber: digits,
            callerIdName: name.trimmingCharacters(in: .whitespacesAndNewlines)
        )
    }

    private func requireConfigured() throws {
        if !isConfigured() {
            throw SipUpException(statusCode: 0, message: "This build is missing billing configuration.")
        }
    }

    private func send(path: String, method: String, body: [String: Any]? = nil) async throws -> [String: Any] {
        var request = URLRequest(url: Self.baseURL.appendingPathComponent(path))
        request.httpMethod = method
        request.setValue("Bearer \(apiKey)", forHTTPHeaderField: "Authorization")
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        if let body {
            request.httpBody = try JSONSerialization.data(withJSONObject: body)
            request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        }
        let (data, response) = try await session.data(for: request)
        let status = (response as? HTTPURLResponse)?.statusCode ?? 0
        let object = (try? JSONSerialization.jsonObject(with: data)) as? [String: Any] ?? [:]
        if (200...299).contains(status) {
            return object
        }
        let message = (object["message"] as? String) ?? (object["error"] as? String)
        throw SipUpException(
            statusCode: status,
            message: SipUpErrorMapper.messageForStatus(status, serverMessage: message)
        )
    }

    private static func device(from object: [String: Any]) -> SipUpDevice {
        let caller = object["caller_id"] as? [String: Any]
        let number = (caller?["number"] as? String) ?? ""
        let raw = (caller?["raw"] as? String) ?? ""
        return SipUpDevice(
            id: (object["id"] as? NSNumber)?.int64Value ?? 0,
            username: object["username"] as? String ?? "",
            description: object["description"] as? String ?? "",
            callerIdNumber: number.isEmpty ? SipUpDeviceMatcher.digitsOnly(raw) : number,
            callerIdName: caller?["name"] as? String ?? ""
        )
    }
}
