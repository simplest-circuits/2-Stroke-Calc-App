import Foundation
import Combine
import sharedKit
import UIKit

enum NavStyle: String, CaseIterable {
    case bottomBar = "BOTTOM_BAR"
    case drawer = "DRAWER"
}

@MainActor
final class AppState: ObservableObject {
    @Published var showSplash = true
    @Published private(set) var preferencesLoaded = false
    private var splashAnimationFinished = false
    @Published var welcomeCompleted = false
    @Published var walkthroughCompleted = false
    @Published var showWalkthrough = false
    @Published var firstInstallPermissionsCompleted = false
    @Published var isAuthenticated = false
    @Published var isAdmin = false
    @Published var isPro = false
    @Published var themeMode: ThemeMode = .system
    @Published var navStyle: NavStyle = .bottomBar
    @Published var languageMode: LanguageMode = .system
    @Published private(set) var localeRevision = UUID()
    @Published var notificationsEnabled = false
    @Published var displayName: String = ""
    @Published var accountEmail: String = ""
    @Published var authError: String?
    @Published var authLoading = false
    @Published var showAuthGate = false
    @Published var pendingAuthRoute: AppRoute?
    @Published var proUpsellModule: String?
    @Published var disabledCalculators: Set<String> = []
    @Published var readOnlyModules: Set<String> = ["VEHICLES", "TIMING", "DC_CABLE", "FLUID", "GEAR"]
    @Published var vehicles: [VehicleItem] = []
    @Published var sharedVehicles: [Vehicle] = []
    @Published var selectedTab: AppRoute = .calculator
    @Published var calculatorStack: CalculatorId?
    @Published var vehicleDetailId: String?
    @Published var walkthroughStepIndex = 0
    @Published var storeKitPrice: String?
    @Published var isPurchasing = false
    @Published var billingError: String?

    var isDarkTheme: Bool {
        switch themeMode {
        case .dark: return true
        case .light: return false
        case .system:
            return UITraitCollection.current.userInterfaceStyle == .dark
        }
    }

    var visibleCalculators: [CalculatorId] {
        CalculatorCatalog.displayOrder.filter { !disabledCalculators.contains($0.name) }
    }

    func isCalculatorReadOnly(_ id: CalculatorId) -> Bool {
        let module = ProModuleMapping.fromCalculator(id)
        return readOnlyModules.contains(module) && !isPro && !isAdmin
    }

    func hasVehiclesAccess() -> Bool {
        isAdmin || isPro || !readOnlyModules.contains("VEHICLES")
    }

    func vehicle(for id: String) -> Vehicle? {
        sharedVehicles.first { $0.id == id }
    }

    func applySession(_ snapshot: IosSessionSnapshot) {
        isAuthenticated = snapshot.isAuthenticated
        accountEmail = snapshot.email
        displayName = snapshot.displayName
        isAdmin = snapshot.isAdmin
        isPro = snapshot.isPro
        disabledCalculators = Set(snapshot.disabledCalculatorNames)
        if !snapshot.readOnlyModuleNames.isEmpty {
            readOnlyModules = Set(snapshot.readOnlyModuleNames)
        }
    }

    private func refreshSessionFromShared() async {
        guard let snapshot = try? await IosKoinInitKt.iosRefreshSession() else { return }
        applySession(snapshot)
    }

    func completeSplash() {
        splashAnimationFinished = true
        dismissSplashIfReady()
    }

    func markPreferencesLoaded() {
        preferencesLoaded = true
        dismissSplashIfReady()
    }

    private func dismissSplashIfReady() {
        guard splashAnimationFinished, preferencesLoaded else { return }
        showSplash = false
    }

    func completeWelcome() {
        welcomeCompleted = true
        SharedKitBridge.persistWelcomeCompleted(true)
        Task { await SharedKitBridge.refreshVehicles(into: self) }
        if !walkthroughCompleted {
            showWalkthrough = true
            walkthroughStepIndex = 0
        }
    }

    func finishWalkthrough() {
        walkthroughCompleted = true
        showWalkthrough = false
        SharedKitBridge.persistWalkthroughCompleted(true)
    }

    func updateTheme(_ mode: ThemeMode) {
        themeMode = mode
        SharedKitBridge.persistTheme(mode)
    }

    func updateLanguage(_ mode: LanguageMode) {
        languageMode = mode
        AppLocalization.apply(languageMode: mode)
        localeRevision = UUID()
        SharedKitBridge.persistLanguage(mode)
    }

    func applyLoadedPreferences() {
        AppLocalization.apply(languageMode: languageMode)
        localeRevision = UUID()
    }

    func updateNavStyle(_ style: NavStyle) {
        navStyle = style
        SharedKitBridge.persistNavStyle(style)
    }

    func updateNotifications(_ enabled: Bool) {
        notificationsEnabled = enabled
        SharedKitBridge.persistNotificationsEnabled(enabled)
    }

    func completeFirstInstallPermissions() {
        firstInstallPermissionsCompleted = true
        SharedKitBridge.persistFirstInstallPermissionsCompleted(true)
    }

