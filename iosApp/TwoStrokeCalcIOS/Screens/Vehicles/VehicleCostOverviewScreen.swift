import SwiftUI
import sharedKit

private enum CostOverviewLevel {
    case overview
    case groupEntries
    case entryDetail
}

struct VehicleCostOverviewScreen: View {
    @EnvironmentObject private var appState: AppState
    @Environment(\.themeColors) private var colors
    @Environment(\.dismiss) private var dismiss

    let vehicleId: String

    @State private var groupBy: CostOverviewGroupBy = .maintenanceType
    @State private var selectedGroupKey: String?
    @State private var selectedEntryId: String?
    @State private var showEditSheet = false

    private var canEdit: Bool { appState.hasVehiclesAccess() }

    private var vehicle: Vehicle? { appState.vehicle(for: vehicleId) }

    private var summary: VehicleCostSummary? {
        vehicle.map(VehicleCostCalculator.summarize)
    }

    private var costGroups: [MaintenanceCostGroup] {
        guard let items = summary?.maintenanceEntriesWithCost else { return [] }
        return VehicleCostCalculator.groupMaintenanceCosts(items: items, groupBy: groupBy)
    }

    private var selectedGroup: MaintenanceCostGroup? {
        guard let key = selectedGroupKey else { return nil }
        return costGroups.first { $0.key == key }
    }

    private var selectedEntryItem: MaintenanceCostItem? {
        guard let entryId = selectedEntryId,
              let items = summary?.maintenanceEntriesWithCost else { return nil }
        return items.first { $0.entry.id == entryId }
    }

    private var groupEntries: [MaintenanceCostItem] {
        guard let key = selectedGroupKey,
              let items = summary?.maintenanceEntriesWithCost else { return [] }
        return VehicleCostCalculator.itemsInGroup(items, groupBy: groupBy, groupKey: key)
    }

    private var level: CostOverviewLevel {
        if selectedEntryItem != nil { return .entryDetail }
        if selectedGroupKey != nil { return .groupEntries }
        return .overview
    }

    private var navigationTitle: String {
        switch level {
        case .overview:
            return L.t("vehicles_cost_overview_title")
        case .groupEntries:
            if let key = selectedGroupKey {
                return costGroupLabel(groupBy: groupBy, key: key)
            }
            return L.t("vehicles_cost_overview_title")
        case .entryDetail:
            if let entry = selectedEntryItem?.entry {
                return maintenanceCostEntryTitle(entry)
            }
            return L.t("vehicles_cost_overview_title")
        }
    }

    var body: some View {
        CalculatorScaffold {
            if !canEdit {
                ProReadOnlyBanner {
                    appState.proUpsellModule = "VEHICLES"
                }
            }

            if level == .overview, let vehicle {
                Text(vehicleDisplayTitle(vehicle))
                    .font(.title3.weight(.semibold))
                    .foregroundStyle(colors.onSurface)
            }

            if let summary {
                switch level {
                case .overview:
                    overviewContent(summary: summary)
                case .groupEntries:
                    groupEntriesContent
                case .entryDetail:
                    if let item = selectedEntryItem {
                        entryDetailContent(item: item)
                    }
                }
            }
        }
        .navigationTitle(navigationTitle)
        .navigationBarTitleDisplayMode(.inline)
        .navigationBarBackButtonHidden(level != .overview)
        .toolbar {
            if level != .overview {
                ToolbarItem(placement: .topBarLeading) {
                    Button {
                        navigateBack()
                    } label: {
                        Image(systemName: "chevron.left")
                    }
                }
            }
        }
        .sheet(isPresented: $showEditSheet) {
            if let entry = selectedEntryItem?.entry {
                MaintenanceEntryEditSheet(entry: entry) { updated in
                    Task { await updateMaintenance(updated) }
                }
            }
        }
        .onChange(of: summary?.maintenanceEntriesWithCost.count) { _, _ in
            if selectedEntryId != nil, selectedEntryItem == nil {
                selectedEntryId = nil
            }
        }
    }

    @ViewBuilder
    private func overviewContent(summary: VehicleCostSummary) -> some View {
        if !summary.hasAnyCostData {
            Text(L.t("vehicles_cost_overview_empty"))
                .font(.body)
                .foregroundStyle(colors.onSurfaceVariant)
        } else {
            costSummaryCard(summary: summary)

            if !summary.maintenanceEntriesWithCost.isEmpty {
                CalculatorChoiceField(
                    label: L.t("vehicles_cost_overview_group_by"),
                    options: CostOverviewGroupBy.allCases.map { group in
                        (key: group.rawValue, title: costGroupByLabel(group))
                    },
                    selectedKey: groupBy.rawValue,
                    onOptionSelected: { key in
                        guard let selected = CostOverviewGroupBy(rawValue: key), selected != groupBy else { return }
                        groupBy = selected
                        selectedGroupKey = nil
                        selectedEntryId = nil
                    }
                )

                if !costGroups.isEmpty {
                    CalculatorSection(L.t("vehicles_cost_overview_breakdown")) {
                        ForEach(costGroups) { group in
                            costGroupRow(group: group)
                            if group.id != costGroups.last?.id {
                                Divider().opacity(0.35)
                            }
                        }
                    }
                }
            }
        }
    }

