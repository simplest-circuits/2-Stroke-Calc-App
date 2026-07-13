import SwiftUI
import sharedKit

struct VehicleDetailScreenExpanded: View {
    @EnvironmentObject private var appState: AppState
    @Environment(\.themeColors) private var colors
    @Environment(\.dismiss) private var dismiss
    let vehicleId: String
    @State private var selectedTab: VehicleDetailTab = .overview
    @State private var showDeleteConfirm = false
    @State private var showMaintenanceSheet = false
    @State private var draft: VehicleDraft = .empty

    private var canEdit: Bool { appState.hasVehiclesAccess() }

    var body: some View {
        VStack(spacing: 0) {
            header
            tabBar
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    tabContent
                    if canEdit {
                        PrimaryButton(title: S.save) {
                            Task { await saveChanges() }
                        }
                    }
                }
                .padding(20)
            }
        }
        .background(colors.background)
        .onAppear(perform: loadFromShared)
        .alert(L.t("vehicles_delete_confirm_title"), isPresented: $showDeleteConfirm) {
            Button(L.t("delete"), role: .destructive) {
                Task { await appState.deleteVehicle(id: vehicleId); dismiss() }
            }
            Button(S.cancel, role: .cancel) {}
        }
        .sheet(isPresented: $showMaintenanceSheet) {
            MaintenanceEntrySheet { entry in
                Task { await addMaintenance(entry) }
            }
        }
    }

    private var header: some View {
        HStack(spacing: 12) {
            Button { dismiss() } label: {
                Image(systemName: "chevron.left")
                    .foregroundStyle(colors.primary)
            }
            Text(draft.displayTitle)
                .font(.headline)
                .lineLimit(1)
            Spacer()
            NavigationLink {
                VehicleFuelLogScreen(vehicleId: vehicleId)
            } label: {
                Image(systemName: "fuelpump.fill")
                    .foregroundStyle(colors.primary)
            }
            .buttonStyle(.plain)
            .accessibilityLabel(L.t("vehicles_quick_fuel_log"))
            Button {
                if canEdit {
                    showMaintenanceSheet = true
                } else {
                    appState.proUpsellModule = "VEHICLES"
                }
            } label: {
                Image(systemName: "wrench.and.screwdriver.fill")
                    .foregroundStyle(colors.primary)
            }
            .buttonStyle(.plain)
            .accessibilityLabel(L.t("vehicles_tab_maintenance"))
            if canEdit {
                Button(role: .destructive) { showDeleteConfirm = true } label: {
                    Image(systemName: "trash")
                }
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 8)
    }

    private var tabBar: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(VehicleDetailTab.allCases) { tab in
                    Button { selectedTab = tab } label: {
                        Text(tab.title)
                            .font(.caption.weight(selectedTab == tab ? .bold : .regular))
                            .padding(.horizontal, 12)
                            .padding(.vertical, 8)
                            .background(selectedTab == tab ? colors.primaryContainer : colors.surfaceVariant.opacity(0.4))
                            .clipShape(Capsule())
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.horizontal, 16)
        }
    }

    @ViewBuilder
    private var tabContent: some View {
        switch selectedTab {
        case .overview:
            section(L.t("vehicles_tab_overview")) {
                Text([draft.licensePlate, draft.year, draft.odometer.isEmpty ? "" : "\(draft.odometer) km"].filter { !$0.isEmpty }.joined(separator: " · "))
                if !draft.notes.isEmpty { Text(draft.notes).foregroundStyle(colors.onSurfaceVariant) }
            }
        case .basic:
            section(L.t("vehicles_tab_basic")) {
                field(L.t("vehicles_field_name"), $draft.name)
                field(L.t("vehicles_field_brand"), $draft.brand)
                field(L.t("vehicles_field_model"), $draft.model)
                field(L.t("vehicles_field_license_plate"), $draft.licensePlate)
                field(L.t("vehicles_field_year"), $draft.year)
                field(L.t("vehicles_field_odometer"), $draft.odometer)
                field(L.t("vehicles_section_notes"), $draft.notes)
            }
        case .engine:
            section(L.t("vehicles_tab_engine")) {
                field(L.t("compression_result_displacement"), $draft.engineDisplacement)
                field(L.t("vehicles_field_bore"), $draft.engineBore)
                field(L.t("pt_stroke"), $draft.engineStroke)
                field(L.t("calculator_tab_compression"), $draft.engineCompression)
                field(L.t("vehicles_maint_exhaust"), $draft.engineExhaust)
                field(L.t("vehicles_field_engine_notes"), $draft.engineNotes)
            }
        case .tuning:
            section(L.t("vehicles_tab_tuning")) {
                field(L.t("vehicles_maint_carb"), $draft.carbType)
                field(L.t("vehicles_field_main_jet"), $draft.mainJet)
                field(L.t("vehicles_maint_fuel_mix"), $draft.fuelMix)
                field(L.t("vehicles_maint_spark"), $draft.sparkPlug)
                field(L.t("vehicles_section_ignition"), $draft.ignitionTiming)
            }
        case .drivetrain:
            section(L.t("vehicles_tab_drivetrain")) {
                field(L.t("vehicles_field_variator_brand"), $draft.variatorBrand)
                field(L.t("vehicles_field_variator_weights"), $draft.variatorWeights)
                field(L.t("vehicles_field_chain_type"), $draft.chainType)
                field(L.t("vehicles_tab_drivetrain"), $draft.driveType)
            }
        case .chassis:
            section(L.t("vehicles_tab_chassis")) {
                field(L.t("vehicles_field_front_tire"), $draft.frontTire)
                field(L.t("vehicles_field_rear_tire"), $draft.rearTire)
                field(L.t("vehicles_field_front_brake"), $draft.frontBrake)
                field(L.t("vehicles_field_rear_brake"), $draft.rearBrake)
            }
        case .electrical:
            section(L.t("vehicles_tab_electrical")) {
                field(L.t("vehicles_field_battery"), $draft.battery)
                field(L.t("vehicles_field_regulator"), $draft.regulator)
                field(L.t("vehicles_field_ignition_coil"), $draft.ignitionCoil)
            }
        case .maintenance:
            section(L.t("vehicles_tab_maintenance")) {
                if draft.maintenancePreview.isEmpty {
                    Text(L.t("vehicles_maintenance_empty")).foregroundStyle(colors.onSurfaceVariant)
                } else {
                    ForEach(draft.maintenancePreview, id: \.self) { line in
                        Text(line).font(.caption)
                    }
                }
            }
        }
    }

    private func section<Content: View>(_ title: String, @ViewBuilder content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title).font(.headline)
            content()
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
        .background(colors.surface)
        .clipShape(RoundedRectangle(cornerRadius: AppTheme.containerCornerRadius))
    }

    @ViewBuilder
    private func field(_ label: String, _ text: Binding<String>) -> some View {
        if canEdit {
            AppOutlinedField(label: label, text: text)
        } else {
            HStack {
                Text(label).foregroundStyle(colors.onSurfaceVariant)
                Spacer()
                Text(text.wrappedValue.isEmpty ? L.t("gear_result_table_no_jump") : text.wrappedValue)
            }
            .font(.subheadline)
        }
    }

    private func loadFromShared() {
        guard let vehicle = appState.vehicle(for: vehicleId) else { return }
        draft = VehicleDraft.from(vehicle)
    }

    private func saveChanges() async {
        guard var vehicle = appState.vehicle(for: vehicleId) else { return }
        vehicle = draft.apply(to: vehicle)
        await appState.saveVehicle(vehicle)
    }

    private func addMaintenance(_ entry: MaintenanceEntry) async {
        guard var vehicle = appState.vehicle(for: vehicleId) else { return }
        var log = vehicle.maintenanceLog
        log.append(entry)
        vehicle = IosKoinInitKt.iosUpdateMaintenanceLog(vehicle: vehicle, entries: log)
        await appState.saveVehicle(vehicle)
        draft.maintenancePreview = vehicle.maintenanceLog.map { "\($0.date) · \($0.entryDescription)" }
    }
}

