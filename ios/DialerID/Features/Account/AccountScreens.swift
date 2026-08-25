import SwiftUI

struct RatesScreen: View {
    @EnvironmentObject private var repository: DialerRepository
    @State private var query = ""

    var filtered: [ListedCallRate] {
        if query.isEmpty { return repository.rateCatalog.listedRates }
        let needle = query.lowercased()
        return repository.rateCatalog.listedRates.filter { $0.destination.lowercased().contains(needle) }
    }

    var body: some View {
        List {
            Text("\(repository.rateCatalog.listedRates.count) destinations, A–Z. Prices in USD per minute.")
                .font(.footnote)
                .foregroundStyle(DialerIDColor.inkMuted)
            if filtered.isEmpty {
                EmptyState(
                    title: repository.rateCatalog.listedRates.isEmpty ? "No rates loaded" : "No matching destinations",
                    message: repository.rateCatalog.listedRates.isEmpty
                        ? "Call rates will appear here when the price list is available."
                        : "Nothing matches this search."
                )
            } else {
                ForEach(filtered) { rate in
                    HStack {
                        Text(rate.destination)
                        Spacer()
                        Text(String(format: "$%.4f /min", rate.markedUpRatePerMin))
                            .foregroundStyle(DialerIDColor.inkMuted)
                    }
                }
            }
        }
        .searchable(text: $query, prompt: "Search destination")
    }
}

struct CallerIdScreen: View {
    @EnvironmentObject private var repository: DialerRepository
    @State private var showAdd = false
    @State private var number = ""
    @State private var label = ""

    var body: some View {
        List {
            Text("The selected number is sent as your identity when you place a call.")
                .font(.footnote)
                .foregroundStyle(DialerIDColor.inkMuted)
            if repository.callerIds.isEmpty {
                EmptyState(
                    title: "No numbers yet",
                    message: "Add a number you control to show it when you call."
                )
            } else {
                ForEach(repository.callerIds) { item in
                    HStack {
                        VStack(alignment: .leading) {
                            Text(CallerIdDisplay.title(item))
                            if !CallerIdDisplay.subtitle(item).isEmpty {
                                Text(CallerIdDisplay.number(item))
                                    .font(.footnote)
                                    .foregroundStyle(DialerIDColor.inkMuted)
                            }
                        }
                        Spacer()
                        if item.isPrimary {
                            Text("Default").font(.caption).foregroundStyle(DialerIDColor.sky700)
                        }
                    }
                    .swipeActions {
                        Button("Use as default") { repository.setPrimaryCallerId(item.id) }
                        Button("Delete", role: .destructive) { repository.deleteCallerId(item.id) }
                    }
                    .onTapGesture { repository.setSelectedCallerId(item.phoneNumber) }
                }
            }
        }
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button("Add number") { showAdd = true }
            }
        }
        .sheet(isPresented: $showAdd) {
            NavigationStack {
                Form {
                    TextField("Phone number", text: $number)
                        .keyboardType(.phonePad)
                    TextField("e.g. Office line", text: $label)
                }
                .navigationTitle("Add a number")
                .toolbar {
                    ToolbarItem(placement: .confirmationAction) {
                        Button("Save number") {
                            if PhoneNumberSanitizer.isValidCallerId(number) {
                                repository.addCallerId(number: number, label: label, makePrimary: repository.callerIds.isEmpty)
                                showAdd = false
                                number = ""
                                label = ""
                            }
                        }
                    }
                    ToolbarItem(placement: .cancellationAction) {
                        Button("Cancel") { showAdd = false }
                    }
                }
            }
        }
    }
}

struct DepositScreen: View {
    @EnvironmentObject private var repository: DialerRepository
    @StateObject private var model = DepositViewModel()

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Text("Your balance").font(.caption).foregroundStyle(DialerIDColor.inkMuted)
                Text(String(format: "$%.2f", repository.userProfile.creditBalance))
                    .font(.largeTitle.bold())
                Text(repository.userProfile.creditBalance > 0 ? "Ready" : "No credit")
                    .foregroundStyle(repository.userProfile.creditBalance > 0
                        ? DialerIDColor.signalGreen600
                        : DialerIDColor.signalRose600)
                Text("Add calling credit to this account. Network and checkout fees are paid by you at checkout.")
                    .foregroundStyle(DialerIDColor.inkMuted)
                TextField("25.00", text: $model.customAmount)
                    .keyboardType(.decimalPad)
                    .textFieldStyle(DialerFieldStyle())
                if model.checkout == nil {
                    Picker("Cryptocurrency", selection: $model.selectedTicker) {
                        ForEach(model.coins) { group in
                            ForEach(group.networks, id: \.ticker) { network in
                                Text("\(group.name) · \(network.networkLabel)").tag(network.ticker)
                            }
                        }
                    }
                    Button("Add $\(model.resolvedAmountText)") { model.startTopUp() }
                        .buttonStyle(PrimaryButtonStyle())
                        .disabled(model.resolvedAmount <= 0 || model.isWorking)
                } else if let payment = model.checkout {
                    CryptoCheckoutPanel(payment: payment, onCheck: model.checkPayment)
                }
                if let error = model.errorMessage {
                    Text(error).foregroundStyle(DialerIDColor.signalRose600).font(.footnote)
                }
            }
            .padding(24)
        }
        .onAppear { model.loadCoins() }
    }
}

