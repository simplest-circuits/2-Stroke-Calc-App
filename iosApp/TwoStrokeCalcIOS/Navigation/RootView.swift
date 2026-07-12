import SwiftUI
import sharedKit

struct RootView: View {
    @EnvironmentObject private var appState: AppState

    var body: some View {
        ThemedRoot(appState: appState) {
            Group {
                if appState.showSplash {
                    SplashScreen {
                        appState.completeSplash()
                    }
                } else if !appState.welcomeCompleted {
                    WelcomeScreen {
                        appState.completeWelcome()
                    }
                } else if !appState.firstInstallPermissionsCompleted {
                    FirstInstallPermissionsScreen {
                        Task {
                            let granted = await NotificationService.requestPermission()
                            appState.updateNotifications(granted)
                            appState.completeFirstInstallPermissions()
                        }
                    }
                } else {
                    MainShellView()
                }
            }
        }
    }
}

struct MainShellView: View {
    @EnvironmentObject private var appState: AppState
    @Environment(\.themeColors) private var colors
    @State private var drawerOpen = false

    var body: some View {
        ZStack {
            if appState.navStyle == .drawer {
                NavigationSplitView {
                    drawerSidebar
                } detail: {
                    shellContent
                }
            } else {
                shellContent
            }
        }
        .background(colors.background)
    }

    private var shellContent: some View {
        tabContent
            .walkthroughTargetSpace()
            .safeAreaInset(edge: .bottom) {
                if appState.navStyle == .bottomBar {
                    bottomBar
                }
            }
            .overlay { overlays }
            .overlay {
                if appState.showWalkthrough {
                    WalkthroughOverlay(stepIndex: $appState.walkthroughStepIndex) {
                        appState.finishWalkthrough()
                    }
                }
            }
    }

    private var overlays: some View {
        Group {
            if appState.showAuthGate {
                AuthGateDialog(
                    onDismiss: { appState.showAuthGate = false },
                    onLogin: {
                        appState.showAuthGate = false
                        appState.selectedTab = .login
                    }
                )
            }
            if let module = appState.proUpsellModule {
                ProUpsellDialog(moduleName: module) {
                    appState.proUpsellModule = nil
                }
            }
        }
    }

    private var drawerSidebar: some View {
        List {
            drawerRow(.calculator, label: S.navCalculator, icon: "gauge.with.dots.needle.67percent")
            drawerRow(.vehicles, label: S.navVehicles, icon: "bicycle")
            drawerRow(.settings, label: S.settingsTitle, icon: "gearshape")
            if appState.isAdmin && appState.isAuthenticated {
                drawerRow(.admin, label: S.adminPanelTitle, icon: "person.badge.key")
            }
        }
        .navigationTitle(S.appName)
    }

    private func drawerRow(_ route: AppRoute, label: String, icon: String) -> some View {
        Button {
            appState.selectedTab = route
        } label: {
            Label(label, systemImage: icon)
        }
    }

    @ViewBuilder
    private var tabContent: some View {
        switch appState.selectedTab {
        case .calculator:
            CalculatorScreen()
        case .vehicles:
            VehiclesScreen()
        case .settings:
            SettingsScreen()
        case .account:
            AccountScreen(onBack: { appState.selectedTab = .settings })
        case .admin:
            AdminPanelScreen(onBack: { appState.selectedTab = .settings })
        case .login:
            LoginScreen()
        case .register:
            RegisterScreen()
        case .forgotPassword:
            ForgotPasswordScreen()
        }
    }

    private var bottomBar: some View {
        HStack {
            tabItem(.calculator, label: S.navCalculator, systemImage: "gauge.with.dots.needle.67percent")
                .walkthroughAnchor(WalkthroughTargetIds.calculatorTab)
            tabItem(.vehicles, label: S.navVehicles, systemImage: "bicycle")
                .walkthroughAnchor(WalkthroughTargetIds.vehiclesTab)
            tabItem(.settings, label: S.settingsTitle, systemImage: "gearshape")
                .walkthroughAnchor(WalkthroughTargetIds.settingsTab)
            if appState.isAdmin && appState.isAuthenticated {
                tabItem(.admin, label: S.adminPanelTitle, systemImage: "person.badge.key")
            }
        }
        .padding(.horizontal, 8)
        .padding(.top, 8)
        .padding(.bottom, 4)
        .background(colors.surface)
        .overlay(alignment: .top) { Divider() }
    }

    private func tabItem(_ route: AppRoute, label: String, systemImage: String) -> some View {
        Button {
            if route == .admin && !appState.isAuthenticated {
                appState.showAuthGate = true
                return
            }
            appState.selectedTab = route
        } label: {
            VStack(spacing: 4) {
                Image(systemName: systemImage)
                Text(label)
                    .font(.caption2)
                    .lineLimit(1)
                    .minimumScaleFactor(0.8)
            }
            .frame(maxWidth: .infinity)
            .foregroundStyle(appState.selectedTab == route ? colors.primary : colors.onSurfaceVariant)
        }
        .buttonStyle(.plain)
    }
}
