package com.simplestsoft.twostrokecalc.domain.vehicles

import com.simplestsoft.twostrokecalc.domain.model.MaintenanceEntry
import com.simplestsoft.twostrokecalc.domain.model.MaintenanceType
import com.simplestsoft.twostrokecalc.domain.model.Vehicle

enum class CostOverviewGroupBy {
    MAINTENANCE_TYPE,
    YEAR,
    MONTH,
    WORKSHOP,
}

const val COST_GROUP_UNKNOWN_DATE = "__unknown_date__"
const val COST_GROUP_UNKNOWN_WORKSHOP = "__unknown_workshop__"

data class MaintenanceCostItem(
    val entry: MaintenanceEntry,
    val cost: Double,
)

data class MaintenanceCostGroup(
    val key: String,
    val total: Double,
    val entryCount: Int,
)

data class VehicleCostSummary(
    val purchaseCost: Double?,
    val maintenanceTotal: Double,
    val maintenanceEntriesWithCost: List<MaintenanceCostItem>,
    val totalCost: Double?,
    val hasAnyCostData: Boolean,
)

object VehicleCostCalculator {

    fun summarize(vehicle: Vehicle): VehicleCostSummary {
        val purchaseCost = parseAmount(vehicle.purchasePrice)
        val maintenanceItems = vehicle.maintenanceLog.mapNotNull { entry ->
            parseAmount(entry.cost)?.let { MaintenanceCostItem(entry, it) }
        }
        val maintenanceTotal = maintenanceItems.sumOf { it.cost }

        val totalCost = when {
            purchaseCost != null && maintenanceItems.isNotEmpty() -> purchaseCost + maintenanceTotal
            purchaseCost != null -> purchaseCost
            maintenanceItems.isNotEmpty() -> maintenanceTotal
            else -> null
        }

        return VehicleCostSummary(
            purchaseCost = purchaseCost,
            maintenanceTotal = maintenanceTotal,
            maintenanceEntriesWithCost = maintenanceItems.sortedByDescending { it.entry.date },
            totalCost = totalCost,
            hasAnyCostData = purchaseCost != null || maintenanceItems.isNotEmpty(),
        )
    }

    fun groupMaintenanceCosts(
        items: List<MaintenanceCostItem>,
        groupBy: CostOverviewGroupBy,
    ): List<MaintenanceCostGroup> {
        return items
            .groupBy { groupKeyForItem(it, groupBy) }
            .map { (key, groupItems) ->
                MaintenanceCostGroup(
                    key = key,
                    total = groupItems.sumOf { it.cost },
                    entryCount = groupItems.size,
                )
            }
            .sortedWith(groupComparator(groupBy))
    }

    fun itemsInGroup(
        items: List<MaintenanceCostItem>,
        groupBy: CostOverviewGroupBy,
        groupKey: String,
    ): List<MaintenanceCostItem> {
        return items
            .filter { groupKeyForItem(it, groupBy) == groupKey }
            .sortedByDescending { it.entry.date }
    }

    private fun groupKeyForItem(item: MaintenanceCostItem, groupBy: CostOverviewGroupBy): String {
        val entry = item.entry
        return when (groupBy) {
            CostOverviewGroupBy.MAINTENANCE_TYPE -> entry.type.name
            CostOverviewGroupBy.YEAR -> parseVehicleDate(entry.date)?.year?.toString()
                ?: COST_GROUP_UNKNOWN_DATE
            CostOverviewGroupBy.MONTH -> parseVehicleDate(entry.date)?.let { date ->
                "%04d-%02d".format(date.year, date.monthValue)
            } ?: COST_GROUP_UNKNOWN_DATE
            CostOverviewGroupBy.WORKSHOP -> entry.workshop.trim().ifEmpty { COST_GROUP_UNKNOWN_WORKSHOP }
        }
    }

    private fun groupComparator(groupBy: CostOverviewGroupBy): Comparator<MaintenanceCostGroup> {
        val unknownLast = compareBy<MaintenanceCostGroup> {
            it.key == COST_GROUP_UNKNOWN_DATE || it.key == COST_GROUP_UNKNOWN_WORKSHOP
        }
        return when (groupBy) {
            CostOverviewGroupBy.MAINTENANCE_TYPE,
            CostOverviewGroupBy.WORKSHOP,
            -> unknownLast.thenByDescending { it.total }
            CostOverviewGroupBy.YEAR,
            CostOverviewGroupBy.MONTH,
            -> unknownLast.thenByDescending { it.key }
        }
    }

    fun parseAmount(value: String): Double? {
        val normalized = value.trim().replace(',', '.')
        if (normalized.isEmpty()) return null
        return normalized.toDoubleOrNull()?.takeIf { it >= 0.0 }
    }
}
