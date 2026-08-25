import Foundation

enum PhoneNumberSanitizer {
    private static let allowed = try! NSRegularExpression(pattern: "^[+]?\\d+$")

    static func sanitizeDestination(_ raw: String) -> String? {
        let compact = raw.trimmingCharacters(in: .whitespacesAndNewlines)
            .filter { !$0.isWhitespace && $0 != "-" && $0 != "(" && $0 != ")" }
        if compact.isEmpty { return nil }
        let range = NSRange(compact.startIndex..., in: compact)
        guard allowed.firstMatch(in: compact, range: range) != nil else { return nil }
        if compact.filter({ $0 == "+" }).count > 1 { return nil }
        if compact.contains("+") && !compact.hasPrefix("+") { return nil }
        let digits = compact.trimmingCharacters(in: CharacterSet(charactersIn: "+"))
        if digits.isEmpty { return nil }
        return compact
    }

    static func filterDialInput(_ raw: String) -> String {
        var builder = ""
        for ch in raw {
            if ch == "+" && builder.isEmpty {
                builder.append(ch)
            } else if ch.isNumber {
                builder.append(ch)
            }
        }
        return builder
    }

    static func isValidCallerId(_ raw: String) -> Bool {
        guard let sanitized = sanitizeDestination(raw) else { return false }
        return sanitized.trimmingCharacters(in: CharacterSet(charactersIn: "+")).count >= 6
    }
}
