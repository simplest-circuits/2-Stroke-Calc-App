import SwiftUI
import sharedKit

struct VehiclesScreen: View {
    @EnvironmentObject private var appState: AppState
    @Environment(\.themeColors) private var colors
    @State private var showAdd = false

    var body: some View {
        NavigationStack {
            ZStack(alignment: .bottomTrailing) {
                ScrollView {
                    VStack(alignment: .leading, spacing: 12) {
                        Text(S.navVehicles).font(.title2.bold())
                        if appState.vehicles.isEmpty {
                            Text(S.vehiclesEmpty)
                                .foregroundStyle(colors.onSurfaceVariant)
                                .padding(.top, 40)
                                .frame(maxWidth: .infinity)
                        } else {
                            ForEach(appState.vehicles) { vehicle in
                                Button {
                                    appState.vehicleDetailId = vehicle.id
                                } label: {
                                    vehicleCard(vehicle)
                                }
                                .buttonStyle(.plain)
                            }
                        }
                    }
                    .padding(20)
                }
                if appState.hasVehiclesAccess() {
                    Button {
                        showAdd = true
                    } label: {
                        Image(systemName: "plus")
                            .font(.title2.bold())
                            .foregroundStyle(colors.onPrimary)
                            .frame(width: 56, height: 56)
                            .background(colors.primary)
                            .clipShape(Circle())
                    }
                    .walkthroughAnchor(WalkthroughTargetIds.addVehicleFab)
                    .padding(24)
                } else {
                    ProReadOnlyBanner {
                        appState.proUpsellModule = "VEHICLES"
                    }
                    .padding(24)
                }
            }
            .background(colors.background)
            .navigationDestination(item: Binding(
                get: { appState.vehicleDetailId.map { VehicleRoute(id: $0) } },
                set: { appState.vehicleDetailId = $0?.id }
            )) { route in
                VehicleDetailScreenExpanded(vehicleId: route.id)
            }
            .sheet(isPresented: $showAdd) {
                AddVehicleDialog(catalogBrands: SharedKitBridge.catalogBrands()) { info in
                    Task { await appState.addVehicle(from: info) }
                }
            }
        }
    }

    private func vehicleCard(_ vehicle: VehicleItem) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(vehicle.displayTitle).font(.headline)
            Text(vehicle.displaySubtitle).font(.subheadline).foregroundStyle(colors.onSurfaceVariant)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
        .background(colors.surface)
        .clipShape(RoundedRectangle(cornerRadius: AppTheme.containerCornerRadius))
        .overlay(RoundedRectangle(cornerRadius: AppTheme.containerCornerRadius).stroke(colors.outline.opacity(0.3)))
    }
}

private struct VehicleRoute: Identifiable, Hashable {
    let id: String
}

struct VehicleFuelLogScreen: View {
    @EnvironmentObject private var appState: AppState
    @Environment(\.themeColors) private var colors
    let vehicleId: String
    @State private var showAdd = false
    @State private var editEntry: FuelLogEntry?

    var body: some View {
        CalculatorScaffold {
            CalculatorHeader(title: "Tankbuch", subtitle: "Spritverbrauch erfassen")
            if let vehicle = appState.vehicle(for: vehicleId) {
                ForEach(Array(vehicle.fuelLog.enumerated()), id: \.offset) { index, entry in
                    fuelRow(entry) {
                        editEntry = entry
                        showAdd = true
                    } onDelete: {
                        Task { await deleteEntry(at: index) }
                    }
                }
                if !vehicle.fuelLog.isEmpty {
                    ResultCard(title: "Verbrauch", lines: consumptionLines(for: vehicle))
                }
            }
            PrimaryButton(title: "Eintrag hinzufügen") { editEntry = nil; showAdd = true }
        }
        .sheet(isPresented: $showAdd) {
            FuelLogEntrySheet(entry: editEntry) { saved in
                Task { await upsertEntry(saved) }
            }
        }
    }

    private func fuelRow(_ entry: FuelLogEntry, onEdit: @escaping () -> Void, onDelete: @escaping () -> Void) -> some View {
        HStack {
            VStack(alignment: .leading) {
                Text(entry.date).font(.subheadline.bold())
                Text("\(entry.liters) l · \(entry.odometerKm) km").font(.caption).foregroundStyle(colors.onSurfaceVariant)
            }
            Spacer()
            Button("Bearbeiten", action: onEdit).font(.caption)
            Button("Löschen", role: .destructive, action: onDelete).font(.caption)
        }
        .padding(.vertical, 4)
    }

    private func consumptionLines(for vehicle: Vehicle) -> [String] {
        let entries = vehicle.fuelLog
        guard entries.count >= 2 else { return ["Mindestens 2 Einträge für Verbrauch"] }
        let sorted = entries.sorted { parseDouble($0.odometerKm) ?? 0 < parseDouble($1.odometerKm) ?? 0 }
        guard let first = sorted.first, let last = sorted.last,
              let km = (parseDouble(last.odometerKm) ?? 0) - (parseDouble(first.odometerKm) ?? 0),
              km > 0 else { return [] }
        let liters = sorted.dropFirst().compactMap { parseDouble($0.liters) }.reduce(0, +)
        let per100 = liters / km * 100
        return [String(format: "Ø Verbrauch: %.1f l/100 km", per100)]
    }

