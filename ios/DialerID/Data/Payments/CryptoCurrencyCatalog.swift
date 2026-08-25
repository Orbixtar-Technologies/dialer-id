import Foundation

struct CryptoCurrencyOption: Equatable {
    var ticker: String
    var label: String
    var featured: Bool
}

enum CryptoCurrencyCatalog {
    static let featured = [
        "btc", "eth", "ltc", "usdt", "usdterc20", "usdttrc20", "usdc",
        "xrp", "doge", "trx", "sol", "bnb", "bch", "ada", "ton", "dai"
    ]

    static func normalize(_ tickers: [String]) -> [String] {
        var seen = Set<String>()
        var result: [String] = []
        for ticker in tickers {
            let value = ticker.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
            if value.isEmpty || seen.contains(value) { continue }
            seen.insert(value)
            result.append(value)
        }
        return result
    }

    static func options(available: [String], query: String) -> [CryptoCurrencyOption] {
        let normalized = normalize(available)
        let featuredSet = Set(featured)
        let filtered: [String]
        if query.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            let featuredMatches = featured.filter { Set(normalized).contains($0) }
            filtered = featuredMatches.isEmpty ? Array(normalized.prefix(16)) : featuredMatches
        } else {
            let needle = query.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
            filtered = normalized.filter {
                $0.contains(needle)
                    || label($0).lowercased().contains(needle)
                    || tickerDisplay($0).lowercased().contains(needle)
            }
        }
        return filtered.map {
            CryptoCurrencyOption(ticker: $0, label: label($0), featured: featuredSet.contains($0))
        }
    }

    static func label(_ ticker: String) -> String {
        switch ticker.trimmingCharacters(in: .whitespacesAndNewlines).lowercased() {
        case "btc": return "Bitcoin"
        case "eth": return "Ethereum"
        case "ltc": return "Litecoin"
        case "usdt": return "Tether"
        case "usdterc20": return "Tether (ERC-20)"
        case "usdttrc20": return "Tether (TRC-20)"
        case "usdtsol": return "Tether (Solana)"
        case "usdc", "usdce": return "USD Coin"
        case "xrp": return "XRP"
        case "doge": return "Dogecoin"
        case "trx": return "TRON"
        case "sol": return "Solana"
        case "bnb", "bnbmainnet": return "BNB"
        case "bch": return "Bitcoin Cash"
        case "ada": return "Cardano"
        case "ton", "toncoin": return "TON"
        case "dai": return "Dai"
        case "matic", "maticmainnet": return "Polygon"
        case "xlm": return "Stellar"
        default: return ticker.trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
        }
    }

    static func tickerDisplay(_ ticker: String) -> String {
        ticker.trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
    }
}

struct CryptoNetworkOption: Equatable {
    var ticker: String
    var networkLabel: String
    var minAmountUsd: Double?
    var iconUrl: String?

    func isEnabledForAmount(_ usdAmount: Double) -> Bool {
        guard let min = minAmountUsd else { return true }
        return usdAmount >= min
    }

    func disabledReason(_ usdAmount: Double) -> String? {
        guard !isEnabledForAmount(usdAmount), let min = minAmountUsd else { return nil }
        return String(format: "Min $%.2f", min)
    }
}

struct CryptoCoinGroup: Equatable, Identifiable {
    var id: String
    var name: String
    var iconTicker: String
    var iconUrl: String?
    var networks: [CryptoNetworkOption]
    var featured: Bool

    var hasMultipleNetworks: Bool { networks.count > 1 }

    func minAmountUsd() -> Double? {
        networks.compactMap(\.minAmountUsd).min()
    }

    func isEnabledForAmount(_ usdAmount: Double) -> Bool {
        networks.contains { $0.isEnabledForAmount(usdAmount) }
    }

    func disabledReason(_ usdAmount: Double) -> String? {
        if isEnabledForAmount(usdAmount) { return nil }
        if let min = minAmountUsd() {
            return String(format: "Min $%.2f", min)
        }
        return "Amount too low"
    }
}

enum CryptoCoinCatalog {
    private static let networkSuffixes: [(String, String)] = [
        ("erc20", "ERC-20"),
        ("trc20", "TRC-20"),
        ("bep20", "BEP-20"),
        ("bsc", "BSC"),
        ("matic", "Polygon"),
        ("mainnet", "Mainnet"),
        ("sol", "Solana"),
        ("arb", "Arbitrum"),
        ("op", "Optimism"),
        ("base", "Base"),
        ("avax", "Avalanche")
    ]

