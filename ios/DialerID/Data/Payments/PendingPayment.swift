import Foundation

enum PendingPaymentKind: String, Codable {
    case topUp = "TOP_UP"
    case deviceRegistration = "DEVICE_REGISTRATION"
}

struct PendingPayment: Equatable, Identifiable, Codable {
    static let validityMs: Int64 = 20 * 60 * 1000

    var paymentId: String
    var kind: PendingPaymentKind
    var amountUsd: Double
    var payCurrency: String
    var network: String
    var payAddress: String
    var payAmount: Double
    var extraId: String
    var qrPayload: String
    var status: String
    var createdAt: Int64
    var expiresAt: Int64
    var orderId: String = ""

    var id: String { paymentId }

    var isExpired: Bool {
        Int64(Date().timeIntervalSince1970 * 1000) >= expiresAt
    }

    var isTerminal: Bool {
        DeviceFeePolicy.isConfirmedStatus(status) || DeviceFeePolicy.isFailedStatus(status) || isExpired
    }

    var timeRemainingMs: Int64 {
        max(0, expiresAt - Int64(Date().timeIntervalSince1970 * 1000))
    }

    func formatTimeRemaining() -> String {
        let ms = timeRemainingMs
        if ms <= 0 { return "Expired" }
        let minutes = ms / 60_000
        let seconds = (ms / 1000) % 60
        if minutes > 0 {
            return String(format: "%dm %02ds", minutes, seconds)
        }
        return String(format: "%ds", seconds)
    }

    static func fromCheckout(
        _ checkout: NowCheckout,
        kind: PendingPaymentKind,
        qrPayload: String,
        expiresAt: Int64? = nil
    ) -> PendingPayment {
        PendingPayment(
            paymentId: checkout.paymentId,
            kind: kind,
            amountUsd: checkout.priceAmountUsd,
            payCurrency: checkout.payCurrency,
            network: checkout.network,
            payAddress: checkout.payAddress,
            payAmount: checkout.payAmount,
            extraId: checkout.extraId,
            qrPayload: qrPayload,
            status: checkout.paymentStatus.isEmpty ? "waiting" : checkout.paymentStatus,
            createdAt: checkout.createdAt,
            expiresAt: expiresAt ?? checkout.createdAt + validityMs,
            orderId: checkout.orderId
        )
    }
}

enum NowPaymentsInvoiceFactory {
    static func deviceRegistration(orderId: String, payCurrency: String) -> NowPaymentRequest {
        payment(
            priceAmountUsd: DeviceFeePolicy.deviceFeeUsd,
            payCurrency: payCurrency,
            orderId: orderId,
            orderDescription: "Register this device"
        )
    }

    static func topUp(amountUsd: Double, orderId: String, payCurrency: String) -> NowPaymentRequest {
        payment(
            priceAmountUsd: amountUsd,
            payCurrency: payCurrency,
            orderId: orderId,
            orderDescription: "Add credit"
        )
    }

    static func payment(
        priceAmountUsd: Double,
        payCurrency: String,
        orderId: String,
        orderDescription: String
    ) -> NowPaymentRequest {
        NowPaymentRequest(
            priceAmount: priceAmountUsd,
            priceCurrency: DeviceFeePolicy.priceCurrency,
            payCurrency: payCurrency.trimmingCharacters(in: .whitespacesAndNewlines).lowercased(),
            orderId: orderId,
            orderDescription: orderDescription,
            isFixedRate: true,
            isFeePaidByUser: true
        )
    }
}
