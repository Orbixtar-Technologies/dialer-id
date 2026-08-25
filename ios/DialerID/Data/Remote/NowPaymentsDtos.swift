import Foundation

func jsonId(_ value: Any?) -> String {
    switch value {
    case nil:
        return ""
    case let number as NSNumber:
        let asDouble = number.doubleValue
        if asDouble.truncatingRemainder(dividingBy: 1) == 0 {
            return String(number.int64Value)
        }
        return String(asDouble)
    default:
        return "\(value ?? "")".trimmingCharacters(in: .whitespacesAndNewlines)
    }
}

func jsonAmount(_ value: Any?) -> Double {
    switch value {
    case let number as NSNumber: return number.doubleValue
    case let string as String: return Double(string) ?? 0
    default: return 0
    }
}

struct NowPaymentRequest: Encodable {
    var priceAmount: Double
    var priceCurrency: String = "usd"
    var payCurrency: String
    var orderId: String
    var orderDescription: String
    var isFixedRate: Bool = true
    var isFeePaidByUser: Bool = true

    enum CodingKeys: String, CodingKey {
        case priceAmount = "price_amount"
        case priceCurrency = "price_currency"
        case payCurrency = "pay_currency"
        case orderId = "order_id"
        case orderDescription = "order_description"
        case isFixedRate = "is_fixed_rate"
        case isFeePaidByUser = "is_fee_paid_by_user"
    }
}

struct NowPaymentDto {
    var paymentIdRaw: Any?
    var invoiceIdRaw: Any?
    var paymentStatus: String?
    var orderId: String?
    var orderDescription: String?
    var priceAmount: Any?
    var priceCurrency: String?
    var payAddress: String?
    var payAmountRaw: Any?
    var payCurrency: String?
    var payinExtraId: String?
    var extraIdRaw: String?
    var network: String?
    var isFixedRate: Bool?
    var isFeePaidByUser: Bool?

    var paymentId: String { jsonId(paymentIdRaw) }
    var invoiceId: String { jsonId(invoiceIdRaw) }
    var payAmount: Double { jsonAmount(payAmountRaw) }
    var extraId: String {
        let payin = payinExtraId?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        return payin.isEmpty ? (extraIdRaw?.trimmingCharacters(in: .whitespacesAndNewlines) ?? "") : payin
    }

    static func from(_ object: [String: Any]) -> NowPaymentDto {
        NowPaymentDto(
            paymentIdRaw: object["payment_id"],
            invoiceIdRaw: object["invoice_id"],
            paymentStatus: object["payment_status"] as? String,
            orderId: object["order_id"] as? String,
            orderDescription: object["order_description"] as? String,
            priceAmount: object["price_amount"],
            priceCurrency: object["price_currency"] as? String,
            payAddress: object["pay_address"] as? String,
            payAmountRaw: object["pay_amount"],
            payCurrency: object["pay_currency"] as? String,
            payinExtraId: object["payin_extra_id"] as? String,
            extraIdRaw: object["extra_id"] as? String,
            network: object["network"] as? String,
            isFixedRate: object["is_fixed_rate"] as? Bool,
            isFeePaidByUser: object["is_fee_paid_by_user"] as? Bool
        )
    }
}

struct NowCheckout: Equatable {
    var paymentId: String
    var payAddress: String
    var payAmount: Double
    var payCurrency: String
    var extraId: String
    var network: String
    var paymentStatus: String
    var orderId: String
    var priceAmountUsd: Double
    var createdAt: Int64 = Int64(Date().timeIntervalSince1970 * 1000)
}

struct NowFullCurrency: Equatable {
    var id: Int = 0
    var code: String = ""
    var ticker: String = ""
    var name: String = ""
    var network: String = ""
    var blockchain: String = ""
    var logoUrl: String?
    var image: String?
    var availableForPayment = true
    var enable = true

    var payTicker: String {
        [code, ticker].first { !$0.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }?
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .lowercased() ?? ""
    }

