import SwiftUI
import sharedKit

struct VehicleDetailScreenExpanded: View {
    @EnvironmentObject private var appState: AppState
    @Environment(\.themeColors) private var colors
    @Environment(\.dismiss) private var dismiss
    let vehicleId: String
    @State private var selectedTab: VehicleDetailTab = .overview
    @State private var showDeleteConfirm = false
    @State private var draft: VehicleDraft = .empty

    private var canEdit: Bool { appState.hasVehiclesAccess() }

    var body: some View {
        VStack(spacing: 0) {
            header
            tabBar
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    tabContent
                    NavigationLink("Tankbuch") { VehicleFuelLogScreen(vehicleId: vehicleId) }
                    NavigationLink("Kosten") { VehicleCostOverviewScreen(vehicleId: vehicleId) }
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
        .alert("Fahrzeug löschen?", isPresented: $showDeleteConfirm) {
            Button("Löschen", role: .destructive) {
                Task { await appState.deleteVehicle(id: vehicleId); dismiss() }
            }
            Button(S.cancel, role: .cancel) {}
        }
    }

    private var header: some View {
        HStack {
            Button { dismiss() } label: { Image(systemName: "chevron.left") }
            Text(draft.displayTitle).font(.headline).lineLimit(1)
            Spacer()
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
            section("Kurzinfo") {
                Text([draft.licensePlate, draft.year, draft.odometer.isEmpty ? "" : "\(draft.odometer) km"].filter { !$0.isEmpty }.joined(separator: " · "))
                if !draft.notes.isEmpty { Text(draft.notes).foregroundStyle(colors.onSurfaceVariant) }
            }
        case .basic:
            section("Stammdaten") {
                field("Name", $draft.name)
                field("Marke", $draft.brand)
                field("Modell", $draft.model)
                field("Kennzeichen", $draft.licensePlate)
                field("Baujahr", $draft.year)
                field("KM-Stand", $draft.odometer)
                field("Notizen", $draft.notes)
            }
        case .engine:
            section("Motor") {
                field("Hubraum", $draft.engineDisplacement)
                field("Bohrung", $draft.engineBore)
                field("Hub", $draft.engineStroke)
                field("Verdichtung", $draft.engineCompression)
                field("Auspuff", $draft.engineExhaust)
                field("Motornotizen", $draft.engineNotes)
            }
        case .tuning:
            section("Vergaser/Zündung") {
                field("Vergaser", $draft.carbType)
                field("Hauptdüse", $draft.mainJet)
                field("Gemisch", $draft.fuelMix)
                field("Zündkerze", $draft.sparkPlug)
                field("Zündung", $draft.ignitionTiming)
            }
        case .drivetrain:
            section("Antrieb") {
                field("Variator", $draft.variatorBrand)
                field("Gewichte", $draft.variatorWeights)
                field("Kette", $draft.chainType)
                field("Antrieb", $draft.driveType)
            }
        case .chassis:
            section("Fahrwerk") {
                field("Reifen vorne", $draft.frontTire)
                field("Reifen hinten", $draft.rearTire)
                field("Bremsen vorne", $draft.frontBrake)
                field("Bremsen hinten", $draft.rearBrake)
            }
        case .electrical:
            section("Elektrik") {
                field("Batterie", $draft.battery)
                field("Regler", $draft.regulator)
                field("Zündspule", $draft.ignitionCoil)
            }
        case .maintenance:
            section("Wartung") {
                if draft.maintenancePreview.isEmpty {
                    Text("Noch keine Wartungseinträge").foregroundStyle(colors.onSurfaceVariant)
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
                Text(text.wrappedValue.isEmpty ? "—" : text.wrappedValue)
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
            maintenancePreview: vehicle.maintenanceLog.map { "\($0.date) · \($0.description)" }
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
        case .overview: return "Übersicht"
        case .basic: return "Stammdaten"
        case .engine: return "Motor"
        case .tuning: return "Vergaser/Zündung"
        case .drivetrain: return "Antrieb"
        case .chassis: return "Fahrwerk"
        case .electrical: return "Elektrik"
        case .maintenance: return "Wartung"
        }
    }
}

struct VehicleDetailScreen: View {
    let vehicleId: String
    var body: some View { VehicleDetailScreenExpanded(vehicleId: vehicleId) }
}