    private func upsertEntry(_ entry: FuelLogEntry) async {
        guard var vehicle = appState.vehicle(for: vehicleId) else { return }
        var log = vehicle.fuelLog
        if let index = log.firstIndex(where: { $0.id == entry.id }) {
            log[index] = entry
        } else {
            log.append(entry)
        }
        vehicle = IosKoinInitKt.iosUpdateFuelLog(vehicle: vehicle, entries: log)
        if let odometer = parseDouble(entry.odometerKm), odometer > 0 {
            vehicle = vehicle.copy(currentOdometerKm: entry.odometerKm)
        }
        await appState.saveVehicle(vehicle)
    }

    private func deleteEntry(at index: Int) async {
        guard var vehicle = appState.vehicle(for: vehicleId) else { return }
        var log = vehicle.fuelLog
        guard log.indices.contains(index) else { return }
        log.remove(at: index)
        vehicle = IosKoinInitKt.iosUpdateFuelLog(vehicle: vehicle, entries: log)
        await appState.saveVehicle(vehicle)
    }
}

struct VehicleCostOverviewScreen: View {
    @EnvironmentObject private var appState: AppState
    let vehicleId: String
    @State private var showAdd = false

    var body: some View {
        CalculatorScaffold {
            CalculatorHeader(title: "Kostenübersicht", subtitle: "Wartung und Betriebskosten")
            if let vehicle = appState.vehicle(for: vehicleId) {
                let lines = costSummary(for: vehicle)
                ResultCard(title: S.resultTitle, lines: lines.isEmpty ? ["Noch keine Einträge"] : lines)
                ForEach(Array(vehicle.maintenanceLog.enumerated()), id: \.offset) { index, entry in
                    HStack {
                        VStack(alignment: .leading) {
                            Text(entry.description).font(.subheadline)
                            Text("\(entry.date) · \(entry.cost) €").font(.caption)
                        }
                        Spacer()
                        Button("Löschen") {
                            Task { await deleteMaintenance(at: index) }
                        }
                        .font(.caption)
                    }
                }
            }
            PrimaryButton(title: "Kostenposition hinzufügen") { showAdd = true }
        }
        .sheet(isPresented: $showAdd) {
            MaintenanceEntrySheet { entry in
                Task { await addMaintenance(entry) }
            }
        }
    }

    private func costSummary(for vehicle: Vehicle) -> [String] {
        let fuel = vehicle.fuelLog.compactMap { parseDouble($0.price) }.reduce(0, +)
        let maintenance = vehicle.maintenanceLog.compactMap { parseDouble($0.cost) }.reduce(0, +)
        var lines: [String] = []
        if fuel > 0 { lines.append(String(format: "Kraftstoff: %.2f €", fuel)) }
        if maintenance > 0 { lines.append(String(format: "Wartung: %.2f €", maintenance)) }
        if fuel + maintenance > 0 { lines.append(String(format: "Gesamt: %.2f €", fuel + maintenance)) }
        return lines
    }

    private func addMaintenance(_ entry: MaintenanceEntry) async {
        guard var vehicle = appState.vehicle(for: vehicleId) else { return }
        var log = vehicle.maintenanceLog
        log.append(entry)
        vehicle = IosKoinInitKt.iosUpdateMaintenanceLog(vehicle: vehicle, entries: log)
        await appState.saveVehicle(vehicle)
    }

    private func deleteMaintenance(at index: Int) async {
        guard var vehicle = appState.vehicle(for: vehicleId) else { return }
        var log = vehicle.maintenanceLog
        guard log.indices.contains(index) else { return }
        log.remove(at: index)
        vehicle = IosKoinInitKt.iosUpdateMaintenanceLog(vehicle: vehicle, entries: log)
        await appState.saveVehicle(vehicle)
    }
}

private struct FuelLogEntrySheet: View {
    @Environment(\.dismiss) private var dismiss
    let entry: FuelLogEntry?
    let onSave: (FuelLogEntry) -> Void
    @State private var date = ""
    @State private var liters = ""
    @State private var odometer = ""
    @State private var price = ""

    var body: some View {
        NavigationStack {
            Form {
                TextField("Datum", text: $date)
                TextField("Liter", text: $liters)
                TextField("Kilometerstand", text: $odometer)
                TextField("Preis (€)", text: $price)
            }
            .navigationTitle("Tankbuch-Eintrag")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button(S.cancel) { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button(S.save) {
                        onSave(FuelLogEntry(
                            id: entry?.id ?? UUID().uuidString,
                            date: date,
                            odometerKm: odometer,
                            liters: liters,
                            price: price
                        ))
                        dismiss()
                    }
                }
            }
            .onAppear {
                date = entry?.date ?? ""
                liters = entry?.liters ?? ""
                odometer = entry?.odometerKm ?? ""
                price = entry?.price ?? ""
            }
        }
    }
}

private struct MaintenanceEntrySheet: View {
    @Environment(\.dismiss) private var dismiss
    let onSave: (MaintenanceEntry) -> Void
    @State private var date = ""
    @State private var description = ""
    @State private var cost = ""

    var body: some View {
        NavigationStack {
            Form {
                TextField("Datum", text: $date)
                TextField("Beschreibung", text: $description)
                TextField("Kosten (€)", text: $cost)
            }
            .navigationTitle("Kostenposition")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button(S.cancel) { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button(S.save) {
                        onSave(MaintenanceEntry(
                            date: date,
                            description: description,
                            cost: cost
                        ))
                        dismiss()
                    }
                }
            }
        }
    }
}

private func parseDouble(_ value: String) -> Double? {
    Double(value.replacingOccurrences(of: ",", with: "."))
}
