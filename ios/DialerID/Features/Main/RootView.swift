import SwiftUI

enum AppTab: String, CaseIterable, Identifiable {
    case favorites, recents, contacts, dialer
    var id: String { rawValue }

    var title: String {
        switch self {
        case .favorites: return "Favorites"
        case .recents: return "Recents"
        case .contacts: return "Contacts"
        case .dialer: return "Dialer"
        }
    }

    var symbol: String {
        switch self {
        case .favorites: return "star.fill"
        case .recents: return "clock.fill"
        case .contacts: return "person.2.fill"
        case .dialer: return "circle.grid.3x3.fill"
        }
    }
}

enum OverlayDest {
    case none, identity, credit, rates, account
}

@MainActor
final class AuthViewModel: ObservableObject {
    @Published var isAuthenticated = false
    @Published var email = ""
    @Published var password = ""
    @Published var name = ""
    @Published var isCreating = false
    @Published var isWorking = false
    @Published var errorMessage: String?

    private let auth = AuthService()

    func appear() {
        auth.refresh()
        isAuthenticated = auth.isAuthenticated
        if let uid = auth.uid {
            Task { await DialerRepository.shared.bindUser(uid: uid, displayName: "", email: "", isNewUser: false) }
        }
    }

    func submit() {
        Task { await performSubmit() }
    }

    func google(from presenter: UIViewController?) {
        Task {
            isWorking = true
            defer { isWorking = false }
            do {
                let result = try await auth.signInWithGoogle(from: presenter)
                await DialerRepository.shared.bindUser(
                    uid: result.uid,
                    displayName: result.displayName,
                    email: result.email,
                    isNewUser: result.isNewUser
                )
                isAuthenticated = true
            } catch {
                errorMessage = error.localizedDescription
            }
        }
    }

    func signOut() {
        auth.signOut()
        DialerRepository.shared.unbindUser()
        isAuthenticated = false
    }

    private func performSubmit() async {
        isWorking = true
        defer { isWorking = false }
        do {
            let result: AuthSignInResult
            if isCreating {
                result = try await auth.createAccount(name: name, email: email, password: password)
            } else {
                result = try await auth.signIn(email: email, password: password)
            }
            await DialerRepository.shared.bindUser(
                uid: result.uid,
                displayName: result.displayName.isEmpty ? name : result.displayName,
                email: result.email,
                isNewUser: result.isNewUser || isCreating
            )
            isAuthenticated = true
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}

struct RootView: View {
    @EnvironmentObject private var session: AppSession
    @EnvironmentObject private var repository: DialerRepository
    @EnvironmentObject private var callManager: CallManager
    @StateObject private var auth = AuthViewModel()
    @StateObject private var deviceAccess = DeviceAccessViewModel()
    @State private var tab = AppTab.dialer
    @State private var overlay = OverlayDest.none

    var body: some View {
        Group {
            if !auth.isAuthenticated {
                AuthScreen(viewModel: auth)
            } else if deviceAccess.isChecking {
                ProgressView("Checking device access…")
                    .tint(DialerIDColor.sky700)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                    .background(DialerIDColor.canvas)
            } else if !deviceAccess.isUnlocked && !repository.userProfile.isGuest {
                DevicePaywallScreen(viewModel: deviceAccess, onSignOut: auth.signOut)
            } else if callManager.callState.isCallActive {
                ActiveCallScreen()
            } else {
                MainShell(tab: $tab, overlay: $overlay)
            }
        }
        .onAppear {
            auth.appear()
            deviceAccess.refresh()
        }
        .onChange(of: auth.isAuthenticated) { _ in
            deviceAccess.refresh()
        }
        .onChange(of: session.incomingDialNumber) { number in
            if number != nil {
                tab = .dialer
                overlay = .none
            }
        }
        .onReceive(NotificationCenter.default.publisher(for: .dialerSignOut)) { _ in
            auth.signOut()
        }
    }
}

struct MainShell: View {
    @Binding var tab: AppTab
    @Binding var overlay: OverlayDest
    @EnvironmentObject private var repository: DialerRepository
    @EnvironmentObject private var sip: SipEngine
    @EnvironmentObject private var contacts: ContactsStore

    var title: String {
        switch overlay {
        case .identity: return "Showing as"
        case .credit: return "Credit"
        case .rates: return "Call rates"
        case .account: return "Account"
        case .none: return tab.title == "Dialer" ? "DialerID" : tab.title
        }
    }

    var body: some View {
        NavigationStack {
            Group {
                switch overlay {
                case .identity: CallerIdScreen()
                case .credit: DepositScreen()
                case .rates: RatesScreen()
                case .account: SettingsScreen()
                case .none:
                    switch tab {
                    case .favorites: FavoritesScreen()
                    case .recents: RecentsScreen()
                    case .contacts: ContactsScreen()
                    case .dialer: DialerScreen()
                    }
                }
            }
            .navigationTitle(title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                if overlay != .none {
                    ToolbarItem(placement: .navigationBarLeading) {
                        Button {
                            overlay = .none
                        } label: {
                            Image(systemName: "chevron.left")
                        }
                        .accessibilityLabel("Back")
                    }
                } else {
                    ToolbarItem(placement: .navigationBarTrailing) {
                        Button {
                            tab = .contacts
                        } label: {
                            Image(systemName: "magnifyingglass")
                        }
                        .accessibilityLabel("Search")
                    }
                    ToolbarItem(placement: .navigationBarTrailing) {
                        Menu {
                            Button("Showing as") { overlay = .identity }
                            Button("Credit") { overlay = .credit }
                            Button("Call rates") { overlay = .rates }
                            Button("Account") { overlay = .account }
                        } label: {
                            Image(systemName: "ellipsis.circle")
                        }
                        .accessibilityLabel("More options")
                    }
                }
            }
            .safeAreaInset(edge: .bottom) {
                if overlay == .none {
                    tabBar
                }
            }
        }
        .onAppear {
            registerSipIfPossible()
            contacts.reload()
        }
        .onChange(of: repository.userProfile.sipConfig) { _ in
            registerSipIfPossible()
        }
    }

    private func registerSipIfPossible() {
        guard !repository.userProfile.isGuest, let sipConfig = repository.resolvedSipConfig() else { return }
        sip.register(sipConfig: sipConfig)
    }

    private var tabBar: some View {
        HStack {
            ForEach(AppTab.allCases) { item in
                Button {
                    tab = item
                } label: {
                    VStack(spacing: 4) {
                        Image(systemName: item.symbol)
                        Text(item.title)
                            .font(.caption2)
                    }
                    .foregroundStyle(tab == item ? DialerIDColor.sky700 : DialerIDColor.inkMuted)
                    .frame(maxWidth: .infinity)
                }
                .accessibilityLabel(item.title)
            }
        }
        .padding(.vertical, 8)
        .background(DialerIDColor.canvas)
        .overlay(alignment: .top) {
            Divider()
        }
    }
}

extension View {
    func presenterController() -> UIViewController? {
        UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap(\.windows)
            .first { $0.isKeyWindow }?
            .rootViewController
    }
}
