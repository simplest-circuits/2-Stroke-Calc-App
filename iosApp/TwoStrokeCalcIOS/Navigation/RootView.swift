import SwiftUI
import sharedKit

struct RootView: View {
    @EnvironmentObject private var appState: AppState

    var body: some View {
        ThemedRoot(appState: appState) {
            Group {
                if appState.showSplash || !appState.preferencesLoaded {
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
        .id(appState.localeRevision)
    }
}

struct MainShellView: View {
    @EnvironmentObject private var appState: AppState
    @Environment(\.themeColors) private var colors
    @State private var drawerOpen = false

    private let drawerWidth: CGFloat = 300

    var body: some View {
        Group {
            if appState.navStyle == .drawer {
                drawerShell
            } else {
                bottomBarShell
            }
        }
        .id(appState.navStyle)
        .background(colors.background)
        .onChange(of: appState.navStyle) { _, _ in
            drawerOpen = false
        }
        .onChange(of: appState.selectedTab) { _, _ in
            drawerOpen = false
        }
        .alert(
            appState.billingError ?? "",
            isPresented: Binding(
                get: { appState.billingError != nil },
                set: { if !$0 { appState.billingError = nil } }
            )
        ) {
            Button(S.cancel, role: .cancel) {
                appState.billingError = nil
            }
        }
    }

    private var bottomBarShell: some View {
        mainContent
            .safeAreaInset(edge: .bottom) {
                bottomBar
            }
    }

    private var drawerShell: some View {
        ZStack(alignment: .leading) {
            VStack(spacing: 0) {
                drawerTopBar
                mainContent
            }

            if drawerOpen {
                Color.black.opacity(0.35)
                    .ignoresSafeArea()
                    .onTapGesture { drawerOpen = false }

                drawerPanel
                    .frame(width: drawerWidth)
                    .transition(.move(edge: .leading))
            }
        }
        .animation(.easeInOut(duration: 0.22), value: drawerOpen)
    }

    private var drawerTopBar: some View {
        HStack(spacing: 12) {
            Button {
                drawerOpen = true
            } label: {
                Image(systemName: "line.3.horizontal")
                    .font(.title3)
                    .frame(width: 44, height: 44)
            }
            .buttonStyle(.plain)
            .accessibilityLabel(L.t("cd_menu"))

            Text(navigationBarTitle)
                .font(.headline)
                .foregroundStyle(colors.onSurface)
                .lineLimit(1)

            Spacer(minLength: 0)
        }
        .padding(.horizontal, 8)
        .background(colors.surface)
        .overlay(alignment: .bottom) {
            Divider()
        }
    }

    private var drawerPanel: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text(S.appName)
                .font(.title3.bold())
                .foregroundStyle(colors.onSurface)
                .padding(.horizontal, 20)
                .padding(.top, 20)
                .padding(.bottom, 12)

            ScrollView {
                VStack(spacing: 4) {
                    drawerNavRow(.calculator, label: S.navCalculator, icon: "gauge.with.dots.needle.67percent")
                        .walkthroughAnchor(WalkthroughTargetIds.calculatorTab)
                    drawerNavRow(.tools, label: S.navTools, icon: "wrench.and.screwdriver")
                    drawerNavRow(.vehicles, label: S.navVehicles, icon: "bicycle")
                        .walkthroughAnchor(WalkthroughTargetIds.vehiclesTab)
                    drawerNavRow(.settings, label: S.settingsTitle, icon: "gearshape")
                        .walkthroughAnchor(WalkthroughTargetIds.settingsTab)
                    if appState.isAdmin && appState.isAuthenticated {
                        drawerNavRow(.admin, label: S.adminPanelTitle, icon: "person.badge.key")
                    }
                }
                .padding(.horizontal, 12)
                .padding(.bottom, 16)
            }

            Spacer(minLength: 0)
        }
        .frame(maxHeight: .infinity)
        .background(colors.surface)
        .shadow(color: .black.opacity(0.15), radius: 8, x: 2, y: 0)
    }

    private var mainContent: some View {
        tabContent
            .walkthroughTargetSpace()
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

    @ViewBuilder
    private var tabContent: some View {
        switch appState.selectedTab {
        case .calculator:
            CalculatorScreen()
        case .tools:
            ToolsScreen()
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
            tabItem(.tools, label: S.navTools, systemImage: "wrench.and.screwdriver")
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

    private func drawerNavRow(_ route: AppRoute, label: String, icon: String) -> some View {
        let selected = isMainTabSelected(route)
        return Button {
            navigateToTab(route)
        } label: {
            HStack(spacing: 12) {
                Image(systemName: icon)
                    .frame(width: 24)
                Text(label)
                    .font(.body.weight(selected ? .semibold : .regular))
                Spacer(minLength: 0)
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 12)
            .foregroundStyle(selected ? colors.primary : colors.onSurface)
            .background(selected ? colors.primaryContainer.opacity(0.65) : Color.clear)
            .clipShape(RoundedRectangle(cornerRadius: 12))
        }
        .buttonStyle(.plain)
    }

    private func tabItem(_ route: AppRoute, label: String, systemImage: String) -> some View {
        Button {
            navigateToTab(route)
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

    private func navigateToTab(_ route: AppRoute) {
        if route == .admin && !appState.isAuthenticated {
            appState.showAuthGate = true
            return
        }
        drawerOpen = false
        appState.selectedTab = route
    }

    private func isMainTabSelected(_ route: AppRoute) -> Bool {
        switch route {
        case .calculator:
            return appState.selectedTab == .calculator
        case .tools:
            return appState.selectedTab == .tools
        case .vehicles:
            return appState.selectedTab == .vehicles
        case .settings:
            return appState.selectedTab == .settings || appState.selectedTab == .account
        case .admin:
            return appState.selectedTab == .admin
        default:
            return false
        }
    }

    private var navigationBarTitle: String {
        switch appState.selectedTab {
        case .calculator:
            return S.navCalculator
        case .tools:
            return S.navTools
        case .vehicles:
            return S.navVehicles
        case .settings, .account:
            return S.settingsTitle
        case .admin:
            return S.adminPanelTitle
        case .login:
            return S.loginTitle
        case .register:
            return S.registerTitle
        case .forgotPassword:
            return S.forgotPasswordTitle
        }
    }
}
