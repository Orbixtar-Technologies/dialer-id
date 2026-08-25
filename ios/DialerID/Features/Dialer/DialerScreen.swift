import SwiftUI

struct DialerScreen: View {
    @EnvironmentObject private var session: AppSession
    @EnvironmentObject private var repository: DialerRepository
    @EnvironmentObject private var sip: SipEngine
    @EnvironmentObject private var calls: CallManager
    @State private var digits = ""
    @State private var blockedMessage: String?

    private var estimate: CallRate? {
        guard !digits.isEmpty else { return nil }
        let e164 = E164.format(digits, defaultRegion: E164.defaultRegion())
        return repository.rateCatalog.lookup(e164)
    }

    var body: some View {
        VStack(spacing: 12) {
            Text(digits.isEmpty ? "Enter number" : digits)
                .font(.system(size: 32, weight: .medium, design: .monospaced))
                .foregroundStyle(DialerIDColor.ink)
                .lineLimit(1)
                .minimumScaleFactor(0.5)
                .frame(maxWidth: .infinity, alignment: .center)
                .padding(.top, 12)
            if let estimate {
                Text("\(estimate.name) · \(String(format: "$%.4f", estimate.markedUpRatePerMin))/min")
                    .font(.footnote)
                    .foregroundStyle(DialerIDColor.inkMuted)
            }
            Text(sip.registrationState.formattedStatus)
                .font(.caption)
                .foregroundStyle(sip.isRegistered ? DialerIDColor.signalGreen600 : DialerIDColor.inkMuted)
            callerIdChip
            keypad
            HStack {
                Button {
                    digits = String(digits.dropLast())
                } label: {
                    Image(systemName: "delete.left")
                        .frame(width: 56, height: 56)
                }
                .accessibilityLabel("Backspace")
                Button(action: placeCall) {
                    Image(systemName: "phone.fill")
                        .font(.title2)
                        .foregroundStyle(.white)
                        .frame(width: 72, height: 72)
                        .background(DialerIDColor.signalGreen600)
                        .clipShape(Circle())
                }
                .accessibilityLabel("Place call")
                Button {
                    sip.refreshNow(force: true)
                } label: {
                    Image(systemName: "arrow.clockwise")
                        .frame(width: 56, height: 56)
                }
                .accessibilityLabel("Retry")
            }
            if let blockedMessage {
                Text(blockedMessage)
                    .font(.footnote)
                    .foregroundStyle(DialerIDColor.signalRose600)
            }
        }
        .padding(.horizontal, 16)
        .background(DialerIDColor.canvas)
        .onChange(of: session.incomingDialNumber) { number in
            if let number {
                digits = PhoneNumberSanitizer.filterDialInput(number)
                session.incomingDialNumber = nil
            }
        }
    }

    private var callerIdChip: some View {
        let shown = repository.userProfile.selectedCallerId
        return HStack {
            Text("Showing as")
                .font(.caption)
                .foregroundStyle(DialerIDColor.inkMuted)
            Text(shown.isEmpty ? "No number yet — tap to add one" : CallerIdDisplay.publicIdentity(shown))
                .font(.subheadline.weight(.medium))
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
        .background(DialerIDColor.brandPaleBlueSoft)
        .clipShape(Capsule())
    }

    private var keypad: some View {
        let rows = [["1", "2", "3"], ["4", "5", "6"], ["7", "8", "9"], ["*", "0", "#"]]
        return VStack(spacing: 10) {
            ForEach(rows, id: \.self) { row in
                HStack(spacing: 18) {
                    ForEach(row, id: \.self) { key in
                        Button {
                            digits = PhoneNumberSanitizer.filterDialInput(digits + key)
                        } label: {
                            Text(key)
                                .font(.title)
                                .frame(width: 72, height: 72)
                                .background(DialerIDColor.sky50)
                                .clipShape(Circle())
                        }
                        .accessibilityLabel(key)
                    }
                }
            }
        }
    }

    private func placeCall() {
        blockedMessage = nil
        if repository.userProfile.isGuest {
            blockedMessage = "Sign in to place calls and add credit."
            return
        }
        if !sip.isRegistered {
            blockedMessage = "Line offline"
            return
        }
        let e164 = E164.format(digits, defaultRegion: E164.defaultRegion())
        switch OutboundBillingPolicy.evaluate(
            destination: e164,
            balanceUsd: repository.userProfile.creditBalance,
            catalog: repository.rateCatalog
        ) {
        case .missingRate:
            blockedMessage = "This destination isn't available."
        case .insufficientCredit:
            blockedMessage = "Not enough credit for this destination."
        case .allowed(let rate):
            let started = calls.startCall(
                destinationNumber: e164,
                callerId: repository.userProfile.selectedCallerId,
                countryName: rate.name,
                rate: rate.markedUpRatePerMin
            )
            if !started {
                blockedMessage = "Couldn't start the call."
            }
        }
    }
}

struct ActiveCallScreen: View {
    @EnvironmentObject private var calls: CallManager
    @State private var showKeypad = false

    var body: some View {
        let info = calls.callState
        VStack(spacing: 24) {
            Spacer()
            Text(info.destinationNumber)
                .font(.largeTitle.weight(.medium))
            Text(info.displayStatus)
                .foregroundStyle(DialerIDColor.inkMuted)
            Text(info.isTalking ? info.formattedDuration : "--:--")
                .font(.system(.title, design: .monospaced))
            Text("Showing as \(CallerIdDisplay.publicIdentity(info.callerIdUsed))")
                .font(.footnote)
                .foregroundStyle(DialerIDColor.inkMuted)
            HStack(spacing: 24) {
                callButton(info.isMuted ? "mic.slash.fill" : "mic.fill", info.isMuted ? "Muted" : "Mute", active: info.isMuted, action: calls.toggleMute)
                callButton("circle.grid.3x3.fill", "Keypad", active: showKeypad) { showKeypad.toggle() }
                callButton(info.isSpeakerOn ? "speaker.wave.3.fill" : "speaker.fill", info.isSpeakerOn ? "Speaker" : "Earpiece", active: info.isSpeakerOn, action: calls.toggleSpeaker)
            }
            if showKeypad {
                dtmfPad
            }
            Button(action: calls.endCall) {
                Image(systemName: "phone.down.fill")
                    .font(.title)
                    .foregroundStyle(.white)
                    .frame(width: 76, height: 76)
                    .background(DialerIDColor.signalRose600)
                    .clipShape(Circle())
            }
            .accessibilityLabel("End call")
            Spacer()
        }
        .padding()
        .background(DialerIDColor.night950.ignoresSafeArea())
        .foregroundStyle(.white)
    }

    private var dtmfPad: some View {
        let keys = ["1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "0", "#"]
        return LazyVGrid(columns: Array(repeating: GridItem(.flexible()), count: 3), spacing: 8) {
            ForEach(keys, id: \.self) { key in
                Button(key) {
                    if let digit = key.first {
                        calls.sendDtmf(digit)
                    }
                }
                .frame(height: 44)
                .background(Color.white.opacity(0.12))
                .clipShape(RoundedRectangle(cornerRadius: 8))
            }
        }
        .padding(.horizontal)
    }

    private func callButton(_ symbol: String, _ title: String, active: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            VStack {
                Image(systemName: symbol)
                    .frame(width: 56, height: 56)
                    .background(active ? DialerIDColor.sky700 : Color.white.opacity(0.12))
                    .clipShape(Circle())
                Text(title).font(.caption2)
            }
        }
        .accessibilityLabel(title)
    }
}
