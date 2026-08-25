import SwiftUI

struct ContactsScreen: View {
    @EnvironmentObject private var contacts: ContactsStore
    @EnvironmentObject private var repository: DialerRepository
    @EnvironmentObject private var sip: SipEngine
    @EnvironmentObject private var calls: CallManager

    var body: some View {
        Group {
            switch contacts.authorization {
            case .notDetermined:
                permissionCard(
                    title: "Contacts access needed",
                    body: "Allow DialerID to read this device address book so you can dial people by name.",
                    actionTitle: "Allow access",
                    action: contacts.requestAccess
                )
            case .denied, .restricted:
                permissionCard(
                    title: "Contacts access is off",
                    body: "Turn contacts access back on in app settings to dial people by name.",
                    actionTitle: "Open settings",
                    action: openSettings
                )
            default:
                list
            }
        }
        .onAppear { contacts.reload() }
    }

    private var list: some View {
        List {
            if contacts.filtered.isEmpty {
                EmptyState(
                    title: contacts.search.isEmpty ? "No contacts on this device" : "No matches",
                    message: contacts.search.isEmpty
                        ? "Contacts you save on this device will appear here."
                        : "Nothing in the address book matches this search."
                )
            } else {
                ForEach(contacts.filtered) { contact in
                    Button {
                        placeCall(contact.primaryNumber)
                    } label: {
                        VStack(alignment: .leading) {
                            Text(contact.name)
                            Text(contact.primaryNumber)
                                .font(.footnote)
                                .foregroundStyle(DialerIDColor.inkMuted)
                        }
                    }
                }
            }
        }
        .searchable(text: $contacts.search, prompt: "Search name or number")
    }

    private func permissionCard(title: String, body: String, actionTitle: String, action: @escaping () -> Void) -> some View {
        VStack(spacing: 16) {
            Text(title).font(.title2.bold())
            Text(body).foregroundStyle(DialerIDColor.inkMuted).multilineTextAlignment(.center)
            Button(actionTitle, action: action).buttonStyle(PrimaryButtonStyle())
        }
        .padding(24)
    }

    private func openSettings() {
        if let url = URL(string: UIApplication.openSettingsURLString) {
            UIApplication.shared.open(url)
        }
    }

    private func placeCall(_ number: String) {
        let e164 = E164.format(number, defaultRegion: E164.defaultRegion())
        if case .allowed(let rate) = OutboundBillingPolicy.evaluate(
            destination: e164,
            balanceUsd: repository.userProfile.creditBalance,
            catalog: repository.rateCatalog
        ), sip.isRegistered {
            _ = calls.startCall(
                destinationNumber: e164,
                callerId: repository.userProfile.selectedCallerId,
                countryName: rate.name,
                rate: rate.markedUpRatePerMin
            )
        }
    }
}

struct FavoritesScreen: View {
    @EnvironmentObject private var contacts: ContactsStore
    @EnvironmentObject private var repository: DialerRepository
    @EnvironmentObject private var calls: CallManager

    var body: some View {
        let frequent = Dictionary(grouping: repository.callLogs, by: \.destinationNumber)
            .map { number, logs in (number, logs.count) }
            .sorted { $0.1 > $1.1 }
            .prefix(8)
        List {
            if contacts.favorites.isEmpty && frequent.isEmpty {
                EmptyState(
                    title: "No favorites yet",
                    message: "Star people in this device address book to pin them here. Frequent numbers you call will also appear."
                )
            }
            if !contacts.favorites.isEmpty {
                Section("Starred") {
                    ForEach(contacts.favorites) { contact in
                        Text(contact.name)
                    }
                }
            }
            if !frequent.isEmpty {
                Section("Frequent") {
                    ForEach(Array(frequent), id: \.0) { number, count in
                        Button {
                            let e164 = E164.format(number, defaultRegion: E164.defaultRegion())
                            if case .allowed(let rate) = OutboundBillingPolicy.evaluate(
                                destination: e164,
                                balanceUsd: repository.userProfile.creditBalance,
                                catalog: repository.rateCatalog
                            ) {
                                _ = calls.startCall(
                                    destinationNumber: e164,
                                    callerId: repository.userProfile.selectedCallerId,
                                    countryName: rate.name,
                                    rate: rate.markedUpRatePerMin
                                )
                            }
                        } label: {
                            HStack {
                                Text(number)
                                Spacer()
                                Text("\(count)×").foregroundStyle(DialerIDColor.inkMuted)
                            }
                        }
                    }
                }
            }
        }
        .onAppear { contacts.reload() }
    }
}

struct RecentsScreen: View {
    @EnvironmentObject private var repository: DialerRepository
    @EnvironmentObject private var calls: CallManager
    @State private var query = ""
    @State private var confirmClear = false

    var filtered: [CallLogItem] {
        if query.isEmpty { return repository.callLogs }
        let needle = query.lowercased()
        return repository.callLogs.filter {
            $0.destinationNumber.contains(needle) || $0.countryName.lowercased().contains(needle)
        }
    }

    var body: some View {
        List {
            if filtered.isEmpty {
                EmptyState(
                    title: repository.callLogs.isEmpty ? "No calls yet" : "No matching calls",
                    message: repository.callLogs.isEmpty
                        ? "Calls you place will be listed here with duration and charge."
                        : "Nothing matches this search or filter."
                )
            } else {
                ForEach(filtered) { item in
                    Button {
                        if case .allowed(let rate) = OutboundBillingPolicy.evaluate(
                            destination: item.destinationNumber,
                            balanceUsd: repository.userProfile.creditBalance,
                            catalog: repository.rateCatalog
                        ) {
                            _ = calls.startCall(
                                destinationNumber: item.destinationNumber,
                                callerId: repository.userProfile.selectedCallerId,
                                countryName: rate.name,
                                rate: rate.markedUpRatePerMin
                            )
                        }
                    } label: {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(item.destinationNumber).font(.headline)
                            Text("\(item.countryName) • \(item.status.rawValue.capitalized)")
                                .font(.footnote)
                                .foregroundStyle(DialerIDColor.inkMuted)
                            HStack {
                                Text(item.durationSeconds > 0
                                    ? String(format: "%02d:%02d", item.durationSeconds / 60, item.durationSeconds % 60)
                                    : "Free")
                                Spacer()
                                Text(item.totalCost > 0 ? String(format: "$%.4f", item.totalCost) : "Free")
                            }
                            .font(.caption)
                        }
                    }
                }
            }
        }
        .searchable(text: $query, prompt: "Search by number or country")
        .toolbar {
            if !repository.callLogs.isEmpty {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Clear") { confirmClear = true }
                }
            }
        }
        .confirmationDialog("Clear call history?", isPresented: $confirmClear) {
            Button("Clear all", role: .destructive) { repository.clearCallLogs() }
        } message: {
            Text("All outbound call records on this device will be permanently deleted.")
        }
    }
}