    static func buildGroups(
        availableTickers: [String],
        fullCurrencies: [NowFullCurrency],
        minAmountsUsd: [String: Double],
        query: String = ""
    ) -> [CryptoCoinGroup] {
        let available = Set(CryptoCurrencyCatalog.normalize(availableTickers))
        if available.isEmpty { return [] }
        var metaByTicker: [String: NowFullCurrency] = [:]
        for currency in fullCurrencies {
            let ticker = currency.payTicker
            if !ticker.isEmpty { metaByTicker[ticker] = currency }
        }
        let networkOptions = available.compactMap { ticker -> CryptoNetworkOption? in
            let meta = metaByTicker[ticker]
            return CryptoNetworkOption(
                ticker: ticker,
                networkLabel: networkLabel(ticker, meta: meta),
                minAmountUsd: minAmountsUsd[ticker],
                iconUrl: meta?.iconUrl
            )
        }
        let grouped = Dictionary(grouping: networkOptions, by: { coinId($0.ticker) })
        let featuredSet = Set(CryptoCurrencyCatalog.featured)
        let groups = grouped.map { coinId, networks in
            let sortedNetworks = networks.sorted { $0.networkLabel < $1.networkLabel }
            let primaryTicker = sortedNetworks[0].ticker
            let meta = metaByTicker[primaryTicker]
            return CryptoCoinGroup(
                id: coinId,
                name: coinName(coinId, meta: meta),
                iconTicker: iconTicker(coinId, fallbackTicker: primaryTicker),
                iconUrl: meta?.iconUrl ?? sortedNetworks.compactMap(\.iconUrl).first,
                networks: sortedNetworks,
                featured: featuredSet.contains(coinId) || networks.contains { featuredSet.contains($0.ticker) }
            )
        }
        if query.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            let featured = groups.filter(\.featured).sorted { $0.name < $1.name }
            return featured.isEmpty ? Array(groups.sorted { $0.name < $1.name }.prefix(20)) : featured
        }
        let needle = query.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        return groups.filter { group in
            group.name.lowercased().contains(needle)
                || group.id.contains(needle)
                || group.networks.contains {
                    $0.ticker.contains(needle) || $0.networkLabel.lowercased().contains(needle)
                }
        }.sorted { $0.name < $1.name }
    }

    static func coinId(_ ticker: String) -> String {
        let normalized = ticker.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        for (suffix, _) in networkSuffixes where normalized.hasSuffix(suffix) && normalized.count > suffix.count {
            return String(normalized.dropLast(suffix.count))
        }
        return normalized
    }

    static func networkLabel(_ ticker: String, meta: NowFullCurrency?) -> String {
        if let network = meta?.network.trimmingCharacters(in: .whitespacesAndNewlines), !network.isEmpty {
            return formatNetworkName(network)
        }
        if let blockchain = meta?.blockchain.trimmingCharacters(in: .whitespacesAndNewlines), !blockchain.isEmpty {
            return formatNetworkName(blockchain)
        }
        let normalized = ticker.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        for (suffix, label) in networkSuffixes where normalized.hasSuffix(suffix) && normalized.count > suffix.count {
            return label
        }
        return "Mainnet"
    }

    static func coinName(_ coinId: String, meta: NowFullCurrency?) -> String {
        if let name = meta?.name.trimmingCharacters(in: .whitespacesAndNewlines), !name.isEmpty {
            return name
        }
        return CryptoCurrencyCatalog.label(coinId)
    }

    static func iconTicker(_ coinId: String, fallbackTicker: String) -> String {
        (coinId.isEmpty ? fallbackTicker : coinId).lowercased()
    }

    private static func formatNetworkName(_ raw: String) -> String {
        switch raw.trimmingCharacters(in: .whitespacesAndNewlines).lowercased() {
        case "eth", "ethereum": return "Ethereum"
        case "trx", "tron": return "TRON"
        case "bsc", "bnb": return "BSC"
        case "matic", "polygon": return "Polygon"
        case "sol", "solana": return "Solana"
        case "btc", "bitcoin": return "Bitcoin"
        case "arb", "arbitrum": return "Arbitrum"
        case "op", "optimism": return "Optimism"
        case "base": return "Base"
        case "avax", "avalanche": return "Avalanche"
        default:
            return raw.prefix(1).uppercased() + raw.dropFirst()
        }
    }
}