    var iconUrl: String? {
        let logo = logoUrl?.trimmingCharacters(in: .whitespacesAndNewlines)
        if let logo, !logo.isEmpty { return logo }
        let img = image?.trimmingCharacters(in: .whitespacesAndNewlines)
        return (img?.isEmpty == false) ? img : nil
    }

    static func from(_ object: [String: Any]) -> NowFullCurrency {
        NowFullCurrency(
            id: (object["id"] as? NSNumber)?.intValue ?? 0,
            code: object["code"] as? String ?? "",
            ticker: object["ticker"] as? String ?? "",
            name: object["name"] as? String ?? "",
            network: object["network"] as? String ?? "",
            blockchain: object["blockchain"] as? String ?? "",
            logoUrl: object["logo_url"] as? String,
            image: object["image"] as? String,
            availableForPayment: object["available_for_payment"] as? Bool ?? true,
            enable: object["enable"] as? Bool ?? true
        )
    }
}

struct NowMinAmountResponse: Equatable {
    var currencyFrom: String?
    var currencyTo: String?
    var minAmountRaw: Any?
    var fiatEquivalentRaw: Any?

    var minAmount: Double { jsonAmount(minAmountRaw) }
    var fiatEquivalentUsd: Double { jsonAmount(fiatEquivalentRaw) }

    func resolvedMinAmountUsd() -> Double {
        fiatEquivalentUsd > 0 ? fiatEquivalentUsd : 0
    }

    static func from(_ object: [String: Any]) -> NowMinAmountResponse {
        NowMinAmountResponse(
            currencyFrom: object["currency_from"] as? String,
            currencyTo: object["currency_to"] as? String,
            minAmountRaw: object["min_amount"],
            fiatEquivalentRaw: object["fiat_equivalent"]
        )
    }
}

struct NowPaymentsException: Error, LocalizedError {
    var statusCode: Int
    var message: String

    var errorDescription: String? { message }
}

enum NowPaymentsErrorMapper {
    static func userMessage(_ error: Error) -> String {
        if let typed = error as? NowPaymentsException {
            let mapped = typed.message.isEmpty ? messageForStatus(typed.statusCode) : typed.message
            return redact(mapped)
        }
        let raw = error.localizedDescription
        if raw.localizedCaseInsensitiveContains("resolve") || raw.localizedCaseInsensitiveContains("offline") {
            return "Couldn't reach checkout. Check your connection."
        }
        if raw.localizedCaseInsensitiveContains("timeout") {
            return "Checkout timed out. Try again."
        }
        return redact(messageForStatus(0, serverMessage: raw))
    }

    static func messageForStatus(_ statusCode: Int, serverMessage: String? = nil) -> String {
        let fromServer = serverMessage
            .map(redact)
            .flatMap { $0.isEmpty || $0.count > 180 || $0.contains("{") ? nil : $0 }
        switch statusCode {
        case 401, 403: return "Checkout is not authorized for this build."
        case 429: return "Too many payment requests. Try again shortly."
        case 402: return "This payment isn't available right now."
        case 404: return "Couldn't find that payment."
        case 422: return fromServer ?? "Checkout rejected this amount. Try a different amount."
        case 503: return "Checkout is temporarily unavailable."
        case 400...499: return fromServer ?? "Checkout rejected the request."
        case 500...599: return "Checkout is temporarily unavailable."
        default: return fromServer ?? "Couldn't start checkout."
        }
    }

    static func redact(_ text: String) -> String {
        var result = text.replacingOccurrences(
            of: #"(?i)x-api-key[:\s=]+\S+"#,
            with: "x-api-key [redacted]",
            options: .regularExpression
        )
        result = result.replacingOccurrences(
            of: #"(?i)bearer\s+\S+"#,
            with: "Bearer [redacted]",
            options: .regularExpression
        )
        result = result.replacingOccurrences(
            of: #"\b[A-Za-z0-9_-]{20,}\b"#,
            with: "[redacted]",
            options: .regularExpression
        )
        return result.replacingOccurrences(of: "\\s{2,}", with: " ", options: .regularExpression)
            .trimmingCharacters(in: .whitespacesAndNewlines)
    }
}
