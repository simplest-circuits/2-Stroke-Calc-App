package com.simplestsoft.twostrokecalc.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class VehicleCatalogFile(
    val version: Int = 1,
    val source: String = "",
    val entries: List<VehicleCatalogEntry> = emptyList(),
)

@Serializable
data class VehicleCatalogEntry(
    val id: String,
    val brand: String,
    val model: String,
    val variant: String = "",
    val frameCode: String = "",
    val category: String = "",
    val yearFrom: Int,
    val yearTo: Int,
    val vehicleType: String,
    val cycleType: String = "TWO_STROKE",
    val engine: VehicleCatalogEngine = VehicleCatalogEngine(),
    val carbIgnition: VehicleCatalogCarbIgnition = VehicleCatalogCarbIgnition(),
    val drivetrain: VehicleCatalogDrivetrain = VehicleCatalogDrivetrain(),
    val chassis: VehicleCatalogChassis = VehicleCatalogChassis(),
    val electrical: VehicleCatalogElectrical = VehicleCatalogElectrical(),
) {
    fun displayModel(): String = if (variant.isBlank()) model else "$model – $variant"

    fun displayTitle(year: Int? = null): String = buildString {
        append(brand)
        append(' ')
        append(model)
        if (variant.isNotBlank()) {
            append(' ')
            append('(')
            append(variant)
            append(')')
        }
        val y = year ?: yearFrom
        if (yearFrom == yearTo) {
            append(" ($y)")
        } else if (year != null) {
            append(" ($y)")
        } else {
            append(" ($yearFrom–$yearTo)")
        }
    }

    fun matchesYear(year: Int): Boolean = year in yearFrom..yearTo

    fun toVehicleType(): VehicleType = runCatching {
        VehicleType.valueOf(vehicleType)
    }.getOrDefault(VehicleType.OTHER)

    fun toEngineCycleType(): EngineCycleType = runCatching {
        EngineCycleType.valueOf(cycleType)
    }.getOrDefault(EngineCycleType.TWO_STROKE)
}

@Serializable
data class VehicleCatalogEngine(
    val displacementCc: String = "",
    val boreMm: String = "",
    val strokeMm: String = "",
    val compressionRatio: String = "",
    val coolingType: String = "",
    val cylinderCount: String = "1",
    val intakeSystem: String = "",
    val portTimingNotes: String = "",
    val exhaustSystem: String = "",
    val engineNotes: String = "",
)

@Serializable
data class VehicleCatalogCarbIgnition(
    val carbType: String = "",
    val mainJet: String = "",
    val pilotJet: String = "",
    val needle: String = "",
    val fuelMixRatio: String = "",
    val fuelType: String = "",
    val oilType: String = "",
    val sparkPlug: String = "",
    val ignitionTimingDeg: String = "",
    val ignitionSystem: String = "",
)

@Serializable
data class VehicleCatalogDrivetrain(
    val driveType: String = "",
    val clutchType: String = "",
    val gearingPrimary: String = "",
    val gearingSecondary: String = "",
    val frontSprocketTeeth: String = "",
    val rearSprocketTeeth: String = "",
    val chainType: String = "",
    val finalDriveNotes: String = "",
)

@Serializable
data class VehicleCatalogChassis(
    val frontTire: String = "",
    val rearTire: String = "",
    val tirePressureFront: String = "",
    val tirePressureRear: String = "",
    val frontBrake: String = "",
    val rearBrake: String = "",
    val chassisNotes: String = "",
)

@Serializable
data class VehicleCatalogElectrical(
    val battery: String = "",
    val alternator: String = "",
    val electricalNotes: String = "",
)
