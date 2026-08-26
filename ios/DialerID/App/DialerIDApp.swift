import SwiftUI
import UIKit
#if canImport(FirebaseAuth)
import FirebaseAuth
#endif
#if canImport(FirebaseCore)
import FirebaseCore
#endif
#if canImport(GoogleSignIn)
import GoogleSignIn
#endif

@main
struct DialerIDApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate
    @StateObject private var session = AppSession()

    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(session)
                .environmentObject(DialerRepository.shared)
                .environmentObject(SipEngine.shared)
                .environmentObject(CallManager.shared)
                .environmentObject(ContactsStore.shared)
                .preferredColorScheme(.light)
                .onOpenURL { url in
                    #if canImport(GoogleSignIn)
                    GIDSignIn.sharedInstance.handle(url)
                    #endif
                    session.handle(url: url)
                }
        }
    }
}

final class AppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        #if canImport(FirebaseCore)
        if FirebaseApp.app() == nil {
            FirebaseApp.configure()
        }
        #endif
        #if canImport(FirebaseAuth)
        configureFirebaseAuthKeychain()
        #endif
        return true
    }

    func application(
        _ app: UIApplication,
        open url: URL,
        options: [UIApplication.OpenURLOptionsKey: Any] = [:]
    ) -> Bool {
        #if canImport(GoogleSignIn)
        return GIDSignIn.sharedInstance.handle(url)
        #else
        return false
        #endif
    }
}

@MainActor
final class AppSession: ObservableObject {
    @Published var incomingDialNumber: String?

    func handle(url: URL) {
        guard url.scheme == "tel" else { return }
        let number = url.absoluteString.replacingOccurrences(of: "tel:", with: "")
        if let sanitized = PhoneNumberSanitizer.sanitizeDestination(number.removingPercentEncoding ?? number) {
            incomingDialNumber = sanitized
        }
    }
}

#if canImport(FirebaseAuth)
private func configureFirebaseAuthKeychain() {
    let candidates = [
        Bundle.main.bundleIdentifier,
        "com.dialerid.app"
    ].compactMap { $0 }
    for group in candidates {
        do {
            try Auth.auth().useUserAccessGroup(group)
            return
        } catch {
            continue
        }
    }
}
#endif