    @ViewBuilder
    private var groupEntriesContent: some View {
        if let group = selectedGroup {
            groupSummaryCard(group: group)
        }
        CalculatorSection(L.t("vehicles_cost_overview_entries")) {
            ForEach(groupEntries) { item in
                costEntryRow(item: item)
                if item.id != groupEntries.last?.id {
                    Divider().opacity(0.35)
                }
            }
        }
    }

    @ViewBuilder
    private func entryDetailContent(item: MaintenanceCostItem) -> some View {
        let entry = item.entry
        CalculatorSection(L.t("vehicles_cost_overview_entry_detail")) {
            if !entry.date.isEmpty {
                costInfoRow(label: L.t("vehicles_maintenance_date"), value: entry.date)
            }
            if !entry.odometerKm.isEmpty {
                costInfoRow(label: L.t("vehicles_maintenance_odometer"), value: "\(entry.odometerKm) km")
            }
            costInfoRow(label: L.t("vehicles_maintenance_type"), value: maintenanceTypeLabel(entry.type))
            if !entry.entryDescription.isEmpty {
                costInfoRow(label: L.t("vehicles_maintenance_description"), value: entry.entryDescription)
            }
            costInfoRow(label: L.t("vehicles_maintenance_cost"), value: formatEuro(item.cost))
            if !entry.partsUsed.isEmpty {
                costInfoRow(label: L.t("vehicles_maintenance_parts"), value: entry.partsUsed)
            }
            if !entry.workshop.isEmpty {
                costInfoRow(label: L.t("vehicles_maintenance_workshop"), value: entry.workshop)
            }

            HStack(spacing: 12) {
                Button(L.t("vehicles_maintenance_edit")) {
                    if canEdit {
                        showEditSheet = true
                    } else {
                        appState.proUpsellModule = "VEHICLES"
                    }
                }
                .font(.subheadline.weight(.semibold))
                .foregroundStyle(colors.primary)

                Button(L.t("delete"), role: .destructive) {
                    if canEdit {
                        Task { await deleteMaintenance(entryId: entry.id) }
                    } else {
                        appState.proUpsellModule = "VEHICLES"
                    }
                }
                .font(.subheadline.weight(.semibold))
            }
            .padding(.top, 4)
        }
    }

    private func costSummaryCard(summary: VehicleCostSummary) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(L.t("vehicles_cost_overview_total"))
                .font(.subheadline)
                .foregroundStyle(colors.onSurfaceVariant)
            Text(summary.totalCost.map(formatEuro) ?? L.t("vehicles_cost_overview_unavailable"))
                .font(.title2.bold())
                .foregroundStyle(colors.onSurface)

            Divider().padding(.vertical, 4)

