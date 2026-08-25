import Foundation

struct SheetRowKey: Hashable {
    let prefix: String
    let name: String
    let wholesaleRatePerMin: Double
}

final class CallRateCatalog: @unchecked Sendable {
    static let assetName = "rates.json"
    static let fallbackAssetName = "all_rates.json"
    private static let nanpCountryCode = "1"
    private static let unitedStatesName = "United States"

    private let ratesByPrefix: [String: CallRate]
    private let sheetRates: [CallRate]

    var listedRates: [ListedCallRate] {
        Dictionary(grouping: sheetRates, by: \.name)
            .map { name, group in
                ListedCallRate(
                    destination: name,
                    markedUpRatePerMin: group.map(\.markedUpRatePerMin).max() ?? 0
                )
            }
            .sorted { $0.destination.lowercased() < $1.destination.lowercased() }
    }

    var sheetRowCount: Int { sheetRates.count }

    var countryCallingCodes: Set<String> {
        var codes = Set<String>()
        for prefix in ratesByPrefix.keys {
            if let code = Self.countryCallingCode(prefix) {
                codes.insert(code)
            }
        }
        return codes
    }

    init(ratesByPrefix: [String: CallRate], sheetRates: [CallRate]) {
        self.ratesByPrefix = ratesByPrefix
        self.sheetRates = sheetRates
    }

    func lookup(_ number: String) -> CallRate? {
        let digits = Self.normalizeDigits(number)
        if digits.isEmpty { return nil }
        if let match = lookupDigits(digits) { return match }
        if digits.count == 10, let first = digits.first, ("2"..."9").contains(first) {
            return lookupDigits("1" + digits)
        }
        return nil
    }

    func isPrefixIndexed(_ prefix: String) -> Bool {
        let digits = Self.normalizeDigits(prefix)
        return !digits.isEmpty && ratesByPrefix[digits] != nil
    }

    private func lookupDigits(_ digits: String) -> CallRate? {
        for len in stride(from: digits.count, through: 1, by: -1) {
            let key = String(digits.prefix(len))
            if let match = ratesByPrefix[key] { return match }
        }
        return nil
    }

    static func fromRates(_ rates: [CallRate]) -> CallRateCatalog {
        var map: [String: CallRate] = [:]
        var sheet: [CallRate] = []
        var seenRows = Set<SheetRowKey>()
        for rate in rates {
            let prefix = normalizeDigits(rate.prefix)
            if prefix.isEmpty { continue }
            let stored = rate.prefix == prefix ? rate : CallRate(
                name: rate.name,
                prefix: prefix,
                wholesaleRatePerMin: rate.wholesaleRatePerMin
            )
            let key = SheetRowKey(
                prefix: stored.prefix,
                name: stored.name,
                wholesaleRatePerMin: stored.wholesaleRatePerMin
            )
            if seenRows.insert(key).inserted {
                sheet.append(stored)
            }
            if map[prefix] == nil {
                map[prefix] = stored
            }
        }
        addCountryCodeFallbacks(map: &map, rates: sheet)
        return CallRateCatalog(ratesByPrefix: map, sheetRates: sheet)
    }

    static func fromJSON(_ json: String) -> CallRateCatalog {
        let rates = parseWholesaleRows(json).compactMap { row -> CallRate? in
            let prefix = normalizeDigits(row.prefix)
            if prefix.isEmpty || row.rate < 0 { return nil }
            return CallRate(
                name: row.name.isEmpty ? prefix : row.name,
                prefix: prefix,
                wholesaleRatePerMin: row.rate
            )
        }
        return fromRates(rates)
    }

    static func fromBundle(_ bundle: Bundle = .main) -> CallRateCatalog {
        if let url = bundle.url(forResource: "rates", withExtension: "json")
            ?? bundle.url(forResource: "all_rates", withExtension: "json"),
           let json = try? String(contentsOf: url, encoding: .utf8) {
            return fromJSON(json)
        }
        return fromRates([])
    }

    static func normalizeDigits(_ number: String) -> String {
        let digits = number.filter(\.isNumber)
        if digits.hasPrefix("00"), digits.count > 2 {
            return String(digits.dropFirst(2))
        }
        return digits
    }

    static func countryCallingCode(_ prefix: String) -> String? {
        E164.callingCodeForDigits(normalizeDigits(prefix))
    }

    private static func addCountryCodeFallbacks(map: inout [String: CallRate], rates: [CallRate]) {
        var byCode: [String: [CallRate]] = [:]
        for rate in rates {
            guard let code = countryCallingCode(rate.prefix), code != rate.prefix else { continue }
            byCode[code, default: []].append(rate)
        }
        for (code, children) in byCode {
            if map[code] != nil || children.isEmpty { continue }
            let chosen: CallRate
            if code == nanpCountryCode {
                chosen = children.first { $0.name.caseInsensitiveCompare(unitedStatesName) == .orderedSame }
                    ?? children[0]
            } else if let shortest = children.min(by: { $0.prefix.count < $1.prefix.count }) {
                chosen = shortest
            } else {
                continue
            }
            map[code] = CallRate(name: chosen.name, prefix: code, wholesaleRatePerMin: chosen.wholesaleRatePerMin)
        }
    }

    private struct WholesaleRateRow {
        var name: String
        var prefix: String
        var rate: Double
    }

    private static func parseWholesaleRows(_ json: String) -> [WholesaleRateRow] {
        guard let data = json.data(using: .utf8),
              let array = try? JSONSerialization.jsonObject(with: data) as? [[String: Any]] else {
            return []
        }
        return array.map { object in
            let name = object["NAME"] as? String ?? ""
            let prefix: String
            if let value = object["PREFIX"] as? String {
                prefix = value
            } else if let value = object["PREFIX"] as? NSNumber {
                prefix = value.stringValue
            } else {
                prefix = ""
            }
            let rate: Double
            if let value = object["RATE"] as? NSNumber {
                rate = value.doubleValue
            } else if let value = object["RATE"] as? String {
                rate = Double(value) ?? 0
            } else {
                rate = 0
            }
            return WholesaleRateRow(name: name, prefix: prefix, rate: rate)
        }
    }
}
