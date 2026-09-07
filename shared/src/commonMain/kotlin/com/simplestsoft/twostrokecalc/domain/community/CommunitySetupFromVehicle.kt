package com.simplestsoft.twostrokecalc.domain.community

import com.simplestsoft.twostrokecalc.domain.model.EngineCycleType
import com.simplestsoft.twostrokecalc.domain.model.Vehicle
import com.simplestsoft.twostrokecalc.domain.model.VehicleType
import com.simplestsoft.twostrokecalc.domain.model.community.CommunitySetup
import com.simplestsoft.twostrokecalc.domain.model.community.EngineTransmissionType

/** Build a Community Setup draft from a garage vehicle, copying all overlapping specs. */
fun CommunitySetup.Companion.fromVehicle(
    vehicle: Vehicle,
    authorDisplayName: String = "",
): CommunitySetup {
    val source = vehicle.withLegacyMigration()
    val transmission = resolveTransmissionType(source)
    return CommunitySetup(
        engineFamilyId = source.engineFamilyId,
        engineFamilyCustomName = source.engineFamilyCustomName,
        transmissionType = transmission,
        vehicleBrand = source.brand,
        vehicleModel = source.model,
        yearFrom = source.year.toIntOrNull(),
        yearTo = source.year.toIntOrNull(),
        engine = source.engine,
        carbIgnition = source.carbIgnition,
        drivetrain = source.drivetrain,
        title = source.displayTitle(),
        experienceNotes = source.notes.trim(),
        tags = suggestedTagsFromVehicle(source, transmission),
        sourceVehicleId = source.id,
        authorDisplayName = authorDisplayName,
        showAuthorName = false,
    )
}

/**
 * First wizard step that still needs user attention after vehicle prefill.
 * 1 = Motortyp, 2 = Fahrzeug, 3 = Specs, 4 = Titel/Erfahrung.
 */
fun suggestedCommunitySubmitStep(vehicle: Vehicle): Int {
    val source = vehicle.withLegacyMigration()
    val transmission = resolveTransmissionType(source)
    val hasEngineFamily =
        source.engineFamilyId.isNotBlank() || source.engineFamilyCustomName.isNotBlank()
    val hasVehicleContext =
        source.brand.isNotBlank() || source.model.isNotBlank() || source.year.isNotBlank()
    val hasCoreSpecs =
        source.engine.displacementCc.isNotBlank() &&
            transmission != EngineTransmissionType.UNKNOWN
    return when {
        hasEngineFamily && hasVehicleContext && hasCoreSpecs -> 4
        hasEngineFamily && hasVehicleContext -> 3
        hasEngineFamily -> 2
        else -> 1
    }
}

fun resolveTransmissionType(vehicle: Vehicle): EngineTransmissionType =
    when {
        vehicle.transmissionType != EngineTransmissionType.UNKNOWN -> vehicle.transmissionType
        else -> when (vehicle.vehicleType) {
            VehicleType.ROLLER -> EngineTransmissionType.VARIATOR
            VehicleType.MOFA,
            VehicleType.MOKICK,
            VehicleType.CROSS,
            -> EngineTransmissionType.GEARBOX
            VehicleType.FOUR_WHEEL,
            VehicleType.OTHER,
            -> EngineTransmissionType.UNKNOWN
        }
    }

private fun suggestedTagsFromVehicle(
    vehicle: Vehicle,
    transmission: EngineTransmissionType,
): List<String> = buildList {
    when (vehicle.vehicleType) {
        VehicleType.MOFA -> add("Mofa")
        VehicleType.MOKICK -> add("Mokick")
        VehicleType.ROLLER -> add("Roller")
        VehicleType.CROSS -> add("Cross")
        VehicleType.FOUR_WHEEL -> add("Quad")
        VehicleType.OTHER -> Unit
    }
    when (vehicle.engine.cycleType) {
        EngineCycleType.TWO_STROKE -> add("2-Takt")
        EngineCycleType.FOUR_STROKE -> add("4-Takt")
    }
    when (transmission) {
        EngineTransmissionType.VARIATOR -> add("Variomatik")
        EngineTransmissionType.GEARBOX -> add("Getriebe")
        EngineTransmissionType.UNKNOWN -> Unit
    }
    vehicle.engine.displacementCc.trim().takeIf { it.isNotBlank() }?.let { add("${it}ccm") }
}.distinct()