    func signIn(email: String, password: String) async {
        authLoading = true
        authError = nil
        defer { authLoading = false }
        guard !email.isEmpty, !password.isEmpty else {
            authError = L.t("auth_error_generic")
            return
        }
        if let error = await SharedKitBridge.signIn(email: email, password: password) {
            authError = error
            return
        }
        await refreshSessionFromShared()
        await SharedKitBridge.refreshVehicles(into: self)
    }

    func register(email: String, password: String, name: String?) async {
        authLoading = true
        authError = nil
        defer { authLoading = false }
        guard !email.isEmpty, password.count >= 6 else {
            authError = L.t("auth_error_weak_password")
            return
        }
        if let error = await SharedKitBridge.register(email: email, password: password, displayName: name) {
            authError = error
            return
        }
        await refreshSessionFromShared()
        await SharedKitBridge.refreshVehicles(into: self)
    }

    func sendPasswordReset(email: String) async -> String? {
        await SharedKitBridge.sendPasswordReset(email: email)
    }

    func signInWithGoogle(idToken: String, accessToken: String?) async {
        authLoading = true
        authError = nil
        defer { authLoading = false }
        if let error = await SharedKitBridge.signInWithGoogle(idToken: idToken, accessToken: accessToken) {
            authError = error
            return
        }
        await completeSuccessfulAuth()
    }

    func completeSuccessfulAuth() async {
        await refreshSessionFromShared()
        await SharedKitBridge.refreshVehicles(into: self)
        showAuthGate = false
        pendingAuthRoute = nil
    }

    func signOut() async {
        await SharedKitBridge.signOut()
        isAuthenticated = false
        isAdmin = false
        isPro = false
        displayName = ""
        accountEmail = ""
        vehicles = []
        sharedVehicles = []
    }

    func addVehicle(from info: VehicleBasicInfo) async {
        let vehicle: Vehicle
        if let entry = info.catalogEntry {
            vehicle = IosKoinInitKt.iosVehicleFromCatalogEntry(
                entry: entry,
                year: info.year,
                name: info.name,
                licensePlate: info.licensePlate,
                odometerKm: info.odometerKm
            )
        } else {
            vehicle = VehicleMapper.fromBasicInfo(info)
        }
        await SharedKitBridge.saveVehicle(vehicle)
        await SharedKitBridge.refreshVehicles(into: self)
    }

    func saveVehicle(_ vehicle: Vehicle) async {
        await SharedKitBridge.saveVehicle(vehicle)
        await SharedKitBridge.refreshVehicles(into: self)
    }

    func deleteVehicle(id: String) async {
        await SharedKitBridge.deleteVehicle(id: id)
        await SharedKitBridge.refreshVehicles(into: self)
    }

    func purchasePro() async {
        guard isAuthenticated else {
            billingError = L.t("settings_pro_sign_in_required")
            return
        }
        isPurchasing = true
        billingError = nil
        defer { isPurchasing = false }
        do {
            let success = try await StoreKitService.shared.purchasePro(appState: self)
            if success {
                await refreshSessionFromShared()
            }
        } catch {
            billingError = StoreKitService.friendlyMessage(for: error)
        }
    }

    func restorePro() async {
        isPurchasing = true
        billingError = nil
        defer { isPurchasing = false }
        do {
            let success = try await StoreKitService.shared.restorePurchases(appState: self)
            if success {
                await refreshSessionFromShared()
            }
        } catch {
            billingError = StoreKitService.friendlyMessage(for: error)
        }
    }

    func applyAdminRefresh() async {
        await refreshSessionFromShared()
        await SharedKitBridge.refreshVehicles(into: self)
    }
}

struct VehicleItem: Identifiable, Hashable {
    let id: String
    var name: String
    var brand: String
    var model: String
    var licensePlate: String
    var year: String
    var odometerKm: String
    var vehicle: Vehicle?

    var displayTitle: String {
        name.isEmpty ? [brand, model].filter { !$0.isEmpty }.joined(separator: " ") : name
    }

    var displaySubtitle: String {
        [licensePlate, year, odometerKm.isEmpty ? "" : "\(odometerKm) km"]
            .filter { !$0.isEmpty }
            .joined(separator: " · ")
    }

    func hash(into hasher: inout Hasher) { hasher.combine(id) }
    static func == (lhs: VehicleItem, rhs: VehicleItem) -> Bool { lhs.id == rhs.id }
}

enum AppRoute: Hashable {
    case calculator, vehicles, settings, account, admin, login, register, forgotPassword
}

enum CalculatorCatalog {
    static let displayOrder: [CalculatorId] = [
        .timing, .portArea, .ignition, .dcCable, .fluid, .compression,
        .squishBand, .meanPressure, .gear, .exhaust, .counterweight,
        .variatorWeight, .fuelMix, .carbJet, .dynoInertia, .vehicleDynamics,
    ]
}

enum ProModuleMapping {
    static func fromCalculator(_ id: CalculatorId) -> String { id.name }
}
