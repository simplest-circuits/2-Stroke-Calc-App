import SwiftUI

struct SplashScreen: View {
    @Environment(\.themeColors) private var colors
    let onFinished: () -> Void
    @State private var opacity: Double = 0

    var body: some View {
        ZStack {
            colors.background.ignoresSafeArea()
            VStack(spacing: 16) {
                Image(systemName: "gauge.with.dots.needle.67percent")
                    .font(.system(size: 72))
                    .foregroundStyle(AppTheme.brightGold)
                    .opacity(opacity)
                Text(S.appName)
                    .font(.title.bold())
                    .foregroundStyle(colors.onBackground)
                    .opacity(opacity)
                Text(S.splashSlogan)
                    .font(.subheadline)
                    .foregroundStyle(colors.onSurfaceVariant)
                    .opacity(opacity)
            }
        }
        .onAppear {
            withAnimation(.easeOut(duration: 0.5)) { opacity = 1 }
            DispatchQueue.main.asyncAfter(deadline: .now() + 1.2) {
                withAnimation(.easeOut(duration: 0.4)) { opacity = 0 }
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.4) { onFinished() }
            }
        }
    }
}

struct WelcomeScreen: View {
    @Environment(\.themeColors) private var colors
    let onContinue: () -> Void

    var body: some View {
        VStack(spacing: 24) {
            Spacer()
            Text(S.welcomeTitle)
                .font(.title.bold())
                .multilineTextAlignment(.center)
            Text(S.calculatorOverviewSubtitle)
                .multilineTextAlignment(.center)
                .foregroundStyle(colors.onSurfaceVariant)
            Spacer()
            PrimaryButton(title: S.welcomeContinue, action: onContinue)
        }
        .padding(24)
        .background(colors.background)
    }
}

struct FirstInstallPermissionsScreen: View {
    @Environment(\.themeColors) private var colors
    let onContinue: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 20) {
            Text(S.permissionsTitle)
                .font(.title2.bold())
            Text("Benachrichtigungen für Wartungserinnerungen können später in den Einstellungen aktiviert werden.")
                .foregroundStyle(colors.onSurfaceVariant)
            Spacer()
            PrimaryButton(title: S.permissionsContinue, action: onContinue)
        }
        .padding(24)
        .background(colors.background)
    }
}
