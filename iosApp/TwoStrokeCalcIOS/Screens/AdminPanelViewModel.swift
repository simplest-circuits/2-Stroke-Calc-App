import SwiftUI
import sharedKit

@MainActor
final class AdminPanelViewModel: ObservableObject {
    @Published var loading = false
    @Published var error: String?
    @Published var message: String?
    @Published var users: [AdminUserDto] = []
    @Published var settings: AdminSettingsDto?
    @Published var searchQuery = ""
    @Published var pushTitle = ""
    @Published var pushBody = ""
    @Published var demoVehiclesEnabled = true
    @Published var calculatorToggles: [String: Bool] = [:]
    @Published var proModuleToggles: [String: Bool] = [:]

    var filteredUsers: [AdminUserDto] {
        let q = searchQuery.trimmingCharacters(in: .whitespaces).lowercased()
        guard !q.isEmpty else { return users }
        return users.filter {
            ($0.email?.lowercased().contains(q) ?? false) ||
            ($0.displayName?.lowercased().contains(q) ?? false) ||
            $0.id.lowercased().contains(q)
        }
    }

    func load() async {
        loading = true
        error = nil
        let snapshot = await IosKoinInitKt.iosAdminLoadState()
        loading = snapshot.loading
        error = snapshot.error
        users = snapshot.users
        settings = snapshot.settings
        if let settings {
            demoVehiclesEnabled = settings.demoVehiclesEnabled
            calculatorToggles = settings.calculatorAvailability
            proModuleToggles = settings.proModules
        } else {
            seedDefaultToggles()
        }
        loading = false
    }

    func saveSettings() async {
        let current = AdminSettingsDto(
            maintenanceMode: settings?.maintenanceMode ?? false,
            debugMode: settings?.debugMode ?? false,
            emailNotifications: settings?.emailNotifications ?? false,
            demoVehiclesEnabled: demoVehiclesEnabled,
            calculatorAvailability: calculatorToggles,
            proModules: proModuleToggles
        )
        loading = true
        if let err = await IosKoinInitKt.iosAdminSaveSettings(settings: current) {
            error = err
        } else {
            message = "Einstellungen gespeichert"
            settings = current
        }
        loading = false
    }

    func toggleCalculator(_ name: String, enabled: Bool) {
        calculatorToggles[name] = enabled
    }

    func toggleProModule(_ name: String, enabled: Bool) {
        proModuleToggles[name] = enabled
    }

    func banUser(_ user: AdminUserDto, banned: Bool) async {
        if let err = await IosKoinInitKt.iosAdminSetUserBanned(userId: user.id, banned: banned) {
            error = err
        } else {
            await load()
        }
    }

    func setPro(_ user: AdminUserDto, isPro: Bool) async {
        if let err = await IosKoinInitKt.iosAdminSetUserPro(userId: user.id, isPro: isPro) {
            error = err
        } else {
            await load()
        }
    }

    func sendPush() async {
        loading = true
        if let err = await IosKoinInitKt.iosAdminSendPush(title: pushTitle, body: pushBody) {
            error = err
        } else {
            message = "Push gesendet"
            pushTitle = ""
            pushBody = ""
        }
        loading = false
    }

    private func seedDefaultToggles() {
        for id in CalculatorCatalog.displayOrder {
            calculatorToggles[id.name] = true
        }
        for module in IosKoinInitKt.iosProModuleNames() as [String] {
            proModuleToggles[module] = true
        }
    }
}
