import Foundation

/// Compact ITU metadata used by `E164` so iOS matches the Android
/// libphonenumber parse order without embedding the full Google dataset.
enum PhoneNumberMetadata {
    static let supportedCallingCodes: Set<String> = {
        var codes = Set(callingCodeByRegion.values.map(String.init))
        [
            "800", "808", "870", "878", "881", "882", "883", "888", "979"
        ].forEach { codes.insert($0) }
        return codes
    }()

    static let trunkZeroRegions: [String] = {
        let rank = Dictionary(uniqueKeysWithValues: likelyTrunkZero.enumerated().map { ($0.element, $0.offset) })
        return callingCodeByRegion.keys
            .filter { nddPrefix[$0] == "0" }
            .sorted {
                let left = rank[$0] ?? Int.max
                let right = rank[$1] ?? Int.max
                if left != right { return left < right }
                return $0 < $1
            }
    }()

    static func regions(forCallingCode code: Int) -> [String] {
        let primary = regionByCallingCode[code] ?? []
        return primary
    }

    static func parse(_ prepared: String, region: String, requireValid: Bool) -> String? {
        let iso = region.uppercased()
        if prepared.hasPrefix("+") {
            let digits = String(prepared.dropFirst().filter(\.isNumber))
            guard let code = E164.callingCodeForDigits(digits) else { return nil }
            let national = String(digits.dropFirst(code.count))
            if requireValid {
                let matchedRegion = regionByCallingCode[Int(code) ?? -1]?.first ?? iso
                guard isValidNational(national, region: matchedRegion) || isValidNational(national, region: iso) else {
                    return nil
                }
            }
            return digits.isEmpty ? nil : "+" + digits
        }

        var national = prepared.filter(\.isNumber)
        if national.hasPrefix("0"), national.count > 1, nddPrefix[iso] == "0" {
            national = String(national.dropFirst())
        }
        guard let callingCode = callingCodeByRegion[iso] else { return nil }
        if requireValid, !isValidNational(national, region: iso) {
            return nil
        }
        if !requireValid, national.isEmpty { return nil }
        return "+" + String(callingCode) + national
    }

    private static func isValidNational(_ national: String, region: String) -> Bool {
        guard let rule = validity[region] else {
            return (6...15).contains(national.count)
        }
        return rule.contains(national.count) && passesLeadingDigit(national, region: region)
    }

    private static func passesLeadingDigit(_ national: String, region: String) -> Bool {
        guard let first = national.first else { return false }
        switch region {
        case "US", "CA":
            return national.count == 10 && ("2"..."9").contains(first)
        case "GB":
            return first == "2" || first == "7" || first == "1" || first == "3" || first == "5"
        case "PK":
            return first == "3" || first == "4" || first == "2" || first == "5"
        case "IN":
            return first == "6" || first == "7" || first == "8" || first == "9"
        case "AU":
            return first == "4" || first == "2" || first == "3" || first == "7" || first == "8"
        case "FI":
            return first == "4" || first == "5" || first == "9"
        default:
            return true
        }
    }

    private static let likelyTrunkZero = [
        "PK", "GB", "IN", "BD", "AE", "SA", "EG", "NG", "KE", "ZA",
        "DE", "FR", "IT", "ES", "NL", "BE", "AT", "CH", "IE", "PT",
        "PL", "AU", "NZ", "MY", "SG", "ID", "PH", "TH", "VN", "JP",
        "KR", "CN", "TR", "RU", "UA", "IQ", "IR", "AF", "LK", "NP",
        "QA", "KW", "OM", "BH", "JO", "LB", "MA", "TN", "DZ", "GH",
        "TZ", "UG", "ET", "CM", "AR", "BR", "CL", "CO", "MX", "FI",
        "SE", "NO", "DK", "CZ", "HU", "RO", "GR", "IL"
    ]

    static let callingCodeByRegion: [String: Int] = [
        "US": 1, "CA": 1, "PR": 1, "DO": 1, "JM": 1, "TT": 1,
        "GB": 44, "IM": 44, "JE": 44, "GG": 44,
        "PK": 92, "IN": 91, "BD": 880, "AE": 971, "SA": 966, "EG": 20,
        "NG": 234, "KE": 254, "ZA": 27, "DE": 49, "FR": 33, "IT": 39,
        "ES": 34, "NL": 31, "BE": 32, "AT": 43, "CH": 41, "IE": 353,
        "PT": 351, "PL": 48, "AU": 61, "NZ": 64, "MY": 60, "SG": 65,
        "ID": 62, "PH": 63, "TH": 66, "VN": 84, "JP": 81, "KR": 82,
        "CN": 86, "TR": 90, "RU": 7, "UA": 380, "IQ": 964, "IR": 98,
        "AF": 93, "LK": 94, "NP": 977, "QA": 974, "KW": 965, "OM": 968,
        "BH": 973, "JO": 962, "LB": 961, "MA": 212, "TN": 216, "DZ": 213,
        "GH": 233, "TZ": 255, "UG": 256, "ET": 251, "CM": 237, "AR": 54,
        "BR": 55, "CL": 56, "CO": 57, "MX": 52, "FI": 358, "SE": 46,
        "NO": 47, "DK": 45, "CZ": 420, "HU": 36, "RO": 40, "GR": 30,
        "IL": 972, "HK": 852, "TW": 886, "MO": 853
    ]

    private static let regionByCallingCode: [Int: [String]] = {
        var map: [Int: [String]] = [:]
        let preferred = ["US", "GB", "PK", "IN"]
        for (region, code) in callingCodeByRegion {
            map[code, default: []].append(region)
        }
        for (code, regions) in map {
            map[code] = regions.sorted { lhs, rhs in
                let left = preferred.firstIndex(of: lhs) ?? Int.max
                let right = preferred.firstIndex(of: rhs) ?? Int.max
                if left != right { return left < right }
                return lhs < rhs
            }
        }
        return map
    }()

    private static let nddPrefix: [String: String] = {
        var map: [String: String] = [:]
        for region in callingCodeByRegion.keys where region != "US" && region != "CA" && region != "SG" {
            map[region] = "0"
        }
        map["US"] = "1"
        map["CA"] = "1"
        return map
    }()

    private static let validity: [String: ClosedRange<Int>] = [
        "US": 10...10,
        "CA": 10...10,
        "GB": 9...10,
        "PK": 10...10,
        "IN": 10...10,
        "AU": 9...9,
        "FI": 9...10,
        "DE": 6...13,
        "FR": 9...9,
        "IT": 6...11,
        "AE": 8...9,
        "SA": 8...9,
        "EG": 8...10,
        "BD": 8...10,
        "NG": 8...10
    ]
}
