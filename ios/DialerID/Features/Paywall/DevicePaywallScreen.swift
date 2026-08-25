import SwiftUI

@MainActor
final class DeviceAccessViewModel: ObservableObject {
    @Published var isChecking = true
    @Published var isUnlocked = false
    @Published var status = ""
    @Published var isWorking = false
    @Published var coins: [CryptoCoinGroup] = []
    @Published var selectedTicker = "btc"
    @Published var checkout: PendingPayment?
    @Published var errorMessage: String?

    private let payments = NowPaymentsClient()

    func refresh() {
        Task {
            isChecking = true
            let registered = await DialerRepository.shared.isDeviceRegistered()
            isUnlocked = registered || DialerRepository.shared.userProfile.isGuest
            isChecking = false
        }
    }

    func startPayment() {
        Task {
            isWorking = true
            defer { isWorking = false }
            do {
                let request = NowPaymentsInvoiceFactory.deviceRegistration(
                    orderId: "device-\(DeviceIdentity.stableDeviceId())-\(Int(Date().timeIntervalSince1970))",
                    payCurrency: selectedTicker
                )
                let created = try await payments.createPayment(request)
                let payload = CryptoPayUri.encode(
                    address: created.payAddress,
                    payCurrency: created.payCurrency,
                    payAmount: created.payAmount,
                    extraId: created.extraId
                )
                checkout = PendingPayment.fromCheckout(
                    created,
                    kind: .deviceRegistration,
                    qrPayload: payload.isEmpty ? created.payAddress : payload
                )
                status = "Waiting for a confirmed payment…"
            } catch {
                errorMessage = NowPaymentsErrorMapper.userMessage(error)
            }
        }
    }

    func loadCoins() {
        Task {
            do {
                let tickers = try await payments.listPayCurrencies()
                let full = (try? await payments.listFullCurrencies()) ?? []
                let mins = await payments.minAmountsUsd(tickers: tickers)
                coins = CryptoCoinCatalog.buildGroups(
                    availableTickers: tickers,
                    fullCurrencies: full,
                    minAmountsUsd: mins
                )
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
                        kind: .deviceRegistration,
                        amountUsd: DeviceFeePolicy.deviceFeeUsd
                    )
                    isUnlocked = true
                    status = "Device registered"
                } else {
                    status = "Status: \(dto.paymentStatus ?? "waiting")"
                }
            } catch {
                errorMessage = NowPaymentsErrorMapper.userMessage(error)
            }
        }
    }
}

struct DevicePaywallScreen: View {
    @ObservedObject var viewModel: DeviceAccessViewModel
    var onSignOut: () -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Text("Register this device")
                    .font(.title.bold())
                Text("This device is new on your account. Pay a one-time $\(Int(DeviceFeePolicy.deviceFeeUsd)) fee to register it. Checkout and network fees are paid by you.")
                    .foregroundStyle(DialerIDColor.inkMuted)
                LabeledContent("This device", value: DeviceIdentity.deviceLabel())
                if !viewModel.status.isEmpty {
                    Text(viewModel.status).font(.footnote)
                }
                if let error = viewModel.errorMessage {
                    Text(error).foregroundStyle(DialerIDColor.signalRose600).font(.footnote)
                }
                if viewModel.checkout == nil {
                    Picker("Cryptocurrency", selection: $viewModel.selectedTicker) {
                        ForEach(viewModel.coins) { group in
                            ForEach(group.networks, id: \.ticker) { network in
                                Text("\(group.name) · \(network.networkLabel)").tag(network.ticker)
                            }
                        }
                    }
                    Button("Pay $\(Int(DeviceFeePolicy.deviceFeeUsd)) to register") {
                        viewModel.startPayment()
                    }
                    .buttonStyle(PrimaryButtonStyle())
                    .disabled(viewModel.isWorking)
                } else if let payment = viewModel.checkout {
                    CryptoCheckoutPanel(payment: payment, onCheck: viewModel.checkPayment)
                }
                Button("Use a different account", action: onSignOut)
                    .frame(maxWidth: .infinity)
            }
            .padding(24)
        }
        .background(DialerIDColor.canvas.ignoresSafeArea())
        .onAppear { viewModel.loadCoins() }
    }
}

struct CryptoCheckoutPanel: View {
    let payment: PendingPayment
    var onCheck: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Pay with crypto").font(.headline)
            Text(String(format: "Amount: $%.2f", payment.amountUsd))
            CryptoQRView(payload: payment.qrPayload)
                .frame(width: 200, height: 200)
            labeled("Send exactly", value: String(payment.payAmount))
            labeled("Wallet address", value: payment.payAddress)
            if !payment.extraId.isEmpty {
                Text("Include this memo or destination tag with the transfer.")
                    .font(.footnote)
                    .foregroundStyle(DialerIDColor.signalAmber600)
                labeled("Memo / destination tag", value: payment.extraId)
            }
            Text("Network and processing fees are paid by you and are already included in the amount above.")
                .font(.footnote)
                .foregroundStyle(DialerIDColor.inkMuted)
            Button("Check payment", action: onCheck)
                .buttonStyle(PrimaryButtonStyle())
        }
        .padding()
        .background(DialerIDColor.sky50)
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private func labeled(_ title: String, value: String) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(title).font(.caption).foregroundStyle(DialerIDColor.inkMuted)
            Text(value).textSelection(.enabled)
        }
    }
}
