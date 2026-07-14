import Foundation
import sharedKit

enum CostOverviewGroupBy: String, CaseIterable {
    case maintenanceType = "MAINTENANCE_TYPE"
    case year = "YEAR"
    case month = "MONTH"
    case workshop = "WORKSHOP"
}

private let costGroupUnknownDate = "__unknown_date__"
private let costGroupUnknownWorkshop = "__unknown_workshop__"

struct MaintenanceCostItem: Identifiable {
    let entry: MaintenanceEntry
    let cost: Double

    var id: String { entry.id }
}

struct MaintenanceCostGroup: Identifiable {
    let key: String
    let total: Double
    let entryCount: Int

    var id: String { key }
}

struct VehicleCostSummary {
    let purchaseCost: Double?
    let maintenanceTotal: Double
    let maintenanceEntriesWithCost: [MaintenanceCostItem]
    let totalCost: Double?
    let hasAnyCostData: Bool
}

enum VehicleCostCalculator {
    static func summarize(vehicle: Vehicle) -> VehicleCostSummary {
        let purchaseCost = parseAmount(vehicle.purchasePrice)
        let maintenanceItems = vehicle.maintenanceLog.compactMap { entry -> MaintenanceCostItem? in
            guard let cost = parseAmount(entry.cost) else { return nil }
            return MaintenanceCostItem(entry: entry, cost: cost)
        }
        let maintenanceTotal = maintenanceItems.reduce(0) { $0 + $1.cost }

        let totalCost: Double? = {
            if let purchaseCost, !maintenanceItems.isEmpty { return purchaseCost + maintenanceTotal }
            if let purchaseCost { return purchaseCost }
            if !maintenanceItems.isEmpty { return maintenanceTotal }
            return nil
        }()

        return VehicleCostSummary(
            purchaseCost: purchaseCost,
            maintenanceTotal: maintenanceTotal,
            maintenanceEntriesWithCost: maintenanceItems.sorted { $0.entry.date > $1.entry.date },
            totalCost: totalCost,
            hasAnyCostData: purchaseCost != nil || !maintenanceItems.isEmpty
        )
    }

    static func groupMaintenanceCosts(
        items: [MaintenanceCostItem],
        groupBy: CostOverviewGroupBy
    ) -> [MaintenanceCostGroup] {
        let grouped = Dictionary(grouping: items) { groupKey(for: $0, groupBy: groupBy) }
        return grouped.map { key, groupItems in
            MaintenanceCostGroup(
                key: key,
                total: groupItems.reduce(0) { $0 + $1.cost },
                entryCount: groupItems.count
            )
        }
        .sorted { lhs, rhs in
            compareGroups(lhs, rhs, groupBy: groupBy)
        }
    }

    static func itemsInGroup(
        _ items: [MaintenanceCostItem],
        groupBy: CostOverviewGroupBy,
        groupKey: String
    ) -> [MaintenanceCostItem] {
        items
            .filter { self.groupKey(for: $0, groupBy: groupBy) == groupKey }
            .sorted { $0.entry.date > $1.entry.date }
    }

    static func parseAmount(_ value: String) -> Double? {
        let normalized = value.trimmingCharacters(in: .whitespacesAndNewlines).replacingOccurrences(of: ",", with: ".")
        guard !normalized.isEmpty, let amount = Double(normalized), amount >= 0 else { return nil }
        return amount
    }

    private static func groupKey(for item: MaintenanceCostItem, groupBy: CostOverviewGroupBy) -> String {
        let entry = item.entry
        switch groupBy {
        case .maintenanceType:
            return entry.type.name
        case .year:
            return parseVehicleDate(entry.date).map { String(Calendar.current.component(.year, from: $0)) }
                ?? costGroupUnknownDate
        case .month:
            guard let date = parseVehicleDate(entry.date) else { return costGroupUnknownDate }
            let year = Calendar.current.component(.year, from: date)
            let month = Calendar.current.component(.month, from: date)
            return String(format: "%04d-%02d", year, month)
        case .workshop:
            let trimmed = entry.workshop.trimmingCharacters(in: .whitespacesAndNewlines)
            return trimmed.isEmpty ? costGroupUnknownWorkshop : trimmed
        }
    }

    private static func compareGroups(
        _ lhs: MaintenanceCostGroup,
        _ rhs: MaintenanceCostGroup,
        groupBy: CostOverviewGroupBy
    ) -> Bool {
        func isUnknown(_ key: String) -> Bool {
            key == costGroupUnknownDate || key == costGroupUnknownWorkshop
        }
        if isUnknown(lhs.key) != isUnknown(rhs.key) {
            return !isUnknown(lhs.key)
        }
        switch groupBy {
        case .maintenanceType, .workshop:
            return lhs.total > rhs.total
        case .year, .month:
            return lhs.key > rhs.key
        }
    }
}

