import Foundation

final class NowPaymentsClient: @unchecked Sendable {
    static let baseURL = URL(string: "https://api.nowpayments.io/")!

    private let apiKey: String
    private let session: URLSession

    init(apiKey: String = AppConfig.nowPaymentsAPIKey, session: URLSession = .shared) {
        self.apiKey = apiKey
        self.session = session
    }

    func isConfigured() -> Bool {
        !apiKey.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    func listPayCurrencies() async throws -> [String] {
        try requireConfigured()
        let object = try await getJSON(path: "v1/currencies", query: ["fixed_rate": "true"])
        let currencies = (object["currencies"] as? [String]) ?? []
        let tickers = CryptoCurrencyCatalog.normalize(currencies)
        if tickers.isEmpty {
            throw NowPaymentsException(statusCode: 0, message: "No currencies are available right now.")
        }
        return tickers
    }

    func listFullCurrencies() async throws -> [NowFullCurrency] {
        try requireConfigured()
        let object = try await getJSON(path: "v1/full-currencies")
        let rows = (object["currencies"] as? [[String: Any]]) ?? []
        return rows.map(NowFullCurrency.from).filter {
            $0.enable && $0.availableForPayment && !$0.payTicker.isEmpty
        }
    }

    func minAmountUsd(payCurrency: String) async throws -> Double {
        try requireConfigured()
        let ticker = payCurrency.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        if ticker.isEmpty {
            throw NowPaymentsException(statusCode: 0, message: "Choose a currency to continue.")
        }
        let object = try await getJSON(
            path: "v1/min-amount",
            query: [
                "currency_from": DeviceFeePolicy.priceCurrency,
                "currency_to": ticker,
                "fiat_equivalent": DeviceFeePolicy.priceCurrency,
                "is_fixed_rate": "true",
                "is_fee_paid_by_user": "false"
            ]
        )
        let minUsd = NowMinAmountResponse.from(object).resolvedMinAmountUsd()
        if minUsd <= 0 {
            throw NowPaymentsException(statusCode: 0, message: "Couldn't load the minimum amount for \(ticker).")
        }
        return minUsd
    }

    func minAmountsUsd(tickers: [String]) async -> [String: Double] {
        var amounts: [String: Double] = [:]
        for ticker in CryptoCurrencyCatalog.normalize(tickers) {
            if let min = try? await minAmountUsd(payCurrency: ticker) {
                amounts[ticker] = min
            }
        }
        return amounts
    }

    func createPayment(_ request: NowPaymentRequest) async throws -> NowCheckout {
        try requireConfigured()
        let object = try await postJSON(path: "v1/payment", body: request)
        let dto = NowPaymentDto.from(object)
        let payAddress = dto.payAddress?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        if dto.paymentId.isEmpty || payAddress.isEmpty {
            throw NowPaymentsException(statusCode: 0, message: "Checkout did not return a payment address.")
        }
        return NowCheckout(
            paymentId: dto.paymentId,
            payAddress: payAddress,
            payAmount: dto.payAmount > 0 ? dto.payAmount : 0,
            payCurrency: {
                let value = dto.payCurrency ?? ""
                return value.isEmpty ? request.payCurrency : value
            }(),
            extraId: dto.extraId,
            network: dto.network ?? "",
            paymentStatus: {
                let value = dto.paymentStatus ?? ""
                return value.isEmpty ? "waiting" : value
            }(),
            orderId: {
                let value = dto.orderId ?? ""
                return value.isEmpty ? request.orderId : value
            }(),
            priceAmountUsd: {
                let amount = jsonAmount(dto.priceAmount)
                return amount > 0 ? amount : request.priceAmount
            }()
        )
    }

    func getPayment(id: String) async throws -> NowPaymentDto {
        try requireConfigured()
        let object = try await getJSON(path: "v1/payment/\(id)")
        return NowPaymentDto.from(object)
    }

    private func requireConfigured() throws {
        if !isConfigured() {
            throw NowPaymentsException(statusCode: 0, message: "This build is missing checkout configuration.")
        }
    }

    private func getJSON(path: String, query: [String: String] = [:]) async throws -> [String: Any] {
        var components = URLComponents(url: Self.baseURL.appendingPathComponent(path), resolvingAgainstBaseURL: false)!
        if !query.isEmpty {
            components.queryItems = query.map { URLQueryItem(name: $0.key, value: $0.value) }
        }
        var request = URLRequest(url: components.url!)
        request.httpMethod = "GET"
        applyHeaders(&request)
        return try await send(request)
    }

    private func postJSON<T: Encodable>(path: String, body: T) async throws -> [String: Any] {
        var request = URLRequest(url: Self.baseURL.appendingPathComponent(path))
        request.httpMethod = "POST"
        request.httpBody = try JSONEncoder().encode(body)
        applyHeaders(&request)
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        return try await send(request)
    }

    private func applyHeaders(_ request: inout URLRequest) {
        request.setValue(apiKey, forHTTPHeaderField: "x-api-key")
        request.setValue("application/json", forHTTPHeaderField: "Accept")
    }

    private func send(_ request: URLRequest) async throws -> [String: Any] {
        let (data, response) = try await session.data(for: request)
        let status = (response as? HTTPURLResponse)?.statusCode ?? 0
        let object = (try? JSONSerialization.jsonObject(with: data)) as? [String: Any] ?? [:]
        if (200...299).contains(status) {
            return object
        }
        let message = (object["message"] as? String) ?? (object["error"] as? String)
        throw NowPaymentsException(
            statusCode: status,
            message: NowPaymentsErrorMapper.messageForStatus(status, serverMessage: message)
        )
    }
}
