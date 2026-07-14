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
            CalculatorHeader(title: L.t("vehicles_quick_fuel_log"), subtitle: L.t("vehicles_fuel_log_add"))
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
                    ResultCard(title: L.t("vehicles_fuel_log_consumption"), lines: consumptionLines(for: vehicle))
                }
            }
            PrimaryButton(title: L.t("vehicles_maintenance_add")) { editEntry = nil; showAdd = true }
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
            Button(action: onEdit) {
                Image(systemName: "square.and.pencil")
                    .foregroundStyle(colors.primary)
            }
            .buttonStyle(.plain)
            .accessibilityLabel(L.t("vehicles_overview_edit"))
            Button(role: .destructive, action: onDelete) {
                Image(systemName: "trash")
            }
            .buttonStyle(.plain)
            .accessibilityLabel(S.delete)
        }
        .padding(.vertical, 4)
    }

    private func consumptionLines(for vehicle: Vehicle) -> [String] {
        let entries = vehicle.fuelLog
        guard entries.count >= 2 else { return [L.t("vehicles_fuel_log_average_hint")] }
        let sorted = entries.sorted { parseDouble($0.odometerKm) ?? 0 < parseDouble($1.odometerKm) ?? 0 }
        guard let first = sorted.first, let last = sorted.last else { return [] }
        let km = (parseDouble(last.odometerKm) ?? 0) - (parseDouble(first.odometerKm) ?? 0)
        guard km > 0 else { return [] }
        let liters = sorted.dropFirst().compactMap { parseDouble($0.liters) }.reduce(0, +)
        let per100 = liters / km * 100
        return [L.tf("vehicles_fuel_log_average_value", fmt(per100, decimals: 1), L.t("vehicles_fuel_log_unit_km"))]
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
        if parseDouble(entry.odometerKm) != nil, !entry.odometerKm.isEmpty {
            vehicle = IosKoinInitKt.iosVehicleWithOdometer(vehicle: vehicle, odometerKm: entry.odometerKm)
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
                TextField(L.t("vehicles_maintenance_date"), text: $date)
                TextField(L.t("vehicles_fuel_log_liters"), text: $liters)
                TextField(L.t("vehicles_field_odometer"), text: $odometer)
                TextField(L.t("vehicles_fuel_log_price"), text: $price)
            }
            .navigationTitle(L.t("vehicles_fuel_log_edit"))
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button(S.cancel) { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button(S.save) {
                        onSave(IosKoinInitKt.iosNewFuelLogEntry(
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

struct MaintenanceEntrySheet: View {
    @Environment(\.dismiss) private var dismiss
    let onSave: (MaintenanceEntry) -> Void
    @State private var date = ""
    @State private var description = ""
    @State private var cost = ""

    var body: some View {
        NavigationStack {
            Form {
                TextField(L.t("vehicles_maintenance_date"), text: $date)
                TextField(L.t("vehicles_maintenance_description"), text: $description)
                TextField(L.t("vehicles_maintenance_cost"), text: $cost)
            }
            .navigationTitle(L.t("vehicles_maintenance_cost"))
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button(S.cancel) { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button(S.save) {
                        onSave(IosKoinInitKt.iosNewMaintenanceEntry(
                            date: date,
                            entryDescription: description,
                            cost: cost
                        ))
                        dismiss()
                    }
                }
            }
        }
    }
}