private struct VehicleDraft {
    var name = ""
    var brand = ""
    var model = ""
    var licensePlate = ""
    var year = ""
    var odometer = ""
    var notes = ""
    var engineDisplacement = ""
    var engineBore = ""
    var engineStroke = ""
    var engineCompression = ""
    var engineExhaust = ""
    var engineNotes = ""
    var carbType = ""
    var mainJet = ""
    var fuelMix = ""
    var sparkPlug = ""
    var ignitionTiming = ""
    var variatorBrand = ""
    var variatorWeights = ""
    var chainType = ""
    var driveType = ""
    var frontTire = ""
    var rearTire = ""
    var frontBrake = ""
    var rearBrake = ""
    var battery = ""
    var regulator = ""
    var ignitionCoil = ""
    var maintenancePreview: [String] = []

    var displayTitle: String {
        name.isEmpty ? [brand, model].filter { !$0.isEmpty }.joined(separator: " ") : name
    }

    static let empty = VehicleDraft()

    static func from(_ vehicle: Vehicle) -> VehicleDraft {
        VehicleDraft(
            name: vehicle.name,
            brand: vehicle.brand,
            model: vehicle.model,
            licensePlate: vehicle.licensePlate,
            year: vehicle.year,
            odometer: vehicle.currentOdometerKm,
            notes: vehicle.notes,
            engineDisplacement: vehicle.engine.displacementCc,
            engineBore: vehicle.engine.boreMm,
            engineStroke: vehicle.engine.strokeMm,
            engineCompression: vehicle.engine.compressionRatio,
            engineExhaust: vehicle.engine.exhaustSystem,
            engineNotes: vehicle.engine.engineNotes,
            carbType: vehicle.carbIgnition.carbType,
            mainJet: vehicle.carbIgnition.mainJet,
            fuelMix: vehicle.carbIgnition.fuelMixRatio,
            sparkPlug: vehicle.carbIgnition.sparkPlug,
            ignitionTiming: vehicle.carbIgnition.ignitionTimingDeg,
            variatorBrand: vehicle.drivetrain.variatorBrand,
            variatorWeights: vehicle.drivetrain.variatorWeightsG,
            chainType: vehicle.drivetrain.chainType,
            driveType: vehicle.drivetrain.driveType,
            frontTire: vehicle.chassis.frontTire,
            rearTire: vehicle.chassis.rearTire,
            frontBrake: vehicle.chassis.frontBrake,
            rearBrake: vehicle.chassis.rearBrake,
            battery: vehicle.electrical.battery,
            regulator: vehicle.electrical.regulator,
            ignitionCoil: vehicle.electrical.ignitionCoil,
            maintenancePreview: vehicle.maintenanceLog.map { "\($0.date) · \($0.entryDescription)" }
        )
    }

