import Foundation
import UIKit

#if canImport(FirebaseAuth)
import FirebaseAuth
#endif
#if canImport(GoogleSignIn)
import GoogleSignIn
#endif

struct AuthSignInResult {
    var uid: String
    var email: String
    var displayName: String
    var isNewUser: Bool
}

enum AuthServiceError: Error, LocalizedError {
    case cancelled
    case missingPresenter
    case firebaseUnavailable
    case message(String)

    var errorDescription: String? {
        switch self {
        case .cancelled: return "Sign-in was cancelled."
        case .missingPresenter: return "Couldn't open Google Sign-In."
        case .firebaseUnavailable: return "Firebase is not linked in this build."
        case .message(let text): return text
        }
    }
}

@MainActor
final class AuthService: ObservableObject {
    @Published private(set) var uid: String?
    @Published private(set) var isAuthenticated = false

    init() {
        refresh()
    }

    func refresh() {
        #if canImport(FirebaseAuth)
        let user = Auth.auth().currentUser
        uid = user?.uid
        isAuthenticated = user != nil
        #else
        uid = nil
        isAuthenticated = false
        #endif
    }

    func signIn(email: String, password: String) async throws -> AuthSignInResult {
        #if canImport(FirebaseAuth)
        do {
            let result = try await Auth.auth().signIn(withEmail: email, password: password)
            refresh()
            return AuthSignInResult(
                uid: result.user.uid,
                email: result.user.email ?? email,
                displayName: result.user.displayName ?? "",
                isNewUser: result.additionalUserInfo?.isNewUser ?? false
            )
        } catch {
            throw mappedAuthError(error)
        }
        #else
        throw AuthServiceError.firebaseUnavailable
        #endif
    }

    func createAccount(name: String, email: String, password: String) async throws -> AuthSignInResult {
        #if canImport(FirebaseAuth)
        do {
            let result = try await Auth.auth().createUser(withEmail: email, password: password)
            let change = result.user.createProfileChangeRequest()
            change.displayName = name
            try await change.commitChanges()
            refresh()
            return AuthSignInResult(
                uid: result.user.uid,
                email: result.user.email ?? email,
                displayName: name,
                isNewUser: true
            )
        } catch {
            throw mappedAuthError(error)
        }
        #else
        throw AuthServiceError.firebaseUnavailable
        #endif
    }

    func signInWithGoogle(from presenter: UIViewController?) async throws -> AuthSignInResult {
        #if canImport(FirebaseAuth) && canImport(GoogleSignIn)
        guard let presenter else { throw AuthServiceError.missingPresenter }
        let iosClientID = (Bundle.main.object(forInfoDictionaryKey: "GID_CLIENT_ID") as? String)?
            .trimmingCharacters(in: .whitespacesAndNewlines)
        GIDSignIn.sharedInstance.configuration = GIDConfiguration(
            clientID: (iosClientID?.isEmpty == false ? iosClientID! : AppConfig.googleWebClientID),
            serverClientID: AppConfig.googleWebClientID
        )
        do {
            let google = try await GIDSignIn.sharedInstance.signIn(withPresenting: presenter)
            guard let idToken = google.user.idToken?.tokenString else {
                throw AuthServiceError.message("Google Sign-In did not return an ID token.")
            }
            let credential = GoogleAuthProvider.credential(
                withIDToken: idToken,
                accessToken: google.user.accessToken.tokenString
            )
            let result = try await Auth.auth().signIn(with: credential)
            refresh()
            return AuthSignInResult(
                uid: result.user.uid,
                email: result.user.email ?? google.user.profile?.email ?? "",
                displayName: result.user.displayName ?? google.user.profile?.name ?? "",
                isNewUser: result.additionalUserInfo?.isNewUser ?? false
            )
        } catch {
            if (error as NSError).code == GIDSignInError.canceled.rawValue {
                throw AuthServiceError.cancelled
            }
            throw mappedAuthError(error)
        }
        #else
        throw AuthServiceError.firebaseUnavailable
        #endif
    }

    func signOut() {
        #if canImport(FirebaseAuth)
        try? Auth.auth().signOut()
        #endif
        #if canImport(GoogleSignIn)
        GIDSignIn.sharedInstance.signOut()
        #endif
        refresh()
    }

    #if canImport(FirebaseAuth)
    private func mappedAuthError(_ error: Error) -> Error {
        let text = error.localizedDescription.lowercased()
        if text.contains("keychain") {
            return AuthServiceError.message(
                "Could not save the sign-in session on this simulator. Close the Appetize tab and try again, or sign in on a physical iPhone."
            )
        }
        return error
    }
    #endif
}
