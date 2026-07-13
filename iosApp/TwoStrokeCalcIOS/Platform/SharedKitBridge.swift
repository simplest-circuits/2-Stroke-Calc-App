import Foundation
import sharedKit

enum SharedKitBridge {
    private static var initialized = false

    static func initializeIfNeeded() {
        guard !initialized else { return }
        FirebaseBootstrap.configureIfNeeded()
        IosKoinInitKt.startSharedKoin()
        initialized = true
    }

    @MainActor
    static func bootstrap(appState: AppState) async {
        initializeIfNeeded()
        await loadPreferences(into: appState)
        CatalogBootstrap.loadIfNeeded()
        CatalogBootstrap.syncInBackground()
        IosKoinInitKt.iosSetAuthListener { snapshot in
            Task { @MainActor in
                appState.applySession(snapshot)
                await loadPreferences(into: appState)
            }
        }
        if let snapshot = try? await IosKoinInitKt.iosRefreshSession() {
            appState.applySession(snapshot)
            await loadPreferences(into: appState)
        }
        await evaluateFirstInstallPermissions(into: appState)
        appState.markPreferencesLoaded()
        await refreshVehicles(into: appState)
    }

    @MainActor
    static func loadPreferences(into appState: AppState) async {
        appState.welcomeCompleted = await loadBool { try await IosKoinInitKt.iosLoadWelcomeCompleted() }
        appState.walkthroughCompleted = await loadBool { try await IosKoinInitKt.iosLoadWalkthroughCompleted() }
        appState.notificationsEnabled = await loadBool { try await IosKoinInitKt.iosLoadNotificationsEnabled() }
        appState.firstInstallPermissionsCompleted = await loadBool {
            try await IosKoinInitKt.iosLoadFirstInstallPermissionsCompleted()
        }
        if let theme = ThemeMode(kotlinName: try? await IosKoinInitKt.iosLoadPreferencesTheme()) {
            appState.themeMode = theme
        }
        if let language = LanguageMode(kotlinName: try? await IosKoinInitKt.iosLoadPreferencesLanguage()) {
            appState.languageMode = language
        }
        if let nav = NavStyle(kotlinName: try? await IosKoinInitKt.iosLoadPreferencesNavStyle()) {
            appState.navStyle = nav
        }
        appState.applyLoadedPreferences()
    }

    @MainActor
    private static func evaluateFirstInstallPermissions(into appState: AppState) async {
        guard !appState.firstInstallPermissionsCompleted else { return }
        let canPost = await NotificationService.canPostNotifications()
        guard !IosKoinInitKt.iosIsFirstInstallPermissionsFlowNeeded(canPostNotifications: canPost) else { return }
        if canPost, !appState.notificationsEnabled {
            appState.updateNotifications(true)
        }
        appState.completeFirstInstallPermissions()
    }

    @MainActor
    private static func loadBool(_ loader: () async throws -> KotlinBoolean) async -> Bool {
        (try? await loader())?.boolValue ?? false
    }

    @MainActor
    static func refreshVehicles(into appState: AppState) async {