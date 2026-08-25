import Foundation

enum CryptoPayUri {
    static func encode(
        address: String,
        payCurrency: String,
        payAmount: Double?,
        extraId: String?
    ) -> String {
        let trimmedAddress = address.trimmingCharacters(in: .whitespacesAndNewlines)
        if trimmedAddress.isEmpty { return "" }
        let ticker = payCurrency.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        let extra = extraId?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        let amount = payAmount.flatMap { $0 > 0 ? $0 : nil }
        switch ticker {
        case "btc": return bip21("bitcoin", address: trimmedAddress, amount: amount)
        case "ltc": return bip21("litecoin", address: trimmedAddress, amount: amount)
        case "bch": return bip21("bitcoincash", address: trimmedAddress, amount: amount)
        case "doge": return bip21("dogecoin", address: trimmedAddress, amount: amount)
        case "xrp": return ripple(trimmedAddress, extra: extra, amount: amount)
        case "xlm": return stellar(trimmedAddress, extra: extra)
        case "ton", "toncoin": return ton(trimmedAddress, extra: extra, amount: amount)
        default: return extra.isEmpty ? trimmedAddress : trimmedAddress
        }
    }

    private static func bip21(_ scheme: String, address: String, amount: Double?) -> String {
        if let amount {
            return "\(scheme):\(address)?amount=\(formatAmount(amount))"
        }
        return "\(scheme):\(address)"
    }

    private static func ripple(_ address: String, extra: String, amount: Double?) -> String {
        var params: [String] = []
        if !extra.isEmpty { params.append("dt=\(encodeQuery(extra))") }
        if let amount { params.append("amount=\(formatAmount(amount))") }
        return params.isEmpty ? "ripple:\(address)" : "ripple:\(address)?\(params.joined(separator: "&"))"
    }

    private static func stellar(_ address: String, extra: String) -> String {
        var params = ["destination=\(encodeQuery(address))"]
        if !extra.isEmpty { params.append("memo=\(encodeQuery(extra))") }
        return "web+stellar:pay?\(params.joined(separator: "&"))"
    }

    private static func ton(_ address: String, extra: String, amount: Double?) -> String {
        var params: [String] = []
        if let amount { params.append("amount=\(formatAmount(amount))") }
        if !extra.isEmpty { params.append("text=\(encodeQuery(extra))") }
        return params.isEmpty ? "ton://transfer/\(address)" : "ton://transfer/\(address)?\(params.joined(separator: "&"))"
    }

    private static func formatAmount(_ amount: Double) -> String {
        var formatted = String(format: "%.8f", amount)
        while formatted.last == "0" { formatted.removeLast() }
        if formatted.last == "." { formatted.removeLast() }
        return formatted
    }

    private static func encodeQuery(_ value: String) -> String {
        var allowed = CharacterSet.urlQueryAllowed
        allowed.remove(charactersIn: "+&=")
        return value.addingPercentEncoding(withAllowedCharacters: allowed)?
            .replacingOccurrences(of: "+", with: "%20") ?? value
    }
}
