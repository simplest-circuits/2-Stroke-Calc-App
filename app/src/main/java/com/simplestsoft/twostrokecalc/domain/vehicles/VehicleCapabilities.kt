package com.simplestsoft.twostrokecalc.domain.vehicles

import com.simplestsoft.twostrokecalc.domain.model.EngineCycleType
import com.simplestsoft.twostrokecalc.domain.model.MaintenanceType
import com.simplestsoft.twostrokecalc.domain.model.Vehicle
import com.simplestsoft.twostrokecalc.domain.model.VehicleEngineSpecs
import com.simplestsoft.twostrokecalc.domain.model.VehicleType

data class VehicleCapabilities(
    val showVariator: Boolean,
    val showChainGearing: Boolean,
    val showClutch: Boolean,
    val showVariatorMaintenance: Boolean,
    val showFuelMix: Boolean,
    val showTransmission: Boolean = false,
)

fun VehicleType.defaultEngineCycle(): EngineCycleType = when (this) {
    VehicleType.FOUR_WHEEL -> EngineCycleType.FOUR_STROKE
    else -> EngineCycleType.TWO_STROKE
}

fun vehicleCapabilities(vehicle: Vehicle): VehicleCapabilities {
    val isTwoStroke = vehicle.engine.cycleType == EngineCycleType.TWO_STROKE
    return when (vehicle.vehicleType) {
        VehicleType.ROLLER -> VehicleCapabilities(
            showVariator = true,
            showChainGearing = false,
            showClutch = true,
            showVariatorMaintenance = true,
            showFuelMix = isTwoStroke,
        )
        VehicleType.MOFA,
        VehicleType.MOKICK,
        VehicleType.CROSS,
        -> VehicleCapabilities(
            showVariator = false,
            showChainGearing = true,
            showClutch = true,
            showVariatorMaintenance = false,
            showFuelMix = isTwoStroke,
        )
        VehicleType.FOUR_WHEEL -> VehicleCapabilities(
            showVariator = false,
            showChainGearing = false,
            showClutch = false,
            showVariatorMaintenance = false,
            showFuelMix = isTwoStroke,
            showTransmission = true,
        )
        VehicleType.OTHER -> VehicleCapabilities(
            showVariator = true,
            showChainGearing = true,
            showClutch = true,
            showVariatorMaintenance = true,
            showFuelMix = isTwoStroke,
        )
    }
}

fun applicableMaintenanceTypes(vehicle: Vehicle): List<MaintenanceType> {
    val capabilities = vehicleCapabilities(vehicle)
    return MaintenanceType.entries.filter { type ->
        type != MaintenanceType.VARIATOR || capabilities.showVariatorMaintenance
    }
}

fun VehicleEngineSpecs.resolvedCylinderCount(): Int =
    cylinderCount.trim().toIntOrNull()?.coerceIn(1, 8) ?: 1

fun VehicleEngineSpecs.resolvedValveClearances(): List<String> {
    val count = resolvedCylinderCount()
    return when {
        valveClearances.size >= count -> valveClearances.take(count)
        valveClearances.isNotEmpty() -> {
            valveClearances + List(count - valveClearances.size) { "" }
        }
        valveClearance.isNotBlank() -> listOf(valveClearance) + List(count - 1) { "" }
        else -> List(count) { "" }
    }
}

fun VehicleEngineSpecs.withValveClearances(clearances: List<String>): VehicleEngineSpecs {
    val count = resolvedCylinderCount()
    val normalized = clearances.take(count) + List((count - clearances.size).coerceAtLeast(0)) { "" }
    return copy(
        valveClearances = normalized,
        valveClearance = normalized.firstOrNull().orEmpty(),
    )
}

fun VehicleEngineSpecs.withCylinderCount(count: String): VehicleEngineSpecs {
    val newCount = count.trim().toIntOrNull()?.coerceIn(1, 8) ?: 1
    val current = resolvedValveClearances()
    val resized = current.take(newCount) + List((newCount - current.size).coerceAtLeast(0)) { "" }
    return copy(
        cylinderCount = count,
        valveClearances = resized,
        valveClearance = resized.firstOrNull().orEmpty(),
    )
}
