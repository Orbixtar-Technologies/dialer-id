import Foundation

enum E164 {
    static let fallbackRegion = "US"

    private static let likelyTrunkZeroRegions = [
        "PK", "GB", "IN", "BD", "AE", "SA", "EG", "NG", "KE", "ZA",
        "DE", "FR", "IT", "ES", "NL", "BE", "AT", "CH", "IE", "PT",
        "PL", "AU", "NZ", "MY", "SG", "ID", "PH", "TH", "VN", "JP",
        "KR", "CN", "TR", "RU", "UA", "IQ", "IR", "AF", "LK", "NP",
        "QA", "KW", "OM", "BH", "JO", "LB", "MA", "TN", "DZ", "GH",
        "TZ", "UG", "ET", "CM", "AR", "BR", "CL", "CO", "MX", "FI",
        "SE", "NO", "DK", "CZ", "HU", "RO", "GR", "IL"
    ]

    static func defaultRegion() -> String {
        if let region = Locale.current.region?.identifier.uppercased(), region.count == 2 {
            return region
        }
        return fallbackRegion
    }

    static func format(
        _ raw: String,
        defaultRegion: String,
        candidateRegions: [String] = []
    ) -> String {
        guard let sanitized = PhoneNumberSanitizer.sanitizeDestination(raw) else {
            return raw.trimmingCharacters(in: .whitespacesAndNewlines)
        }
        let digits = String(sanitized.drop(while: { $0 == "+" }).filter(\.isNumber))
        if isShortCode(compact: sanitized, digits: digits) {
            return sanitized.hasPrefix("+") ? digits : sanitized
        }

        let region = defaultRegion.trimmingCharacters(in: .whitespacesAndNewlines)
            .uppercased()
        let resolvedRegion = region.isEmpty ? fallbackRegion : region
        let prepared = prepareForParse(sanitized)

        if prepared.hasPrefix("+") {
            if let parsed = parsePossible(prepared, region: resolvedRegion) {
                return parsed
            }
            return fallbackE164(prepared)
        }

        if isTrunkNational(prepared) {
            var validByRegion: [String: String] = [:]
            for candidate in regionCandidates(resolvedRegion, extra: candidateRegions) {
                if validByRegion[candidate] != nil { continue }
                if let e164 = parseValidForRegion(prepared, region: candidate) {
                    validByRegion[candidate] = e164
                }
            }
            if let picked = pickValidE164(defaultRegion: resolvedRegion, validByRegion: validByRegion) {
                return picked
            }
        } else {
            if let parsed = parseValidForRegion(prepared, region: resolvedRegion) {
                return parsed
            }
            if isNanpNational(prepared) {
                if let parsed = parseValidForRegion(prepared, region: "US") { return parsed }
                if let parsed = parseValidForRegion(prepared, region: "CA") { return parsed }
                if let parsed = parsePossible(prepared, region: resolvedRegion) { return parsed }
                return fallbackE164(prepared)
            }
            for candidate in nonTrunkCandidates(resolvedRegion, extra: candidateRegions) {
                if let parsed = parseValidForRegion(prepared, region: candidate) {
                    return parsed
                }
            }
        }
        if let parsed = parsePossible(prepared, region: resolvedRegion) {
            return parsed
        }
        return fallbackE164(prepared)
    }

    static func regionsFromCallingCodes(_ callingCodes: [String]) -> [String] {
        var regions: [String] = []
        var seen = Set<String>()
        for raw in callingCodes {
            guard let code = Int(raw.trimmingCharacters(in: .whitespacesAndNewlines)) else { continue }
            for region in PhoneNumberMetadata.regions(forCallingCode: code) {
                let iso = region.uppercased()
                guard iso.count == 2, iso != "ZZ", !seen.contains(iso) else { continue }
                seen.insert(iso)
                regions.append(iso)
            }
        }
        return regions
    }

    static func callingCodeForDigits(_ digits: String) -> String? {
        var body = digits.filter(\.isNumber)
        if body.hasPrefix("00"), body.count > 2 {
            body = String(body.dropFirst(2))
        }
        if body.isEmpty { return nil }
        let maxLen = min(3, body.count)
        for len in stride(from: maxLen, through: 1, by: -1) {
            let candidate = String(body.prefix(len))
            if PhoneNumberMetadata.supportedCallingCodes.contains(candidate) {
                return candidate
            }
        }
        return nil
    }

    private static func isShortCode(compact: String, digits: String) -> Bool {
        !compact.hasPrefix("+") && (1...4).contains(digits.count)
    }

    private static func prepareForParse(_ compact: String) -> String {
        if compact.hasPrefix("00"), compact.count > 2 {
            return "+" + compact.dropFirst(2)
        }
        return compact
    }

    private static func isTrunkNational(_ prepared: String) -> Bool {
        prepared.hasPrefix("0") && !prepared.hasPrefix("00") && prepared.count > 1
    }

    private static func isNanpNational(_ prepared: String) -> Bool {
        let digits = prepared.filter(\.isNumber)
        return digits.count == 10 && ("2"..."9").contains(digits.first!)
    }

    private static func nonTrunkCandidates(_ defaultRegion: String, extra: [String]) -> [String] {
        var ordered: [String] = []
        var seen = Set<String>()
        func add(_ raw: String) {
            let region = raw.trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
            guard region.count == 2, !seen.contains(region) else { return }
            seen.insert(region)
            ordered.append(region)
        }
        add(defaultRegion)
        extra.forEach(add)
        return ordered
    }

    private static func regionCandidates(_ defaultRegion: String, extra: [String]) -> [String] {
        var ordered: [String] = []
        var seen = Set<String>()
        func add(_ raw: String) {
            let region = raw.trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
            guard region.count == 2, !seen.contains(region) else { return }
            seen.insert(region)
            ordered.append(region)
        }
        add(defaultRegion)
        extra.forEach(add)
        PhoneNumberMetadata.trunkZeroRegions.forEach(add)
        return ordered
    }

    private static func pickValidE164(defaultRegion: String, validByRegion: [String: String]) -> String? {
        if validByRegion.isEmpty { return nil }
        if let preferred = validByRegion[defaultRegion] { return preferred }
        if validByRegion.count == 1 { return validByRegion.values.first }
        let rank = Dictionary(uniqueKeysWithValues: likelyTrunkZeroRegions.enumerated().map { ($0.element, $0.offset) })
        return validByRegion.min { lhs, rhs in
            let left = rank[lhs.key] ?? Int.max
            let right = rank[rhs.key] ?? Int.max
            if left != right { return left < right }
            return lhs.key < rhs.key
        }?.value
    }

    private static func parsePossible(_ prepared: String, region: String) -> String? {
        PhoneNumberMetadata.parse(prepared, region: region, requireValid: false)
    }

    private static func parseValidForRegion(_ prepared: String, region: String) -> String? {
        PhoneNumberMetadata.parse(prepared, region: region, requireValid: true)
    }

    private static func fallbackE164(_ prepared: String) -> String {
        if prepared.hasPrefix("+"), prepared.count > 2 { return prepared }
        let body = String(prepared.drop(while: { $0 == "+" }).filter(\.isNumber))
        if body.count == 10, let first = body.first, ("2"..."9").contains(first) {
            return "+1" + body
        }
        return body.isEmpty ? prepared : "+" + body
    }
}