func parseVehicleDate(_ raw: String) -> Date? {
    let trimmed = raw.trimmingCharacters(in: .whitespacesAndNewlines)
    guard !trimmed.isEmpty else { return nil }

    let formats = [
        "dd.MM.yyyy HH:mm",
        "d.M.yyyy H:mm",
        "dd.MM.yyyy",
        "d.M.yyyy",
        "yyyy-MM-dd",
    ]
    let formatter = DateFormatter()
    formatter.locale = Locale(identifier: "de_DE")
    for format in formats {
        formatter.dateFormat = format
        if let date = formatter.date(from: trimmed) {
            return date
        }
    }
    return nil
}

func formatEuro(_ amount: Double) -> String {
    String(format: "%.2f €", locale: Locale(identifier: "de_DE"), amount)
}

func maintenanceTypeLabel(_ type: MaintenanceType) -> String {
    switch type {
    case .oilChange: return L.t("vehicles_maint_oil")
    case .sparkPlug: return L.t("vehicles_maint_spark")
    case .variator: return L.t("vehicles_maint_variator")
    case .carburetor: return L.t("vehicles_maint_carb")
    case .exhaust: return L.t("vehicles_maint_exhaust")
    case .tires: return L.t("vehicles_maint_tires")
    case .brakes: return L.t("vehicles_maint_brakes")
    case .bearing: return L.t("vehicles_maint_bearing")
    case .topEnd: return L.t("vehicles_maint_top_end")
    case .bottomEnd: return L.t("vehicles_maint_bottom_end")
    case .electrical: return L.t("vehicles_maint_electrical")
    case .inspectionTuv: return L.t("vehicles_maint_tuv")
    case .general: return L.t("vehicles_maint_general")
    case .other: return L.t("vehicles_maint_other")
    default: return type.name
    }
}

func costGroupByLabel(_ groupBy: CostOverviewGroupBy) -> String {
    switch groupBy {
    case .maintenanceType: return L.t("vehicles_cost_overview_group_type")
    case .year: return L.t("vehicles_cost_overview_group_year")
    case .month: return L.t("vehicles_cost_overview_group_month")
    case .workshop: return L.t("vehicles_cost_overview_group_workshop")
    }
}

func costGroupLabel(groupBy: CostOverviewGroupBy, key: String) -> String {
    switch groupBy {
    case .maintenanceType:
        return maintenanceTypeFromName(key).map(maintenanceTypeLabel) ?? key
    case .year:
        return key == costGroupUnknownDate ? L.t("vehicles_cost_overview_no_date") : key
    case .month:
        guard key != costGroupUnknownDate else { return L.t("vehicles_cost_overview_no_date") }
        let parts = key.split(separator: "-")
        guard parts.count == 2,
              let year = Int(parts[0]),
              let month = Int(parts[1]),
              month >= 1, month <= 12 else { return key }
        var components = DateComponents()
        components.year = year
        components.month = month
        components.day = 1
        guard let date = Calendar.current.date(from: components) else { return key }
        let formatter = DateFormatter()
        formatter.locale = Locale.current
        formatter.dateFormat = "MMMM yyyy"
        return formatter.string(from: date)
    case .workshop:
        return key == costGroupUnknownWorkshop ? L.t("vehicles_cost_overview_no_workshop") : key
    }
}

func maintenanceTypeFromName(_ name: String) -> MaintenanceType? {
    switch name {
    case "OIL_CHANGE": return .oilChange
    case "SPARK_PLUG": return .sparkPlug
    case "VARIATOR": return .variator
    case "CARBURETOR": return .carburetor
    case "EXHAUST": return .exhaust
    case "TIRES": return .tires
    case "BRAKES": return .brakes
    case "BEARING": return .bearing
    case "TOP_END": return .topEnd
    case "BOTTOM_END": return .bottomEnd
    case "ELECTRICAL": return .electrical
    case "INSPECTION_TUV": return .inspectionTuv
    case "GENERAL": return .general
    case "OTHER": return .other
    default: return nil
    }
}

func maintenanceCostEntryTitle(_ entry: MaintenanceEntry) -> String {
    let typeLabel = maintenanceTypeLabel(entry.type)
    return entry.date.isEmpty ? typeLabel : "\(typeLabel) · \(entry.date)"
}
