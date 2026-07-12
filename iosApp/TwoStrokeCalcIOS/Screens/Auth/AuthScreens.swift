import SwiftUI

struct LoginScreen: View {
    @EnvironmentObject private var appState: AppState
    @Environment(\.themeColors) private var colors
    @State private var email = ""
    @State private var password = ""

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                Text(S.loginTitle).font(.title2.bold())
                Text(S.loginSubtitle)
                    .foregroundStyle(colors.onSurfaceVariant)
                    .multilineTextAlignment(.center)
                AppOutlinedField(label: S.emailLabel, text: $email)
                AppOutlinedField(label: S.passwordLabel, text: $password, secure: true)
                if let error = appState.authError {
                    Text(error).foregroundStyle(colors.error).font(.footnote)
                }
                PrimaryButton(title: S.loginButton, loading: appState.authLoading) {
                    Task { await appState.signIn(email: email, password: password) }
                }
                Button(S.forgotPassword) { appState.selectedTab = .forgotPassword }
                PrimaryButton(title: S.signInGoogle) {
                    Task { await GoogleSignInService.signIn(appState: appState) }
                }
                Button(S.noAccountRegister) { appState.selectedTab = .register }
            }
            .padding(24)
        }
        .background(colors.background)
    }
}

struct RegisterScreen: View {
    @EnvironmentObject private var appState: AppState
    @Environment(\.themeColors) private var colors
    @State private var email = ""
    @State private var password = ""
    @State private var displayName = ""

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                Text(S.registerTitle).font(.title2.bold())
                AppOutlinedField(label: S.displayNameLabel, text: $displayName)
                AppOutlinedField(label: S.emailLabel, text: $email)
                AppOutlinedField(label: S.passwordLabel, text: $password, secure: true)
                if let error = appState.authError {
                    Text(error).foregroundStyle(colors.error)
                }
                PrimaryButton(title: S.registerButton, loading: appState.authLoading) {
                    Task { await appState.register(email: email, password: password, name: displayName) }
                }
                Button("Zurück zum Login") { appState.selectedTab = .login }
            }
            .padding(24)
        }
        .background(colors.background)
    }
}

struct ForgotPasswordScreen: View {
    @EnvironmentObject private var appState: AppState
    @Environment(\.themeColors) private var colors
    @State private var email = ""
    @State private var sent = false

    var body: some View {
        VStack(spacing: 16) {
            Text(S.forgotPasswordTitle).font(.title2.bold())
            AppOutlinedField(label: S.emailLabel, text: $email)
            if sent {
                Text("Reset-Link wurde gesendet. Bitte prüfe dein Postfach.").foregroundStyle(colors.primary)
            }
            PrimaryButton(title: S.forgotPasswordButton, loading: appState.authLoading) {
                Task {
                    if let error = await appState.sendPasswordReset(email: email) {
                        appState.authError = error
                        sent = false
                    } else {
                        appState.authError = nil
                        sent = true
                    }
                }
            }
            Button("Zurück") { appState.selectedTab = .login }
            Spacer()
        }
        .padding(24)
        .background(colors.background)
    }
}

struct AuthGateDialog: View {
    let onDismiss: () -> Void
    let onLogin: () -> Void

    var body: some View {
        ZStack {
            Color.black.opacity(0.4).ignoresSafeArea()
            VStack(spacing: 16) {
                Text(S.authGateTitle).font(.headline)
                Text(S.authGateMessage)
                HStack {
                    Button(S.cancel, action: onDismiss)
                    Button(S.authGateLogin, action: onLogin)
                        .buttonStyle(.borderedProminent)
                }
            }
            .padding(24)
            .background(.regularMaterial)
            .clipShape(RoundedRectangle(cornerRadius: 16))
            .padding(32)
        }
    }
}

struct ProUpsellDialog: View {
    @EnvironmentObject private var appState: AppState
    let moduleName: String
    let onDismiss: () -> Void

    var body: some View {
        ZStack {
            Color.black.opacity(0.4).ignoresSafeArea()
            VStack(spacing: 16) {
                Text(S.proUpsellTitle).font(.headline)
                Text("Modul \(moduleName) erfordert Pro.")
                if let price = appState.storeKitPrice {
                    Text(price).font(.subheadline).foregroundStyle(.secondary)
                }
                HStack {
                    Button(S.cancel, action: onDismiss)
                    Button(S.buyPro) {
                        Task {
                            await appState.purchasePro()
                            onDismiss()
                        }
                    }
                    .buttonStyle(.borderedProminent)
                }
            }
            .padding(24)
            .background(.regularMaterial)
            .clipShape(RoundedRectangle(cornerRadius: 16))
            .padding(32)
        }
    }
}
