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
        CatalogBootstrap.loadIfNeeded()
        CatalogBootstrap.syncInBackground()
        IosKoinInitKt.iosSetAuthListener { snapshot in
            Task { @MainActor in
                appState.applySession(snapshot)
            }
        }
        if let snapshot = try? await IosKoinInitKt.iosRefreshSession() {
            appState.applySession(snapshot)
        }
        await refreshVehicles(into: appState)
    }

    @MainActor
    static func loadPreferences(into appState: AppState) async {
        appState.welcomeCompleted = (try? await IosKoinInitKt.iosLoadWelcomeCompleted())?.boolValue ?? false
        appState.walkthroughCompleted = (try? await IosKoinInitKt.iosLoadWalkthroughCompleted())?.boolValue ?? false
        appState.notificationsEnabled = (try? await IosKoinInitKt.iosLoadNotificationsEnabled())?.boolValue ?? false
        appState.firstInstallPermissionsCompleted = (try? await IosKoinInitKt.iosLoadFirstInstallPermissionsCompleted())?.boolValue ?? false
        if let theme = ThemeMode(kotlinName: try? await IosKoinInitKt.iosLoadPreferencesTheme()) {
            appState.themeMode = theme
        }
        if let language = LanguageMode(kotlinName: try? await IosKoinInitKt.iosLoadPreferencesLanguage()) {
            appState.languageMode = language
        }
        if let nav = NavStyle(kotlinName: try? await IosKoinInitKt.iosLoadPreferencesNavStyle()) {
            appState.navStyle = nav
        }
    }

    @MainActor
    static func refreshVehicles(into appState: AppState) async {
        guard let vehicles = try? await IosKoinInitKt.iosRefreshVehicles() else { return }
        appState.vehicles = vehicles.map(VehicleMapper.toItem)
        appState.sharedVehicles = vehicles
    }

    @MainActor
    static func signIn(email: String, password: String) async -> String? {
        initializeIfNeeded()
        return try? await IosKoinInitKt.iosSignIn(email: email, password: password)
    }

    @MainActor
    static func register(email: String, password: String, displayName: String?) async -> String? {
        initializeIfNeeded()
        return try? await IosKoinInitKt.iosRegister(email: email, password: password, displayName: displayName)
    }

    @MainActor
    static func sendPasswordReset(email: String) async -> String? {
        initializeIfNeeded()
        return try? await IosKoinInitKt.iosSendPasswordReset(email: email)
    }

    @MainActor
    static func signInWithGoogle(idToken: String) async -> String? {
        initializeIfNeeded()
        return try? await IosKoinInitKt.iosSignInWithGoogle(idToken: idToken)
    }

    @MainActor
    static func signOut() async {
        initializeIfNeeded()
        try? await IosKoinInitKt.iosSignOut()
    }

    @MainActor
    static func saveVehicle(_ vehicle: Vehicle) async {
        initializeIfNeeded()
        try? await IosKoinInitKt.iosSaveVehicle(vehicle: vehicle)
    }

    @MainActor
    static func deleteVehicle(id: String) async {
        initializeIfNeeded()
        try? await IosKoinInitKt.iosDeleteVehicle(vehicleId: id)
    }

    @MainActor
    static func verifyProPurchase(token: String, productId: String) async -> Bool {
        initializeIfNeeded()
        return IosKoinInitKt.iosVerifyProPurchase(
            purchaseToken: token,
            productId: productId,
            packageName: "com.simplestsoft.twostrokecalc.ios"
        ).boolValue
    }

    static func catalogBrands() -> [String] {
        initializeIfNeeded()
        return IosKoinInitKt.iosCatalogBrands() as [String]
    }

    static func catalogModels(brand: String) -> [String] {
        initializeIfNeeded()
        return IosKoinInitKt.iosCatalogModels(brand: brand) as [String]
    }

    static func catalogYears(brand: String, model: String) -> [Int] {
        initializeIfNeeded()
        return (IosKoinInitKt.iosCatalogYears(brand: brand, model: model) as [KotlinInt]).map(\.intValue)
    }

    static func catalogEntryCount() -> Int {
        initializeIfNeeded()
        return Int(IosKoinInitKt.iosCatalogEntryCount())
    }

    static func persistTheme(_ mode: ThemeMode) {
        IosKoinInitKt.iosSaveTheme(mode: mode.kotlinName)
    }

    static func persistLanguage(_ mode: LanguageMode) {
        IosKoinInitKt.iosSaveLanguage(mode: mode.kotlinName)
    }

    static func persistNavStyle(_ style: NavStyle) {
        IosKoinInitKt.iosSaveNavStyle(style: style.kotlinName)
    }

    static func persistWelcomeCompleted(_ done: Bool) {
        IosKoinInitKt.iosSaveWelcomeCompleted(done: done)
    }

    static func persistWalkthroughCompleted(_ done: Bool) {
        IosKoinInitKt.iosSaveWalkthroughCompleted(done: done)
    }

    static func persistNotificationsEnabled(_ enabled: Bool) {
        IosKoinInitKt.iosSaveNotificationsEnabled(enabled: enabled)
    }

    static func persistFirstInstallPermissionsCompleted(_ done: Bool) {
        IosKoinInitKt.iosSaveFirstInstallPermissionsCompleted(done: done)
    }
}

private extension ThemeMode {
    init?(kotlinName: String?) {
        guard let kotlinName else { return nil }
        switch kotlinName {
        case "SYSTEM": self = .system
        case "LIGHT": self = .light
        case "DARK": self = .dark
        default: return nil
        }
    }

    var kotlinName: String {
        switch self {
        case .system: return "SYSTEM"
        case .light: return "LIGHT"
        case .dark: return "DARK"
        }
    }
}

private extension LanguageMode {
    init?(kotlinName: String?) {
        guard let kotlinName else { return nil }
        switch kotlinName {
        case "SYSTEM": self = .system
        case "GERMAN": self = .german
        case "ENGLISH": self = .english
        default: return nil
        }
    }

    var kotlinName: String {
        switch self {
        case .system: return "SYSTEM"
        case .german: return "GERMAN"
        case .english: return "ENGLISH"
        }
    }
}

private extension NavStyle {
    init?(kotlinName: String?) {
        guard let kotlinName else { return nil }
        switch kotlinName {
        case "BOTTOM_BAR": self = .bottomBar
        case "DRAWER": self = .drawer
        default: return nil
        }
    }

    var kotlinName: String {
        switch self {
        case .bottomBar: return "BOTTOM_BAR"
        case .drawer: return "DRAWER"
        }
    }
}
