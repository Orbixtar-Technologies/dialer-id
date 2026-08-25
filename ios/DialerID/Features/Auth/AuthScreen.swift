import SwiftUI

struct AuthScreen: View {
    @ObservedObject var viewModel: AuthViewModel

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                Text("DialerID")
                    .font(.largeTitle.bold())
                    .foregroundStyle(DialerIDColor.ink)
                Text("Choose the number you show. Then dial.")
                    .foregroundStyle(DialerIDColor.inkMuted)
                Button {
                    viewModel.google(from: presenterController())
                } label: {
                    Text("Continue with Google")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(PrimaryButtonStyle())
                .disabled(viewModel.isWorking)

                HStack {
                    Rectangle().fill(DialerIDColor.mist).frame(height: 1)
                    Text("or with email").font(.caption).foregroundStyle(DialerIDColor.inkMuted)
                    Rectangle().fill(DialerIDColor.mist).frame(height: 1)
                }

                Picker("Mode", selection: $viewModel.isCreating) {
                    Text("Sign in").tag(false)
                    Text("Create account").tag(true)
                }
                .pickerStyle(.segmented)

                if viewModel.isCreating {
                    labeledField("Full name", text: $viewModel.name, prompt: "e.g. Alex Rivera")
                }
                labeledField("Email", text: $viewModel.email, prompt: "you@company.com")
                    .textInputAutocapitalization(.never)
                    .keyboardType(.emailAddress)
                SecureField("At least 6 characters", text: $viewModel.password)
                    .textFieldStyle(DialerFieldStyle())
                if let error = viewModel.errorMessage {
                    Text(error).foregroundStyle(DialerIDColor.signalRose600).font(.footnote)
                }
                Button(viewModel.isCreating ? "Create account" : "Sign in") {
                    viewModel.submit()
                }
                .buttonStyle(PrimaryButtonStyle())
                .disabled(viewModel.isWorking || viewModel.email.isEmpty || viewModel.password.count < 6)
                Text("Your account and balance stay in sync across devices.")
                    .font(.footnote)
                    .foregroundStyle(DialerIDColor.inkMuted)
            }
            .padding(24)
        }
        .background(DialerIDColor.canvas.ignoresSafeArea())
    }

    private func labeledField(_ title: String, text: Binding<String>, prompt: String) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(title).font(.subheadline.weight(.medium))
            TextField(prompt, text: text)
                .textFieldStyle(DialerFieldStyle())
        }
    }
}

struct PrimaryButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.headline)
            .padding()
            .background(DialerIDColor.sky700.opacity(configuration.isPressed ? 0.85 : 1))
            .foregroundStyle(.white)
            .clipShape(RoundedRectangle(cornerRadius: 14))
    }
}

struct DialerFieldStyle: TextFieldStyle {
    func _body(configuration: TextField<Self._Label>) -> some View {
        configuration
            .padding(12)
            .background(DialerIDColor.sky50)
            .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}
