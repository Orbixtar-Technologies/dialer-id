import Foundation

enum DeviceFeePolicy {
    static let deviceFeeUsd = 50.0
    static let priceCurrency = "usd"

    static func shouldChargeNewDevice(alreadyRegistered: Bool) -> Bool {
        !alreadyRegistered
    }

    static func isConfirmedStatus(_ status: String?) -> Bool {
        switch status?.trimmingCharacters(in: .whitespacesAndNewlines).lowercased() {
        case "finished", "confirmed", "sending": return true
        default: return false
        }
    }

    static func isFailedStatus(_ status: String?) -> Bool {
        switch status?.trimmingCharacters(in: .whitespacesAndNewlines).lowercased() {
        case "failed", "expired", "refunded": return true
        default: return false
        }
    }

    static func isWaitingStatus(_ status: String?) -> Bool {
        switch status?.trimmingCharacters(in: .whitespacesAndNewlines).lowercased() {
        case nil, "", "waiting", "confirming", "partially_paid": return true
        default: return false
        }
    }
}

enum WalletCredit {
    static func applyIntendedTopUp(currentBalanceUsd: Double, intendedTopUpUsd: Double) -> Double {
        let credit = intendedTopUpUsd > 0 ? intendedTopUpUsd : 0
        return max(0, currentBalanceUsd + credit)
    }
}

enum CheckoutKind: String {
    case deviceRegistration = "DEVICE_REGISTRATION"
    case topUp = "TOP_UP"
}