            costSummaryRow(
                label: L.t("vehicles_cost_overview_purchase"),
                value: summary.purchaseCost.map(formatEuro) ?? L.t("vehicles_cost_overview_unavailable")
            )
            costSummaryRow(
                label: L.t("vehicles_cost_overview_maintenance"),
                value: summary.maintenanceEntriesWithCost.isEmpty
                    ? L.t("vehicles_cost_overview_unavailable")
                    : formatEuro(summary.maintenanceTotal)
            )
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(colors.surface)
        .clipShape(RoundedRectangle(cornerRadius: AppTheme.containerCornerRadius))
        .overlay(
            RoundedRectangle(cornerRadius: AppTheme.containerCornerRadius)
                .stroke(colors.outline.opacity(0.35), lineWidth: 1)
        )
    }

    private func groupSummaryCard(group: MaintenanceCostGroup) -> some View {
        HStack {
            Text(L.tf("vehicles_cost_overview_entry_count", String(group.entryCount)))
                .font(.body)
                .foregroundStyle(colors.onSurfaceVariant)
            Spacer()
            Text(formatEuro(group.total))
                .font(.headline)
                .foregroundStyle(colors.onSurface)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(colors.surface)
        .clipShape(RoundedRectangle(cornerRadius: AppTheme.containerCornerRadius))
        .overlay(
            RoundedRectangle(cornerRadius: AppTheme.containerCornerRadius)
                .stroke(colors.outline.opacity(0.35), lineWidth: 1)
        )
    }

    private func costSummaryRow(label: String, value: String) -> some View {
        HStack {
            Text(label)
                .font(.body)
                .foregroundStyle(colors.onSurfaceVariant)
            Spacer()
            Text(value)
                .font(.body.weight(.semibold))
                .foregroundStyle(colors.onSurface)
                .multilineTextAlignment(.trailing)
        }
    }

    private func costInfoRow(label: String, value: String) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(label)
                .font(.caption)
                .foregroundStyle(colors.onSurfaceVariant)
            Text(value)
                .font(.body)
                .foregroundStyle(colors.onSurface)
        }
        .padding(.bottom, 4)
    }

    private func costGroupRow(group: MaintenanceCostGroup) -> some View {
        Button {
            selectedGroupKey = group.key
            selectedEntryId = nil
        } label: {
            HStack(alignment: .center) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(costGroupLabel(groupBy: groupBy, key: group.key))
                        .font(.body)
                        .foregroundStyle(colors.onSurface)
                    Text(L.tf("vehicles_cost_overview_entry_count", String(group.entryCount)))
                        .font(.caption)
                        .foregroundStyle(colors.onSurfaceVariant)
                }
                Spacer()
                HStack(spacing: 4) {
                    Text(formatEuro(group.total))
                        .font(.body.weight(.semibold))
                        .foregroundStyle(colors.onSurface)
                    Image(systemName: "chevron.right")
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(colors.onSurfaceVariant)
                }
            }
            .padding(.vertical, 8)
        }
        .buttonStyle(.plain)
    }

    private func costEntryRow(item: MaintenanceCostItem) -> some View {
        Button {
            selectedEntryId = item.entry.id
        } label: {
            HStack(alignment: .center) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(maintenanceCostEntryTitle(item.entry))
                        .font(.body)
                        .foregroundStyle(colors.onSurface)
                        .lineLimit(1)
                    if !item.entry.entryDescription.isEmpty {
                        Text(item.entry.entryDescription)
                            .font(.caption)
                            .foregroundStyle(colors.onSurfaceVariant)
                            .lineLimit(1)
                    }
                }
                Spacer()
                HStack(spacing: 4) {
                    Text(formatEuro(item.cost))
                        .font(.body.weight(.semibold))
                        .foregroundStyle(colors.onSurface)
                    Image(systemName: "chevron.right")
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(colors.onSurfaceVariant)
                }
            }
            .padding(.vertical, 8)
        }
        .buttonStyle(.plain)
    }

    private func navigateBack() {
        switch level {
        case .entryDetail:
            selectedEntryId = nil
        case .groupEntries:
            selectedGroupKey = nil
            selectedEntryId = nil
        case .overview:
            dismiss()
        }
    }

    private func vehicleDisplayTitle(_ vehicle: Vehicle) -> String {
        if !vehicle.name.isEmpty { return vehicle.name }
        return [vehicle.brand, vehicle.model].filter { !$0.isEmpty }.joined(separator: " ")
    }

    private func updateMaintenance(_ entry: MaintenanceEntry) async {
        guard var vehicle = appState.vehicle(for: vehicleId) else { return }
        let log = vehicle.maintenanceLog.map { $0.id == entry.id ? entry : $0 }
        vehicle = IosKoinInitKt.iosUpdateMaintenanceLog(vehicle: vehicle, entries: log)
        await appState.saveVehicle(vehicle)
    }

    private func deleteMaintenance(entryId: String) async {
        guard var vehicle = appState.vehicle(for: vehicleId) else { return }
        let log = vehicle.maintenanceLog.filter { $0.id != entryId }
        vehicle = IosKoinInitKt.iosUpdateMaintenanceLog(vehicle: vehicle, entries: log)
        await appState.saveVehicle(vehicle)
        selectedEntryId = nil
    }
}

private struct MaintenanceEntryEditSheet: View {
    @Environment(\.dismiss) private var dismiss

    let entry: MaintenanceEntry
    let onSave: (MaintenanceEntry) -> Void

    @State private var date = ""
    @State private var description = ""
    @State private var cost = ""
    @State private var odometer = ""
    @State private var parts = ""
    @State private var workshop = ""

    var body: some View {
        NavigationStack {
            Form {
                TextField(L.t("vehicles_maintenance_date"), text: $date)
                TextField(L.t("vehicles_maintenance_odometer"), text: $odometer)
                TextField(L.t("vehicles_maintenance_description"), text: $description)
                TextField(L.t("vehicles_maintenance_cost"), text: $cost)
                TextField(L.t("vehicles_maintenance_parts"), text: $parts)
                TextField(L.t("vehicles_maintenance_workshop"), text: $workshop)
            }
            .navigationTitle(L.t("vehicles_maintenance_edit"))
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button(S.cancel) { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button(S.save) {
                        onSave(
                            IosKoinInitKt.iosUpdateMaintenanceEntry(
                                entry: entry,
                                date: date,
                                odometerKm: odometer,
                                entryDescription: description,
                                cost: cost,
                                partsUsed: parts,
                                workshop: workshop
                            )
                        )
                        dismiss()
                    }
                }
            }
            .onAppear {
                date = entry.date
                description = entry.entryDescription
                cost = entry.cost
                odometer = entry.odometerKm
                parts = entry.partsUsed
                workshop = entry.workshop
            }
        }
    }
}