@MainActor
final class DepositViewModel: ObservableObject {
    @Published var customAmount = "25.00"
    @Published var selectedTicker = "btc"
    @Published var coins: [CryptoCoinGroup] = []
    @Published var checkout: PendingPayment?
    @Published var errorMessage: String?
    @Published var isWorking = false

    private let payments = NowPaymentsClient()

    var resolvedAmount: Double {
        Double(customAmount) ?? 0
    }

    var resolvedAmountText: String {
        String(format: "%.2f", resolvedAmount)
    }

    func loadCoins() {
        Task {
            if let tickers = try? await payments.listPayCurrencies() {
                let full = (try? await payments.listFullCurrencies()) ?? []
                let mins = await payments.minAmountsUsd(tickers: tickers)
                coins = CryptoCoinCatalog.buildGroups(
                    availableTickers: tickers,
                    fullCurrencies: full,
                    minAmountsUsd: mins
                )
            }
        }
    }

    func startTopUp() {
        Task {
            isWorking = true
            defer { isWorking = false }
            do {
                let request = NowPaymentsInvoiceFactory.topUp(
                    amountUsd: resolvedAmount,
                    orderId: "topup-\(Int(Date().timeIntervalSince1970))",
                    payCurrency: selectedTicker
                )
                let created = try await payments.createPayment(request)
                let payload = CryptoPayUri.encode(
                    address: created.payAddress,
                    payCurrency: created.payCurrency,
                    payAmount: created.payAmount,
                    extraId: created.extraId
                )
                checkout = PendingPayment.fromCheckout(created, kind: .topUp, qrPayload: payload)
            } catch {
                errorMessage = NowPaymentsErrorMapper.userMessage(error)
            }
        }
    }

    func checkPayment() {
        guard let checkout else { return }
        Task {
            do {
                let dto = try await payments.getPayment(id: checkout.paymentId)
                if DeviceFeePolicy.isConfirmedStatus(dto.paymentStatus) {
                    await DialerRepository.shared.applyConfirmedPayment(
                        paymentId: checkout.paymentId,
                        kind: .topUp,
                        amountUsd: checkout.amountUsd
                    )
                    self.checkout = nil
                }
            } catch {
                errorMessage = NowPaymentsErrorMapper.userMessage(error)
            }
        }
    }
}

struct SettingsScreen: View {
    @EnvironmentObject private var repository: DialerRepository
    @EnvironmentObject private var sip: SipEngine
    @State private var confirmSignOut = false
    @State private var host = ""
    @State private var username = ""
    @State private var password = ""
    @State private var port = "5060"
    @State private var testResult: String?

    var body: some View {
        Form {
            Section("Profile") {
                Text(repository.userProfile.displayName)
                Text(repository.userProfile.email).foregroundStyle(DialerIDColor.inkMuted)
                LabeledContent("Calls", value: "\(repository.userProfile.callsCount)")
                LabeledContent("Minutes", value: "\(repository.userProfile.totalMinutes)m")
            }
            Section("Call audio") {
                Picker("How voice is sent on the line", selection: codecBinding) {
                    Text("Automatic").tag("G711_AUTO")
                    Text("G.711 µ-law").tag("PCMU")
                    Text("G.711 A-law").tag("PCMA")
                }
            }
            Section("Phone line") {
                TextField("Server", text: $host)
                TextField("Username", text: $username)
                SecureField("Password", text: $password)
                TextField("Port", text: $port).keyboardType(.numberPad)
                LabeledContent("Line status", value: sip.registrationState.formattedStatus)
                Button("Test and save line") { saveAndTest() }
                if let testResult {
                    Text(testResult).font(.footnote)
                }
            }
            Section {
                Button("Sign out", role: .destructive) { confirmSignOut = true }
            }
        }
        .onAppear {
            host = repository.userProfile.sipConfig?.host ?? ""
            username = repository.userProfile.sipConfig?.username ?? ""
            password = repository.userProfile.sipConfig?.password ?? ""
            port = String(repository.userProfile.sipConfig?.port ?? 5060)
        }
        .confirmationDialog("Sign out of DialerID?", isPresented: $confirmSignOut) {
            Button("Sign out", role: .destructive) {
                NotificationCenter.default.post(name: .dialerSignOut, object: nil)
            }
        } message: {
            Text("You will need to sign in again before you can place calls from this account.")
        }
    }

    private var codecBinding: Binding<String> {
        Binding(
            get: { repository.userProfile.preferredCodec },
            set: { repository.setPreferredCodec($0) }
        )
    }

    private func saveAndTest() {
        var config = repository.userProfile.sipConfig ?? SipConfig()
        config.host = host
        config.username = username
        config.password = password
        config.port = Int(port) ?? 5060
        repository.saveSipConfig(config)
        Task {
            let result = await sip.testSipConnection(config)
            testResult = result.isSuccess ? "Line verified • \(result.latencyMs)ms" : result.message
        }
    }
}

extension Notification.Name {
    static let dialerSignOut = Notification.Name("dialerSignOut")
}
