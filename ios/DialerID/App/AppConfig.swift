import Foundation

enum AppConfig {
    static let firebaseDatabaseURL = "https://dialerid-default-rtdb.firebaseio.com"
    static let defaultGoogleWebClientID =
        "648026217137-5ej4o7d73l4skvj6osi06ddqprttfs31.apps.googleusercontent.com"

    static var sipUpAPIKey: String {
        string(for: "SIPUP_API_KEY")
    }

    static var nowPaymentsAPIKey: String {
        string(for: "NOW_PAYMENTS_API_KEY")
    }

    static var googleWebClientID: String {
        let configured = string(for: "GOOGLE_WEB_CLIENT_ID")
        return configured.isEmpty ? defaultGoogleWebClientID : configured
    }

    private static func string(for key: String) -> String {
        (Bundle.main.object(forInfoDictionaryKey: key) as? String)?
            .trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
    }
}