    func apply(to vehicle: Vehicle) -> Vehicle {
        IosKoinInitKt.iosApplyVehicleDraft(
            vehicle: vehicle,
            name: name,
            brand: brand,
            model: model,
            licensePlate: licensePlate,
            year: year,
            odometer: odometer,
            notes: notes,
            engineDisplacement: engineDisplacement,
            engineBore: engineBore,
            engineStroke: engineStroke,
            engineCompression: engineCompression,
            engineExhaust: engineExhaust,
            engineNotes: engineNotes,
            carbType: carbType,
            mainJet: mainJet,
            fuelMix: fuelMix,
            sparkPlug: sparkPlug,
            ignitionTiming: ignitionTiming,
            driveType: driveType,
            variatorBrand: variatorBrand,
            variatorWeights: variatorWeights,
            chainType: chainType,
            frontTire: frontTire,
            rearTire: rearTire,
            frontBrake: frontBrake,
            rearBrake: rearBrake,
            battery: battery,
            regulator: regulator,
            ignitionCoil: ignitionCoil
        )
    }
}

enum VehicleDetailTab: String, CaseIterable, Identifiable {
    case overview, basic, engine, tuning, drivetrain, chassis, electrical, maintenance
    var id: String { rawValue }

    var title: String {
        switch self {
        case .overview: return L.t("vehicles_tab_overview")
        case .basic: return L.t("vehicles_tab_basic")
        case .engine: return L.t("vehicles_tab_engine")
        case .tuning: return L.t("vehicles_tab_tuning")
        case .drivetrain: return L.t("vehicles_tab_drivetrain")
        case .chassis: return L.t("vehicles_tab_chassis")
        case .electrical: return L.t("vehicles_tab_electrical")
        case .maintenance: return L.t("vehicles_tab_maintenance")
        }
    }
}

struct VehicleDetailScreen: View {
    let vehicleId: String
    var body: some View { VehicleDetailScreenExpanded(vehicleId: vehicleId) }
}
