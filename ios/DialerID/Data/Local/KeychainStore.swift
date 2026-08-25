import Foundation
import Security
import UIKit

final class KeychainStore {
    static let shared = KeychainStore()
    private let service = "com.dialerid.app.sip"

    func savePassword(uid: String, password: String) {
        let account = "sip.\(uid)"
        let data = Data(password.utf8)
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account
        ]
        SecItemDelete(query as CFDictionary)
        var add = query
        add[kSecValueData as String] = data
        add[kSecAttrAccessible as String] = kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
        SecItemAdd(add as CFDictionary, nil)
    }

    func password(uid: String) -> String {
        let account = "sip.\(uid)"
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account,
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne
        ]
        var result: AnyObject?
        let status = SecItemCopyMatching(query as CFDictionary, &result)
        guard status == errSecSuccess, let data = result as? Data else { return "" }
        return String(data: data, encoding: .utf8) ?? ""
    }

    func deletePassword(uid: String) {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: "sip.\(uid)"
        ]
        SecItemDelete(query as CFDictionary)
    }
}

enum DeviceIdentity {
    private static let fallbackKey = "fallback_vendor_id"

    static func stableDeviceId() -> String {
        if let vendor = UIDevice.current.identifierForVendor?.uuidString,
           !vendor.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            return vendor
        }
        if let existing = UserDefaults.standard.string(forKey: fallbackKey), !existing.isEmpty {
            return existing
        }
        let created = UUID().uuidString
        UserDefaults.standard.set(created, forKey: fallbackKey)
        return created
    }

    static func deviceLabel() -> String {
        let model = UIDevice.current.model.trimmingCharacters(in: .whitespacesAndNewlines)
        let name = UIDevice.current.name.trimmingCharacters(in: .whitespacesAndNewlines)
        let label = [model, name].filter { !$0.isEmpty }.joined(separator: " ")
        return label.isEmpty ? "iPhone" : label
    }
}

final class PendingPaymentStore {
    static let shared = PendingPaymentStore()
    private let key = "pending_crypto_payments"

    func load() -> [PendingPayment] {
        guard let data = UserDefaults.standard.data(forKey: key),
              let items = try? JSONDecoder().decode([PendingPayment].self, from: data) else {
            return []
        }
        return items
    }

    func save(_ items: [PendingPayment]) {
        if let data = try? JSONEncoder().encode(items) {
            UserDefaults.standard.set(data, forKey: key)
        }
    }

    func upsert(_ payment: PendingPayment) {
        var items = load().filter { $0.paymentId != payment.paymentId }
        items.insert(payment, at: 0)
        save(items)
    }

    func remove(paymentId: String) {
        save(load().filter { $0.paymentId != paymentId })
    }
}

final class LocalCallLogStore {
    static let shared = LocalCallLogStore()
    private let key = "call_logs_cache"

    func load() -> [CallLogItem] {
        guard let data = UserDefaults.standard.data(forKey: key),
              let items = try? JSONDecoder().decode([CodableCallLog].self, from: data) else {
            return []
        }
        return items.map(\.item)
    }

    func save(_ items: [CallLogItem]) {
        let payload = items.map(CodableCallLog.init)
        if let data = try? JSONEncoder().encode(payload) {
            UserDefaults.standard.set(data, forKey: key)
        }
    }

    private struct CodableCallLog: Codable {
        var id: String
        var destinationNumber: String
        var callerIdUsed: String
        var countryName: String
        var status: String
        var timestamp: Int64
        var durationSeconds: Int
        var billingRatePerMin: Double
        var totalCost: Double

        init(_ item: CallLogItem) {
            id = item.id
            destinationNumber = item.destinationNumber
            callerIdUsed = item.callerIdUsed
            countryName = item.countryName
            status = item.status.rawValue
            timestamp = item.timestamp
            durationSeconds = item.durationSeconds
            billingRatePerMin = item.billingRatePerMin
            totalCost = item.totalCost
        }

        var item: CallLogItem {
            CallLogItem(
                id: id,
                destinationNumber: destinationNumber,
                callerIdUsed: callerIdUsed,
                countryName: countryName,
                status: CallStatus(rawValue: status) ?? .completed,
                timestamp: timestamp,
                durationSeconds: durationSeconds,
                billingRatePerMin: billingRatePerMin,
                totalCost: totalCost
            )
        }
    }
}
