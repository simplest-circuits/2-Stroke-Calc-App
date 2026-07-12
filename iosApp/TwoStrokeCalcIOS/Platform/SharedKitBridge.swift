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
        await loadPreferences(into: appState)
        IosKoinInitKt.iosSetAuthListener { snapshot in
            Task { @MainActor in
                appState.applySession(snapshot)
            }
        }
        let snapshot = await IosKoinInitKt.iosRefreshSession()
        appState.applySession(snapshot)
        await refreshVehicles(into: appState)
    }

    @MainActor
    static func loadPreferences(into appState: AppState) async {
        appState.welcomeCompleted = await IosKoinInitKt.iosLoadWelcomeCompleted()
        appState.walkthroughCompleted = await IosKoinInitKt.iosLoadWalkthroughCompleted()
        appState.notificationsEnabled = await IosKoinInitKt.iosLoadNotificationsEnabled()
        appState.firstInstallPermissionsCompleted = await IosKoinInitKt.iosLoadFirstInstallPermissionsCompleted()
        if let theme = ThemeMode(kotlinName: await IosKoinInitKt.iosLoadPreferencesTheme()) {
            appState.themeMode = theme
        }
        if let language = LanguageMode(kotlinName: await IosKoinInitKt.iosLoadPreferencesLanguage()) {
            appState.languageMode = language
        }
        if let nav = NavStyle(kotlinName: await IosKoinInitKt.iosLoadPreferencesNavStyle()) {
            appState.navStyle = nav
        }
    }

    @MainActor
    static func refreshVehicles(into appState: AppState) async {
        let vehicles = await IosKoinInitKt.iosRefreshVehicles()
        appState.vehicles = vehicles.map(VehicleMapper.toItem)
        appState.sharedVehicles = vehicles
    }

    @MainActor
    static func signIn(email: String, password: String) async -> String? {
        initializeIfNeeded()
        return await IosKoinInitKt.iosSignIn(email: email, password: password)
    }

    @MainActor
    static func register(email: String, password: String, displayName: String?) async -> String? {
        initializeIfNeeded()
        return await IosKoinInitKt.iosRegister(email: email, password: password, displayName: displayName)
    }

    @MainActor
    static func sendPasswordReset(email: String) async -> String? {
        initializeIfNeeded()
        return await IosKoinInitKt.iosSendPasswordReset(email: email)
    }

    @MainActor
    static func signInWithGoogle(idToken: String) async -> String? {
        initializeIfNeeded()
        return await IosKoinInitKt.iosSignInWithGoogle(idToken: idToken)
    }

    @MainActor
    static func signOut() async {
        initializeIfNeeded()
        await IosKoinInitKt.iosSignOut()
    }

    @MainActor
    static func saveVehicle(_ vehicle: Vehicle) async {
        initializeIfNeeded()
        await IosKoinInitKt.iosSaveVehicle(vehicle: vehicle)
    }

    @MainActor
    static func deleteVehicle(id: String) async {
        initializeIfNeeded()
        await IosKoinInitKt.iosDeleteVehicle(vehicleId: id)
    }

    @MainActor
    static func verifyProPurchase(token: String, productId: String) async -> Bool {
        initializeIfNeeded()
        return IosKoinInitKt.iosVerifyProPurchase(
            purchaseToken: token,
            productId: productId,
            packageName: "com.simplestsoft.twostrokecalc.ios"
        )
    }

    static func catalogBrands() -> [String] {
        initializeIfNeeded()
        return IosKoinInitKt.iosCatalogBrands() as [String]
    }

    static func catalogModels(brand: String) -> [String] {
        initializeIfNeeded()
        return IosKoinInitKt.iosCatalogModels(brand: brand) as [String]
    }

    static func catalogYears(brand: String, model: String) -> [Int32] {
        initializeIfNeeded()
        return IosKoinInitKt.iosCatalogYears(brand: brand, model: model) as [Int32]
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
    init?(kotlinName: String) {
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
    init?(kotlinName: String) {
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
    init?(kotlinName: String) {
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
