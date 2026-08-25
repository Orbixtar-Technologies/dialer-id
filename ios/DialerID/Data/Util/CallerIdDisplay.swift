import Foundation

enum CallerIdDisplay {
    static func publicIdentity(_ raw: String) -> String {
        var value = raw.trimmingCharacters(in: .whitespacesAndNewlines)
        if value.isEmpty { return "" }
        if value.hasPrefix("<") { value.removeFirst() }
        if value.hasSuffix(">") { value.removeLast() }
        if value.lowercased().hasPrefix("sip:") {
            value = String(value.dropFirst(4))
        }
        value = value.split(separator: ";").first.map(String.init) ?? value
        value = value.split(separator: "?").first.map(String.init) ?? value
        value = value.trimmingCharacters(in: .whitespacesAndNewlines)
        if isProviderHost(value) { return "" }
        if let at = value.firstIndex(of: "@") {
            value = String(value[..<at]).trimmingCharacters(in: .whitespacesAndNewlines)
        }
        if isProviderHost(value) || looksLikeProviderLabel(value) { return "" }
        return value
    }

    static func publicLabel(_ label: String, fallbackNumber: String = "") -> String {
        let trimmed = label.trimmingCharacters(in: .whitespacesAndNewlines)
        if trimmed.isEmpty || looksLikeProviderLabel(trimmed) || trimmed.contains("@") {
            return publicIdentity(fallbackNumber)
        }
        return trimmed
    }

    static func title(_ item: CallerIdItem) -> String {
        let label = publicLabel(item.label)
        return label.isEmpty ? number(item) : label
    }

    static func number(_ item: CallerIdItem) -> String {
        let fromPhone = publicIdentity(item.phoneNumber)
        return fromPhone.isEmpty ? publicIdentity(item.username) : fromPhone
    }

    static func subtitle(_ item: CallerIdItem) -> String {
        let shownNumber = number(item)
        let label = publicLabel(item.label)
        if label.isEmpty || label.caseInsensitiveCompare(shownNumber) == .orderedSame {
            return ""
        }
        return label
    }

    static func redactUserText(_ text: String) -> String {
        var result = text.replacingOccurrences(
            of: #"(?i)sip:[^\s<>]+"#,
            with: "",
            options: .regularExpression
        )
        result = result.replacingOccurrences(
            of: #"(?i)(?:[\w.+-]+@)?(?:sip\.)?sipup\.org\b"#,
            with: "",
            options: .regularExpression
        )
        result = result.replacingOccurrences(
            of: #"(?i)\bsipup\b"#,
            with: "",
            options: .regularExpression
        )
        return result.replacingOccurrences(of: "\\s{2,}", with: " ", options: .regularExpression)
            .trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private static func isProviderHost(_ value: String) -> Bool {
        let lower = value.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        return lower == "sip.sipup.org" || lower == "sipup.org" || lower == "sipup"
    }

    private static func looksLikeProviderLabel(_ value: String) -> Bool {
        let lower = value.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        if lower.isEmpty { return false }
        return lower.contains("sip.sipup.org")
            || lower.contains("sipup.org")
            || lower.contains("@sipup")
            || lower == "sipup line"
            || lower == "sipup"
    }
}
