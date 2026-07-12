package com.simplestsoft.twostrokecalc.domain.vehicles

import com.simplestsoft.twostrokecalc.domain.model.Vehicle
import com.simplestsoft.twostrokecalc.domain.model.VehicleCarbIgnitionSpecs
import com.simplestsoft.twostrokecalc.domain.model.VehicleCatalogEntry
import com.simplestsoft.twostrokecalc.domain.model.VehicleChassisSpecs
import com.simplestsoft.twostrokecalc.domain.model.VehicleDrivetrainSpecs
import com.simplestsoft.twostrokecalc.domain.model.VehicleElectricalSpecs
import com.simplestsoft.twostrokecalc.domain.model.VehicleEngineSpecs

object VehicleCatalogMapper {
    fun toVehicle(
        entry: VehicleCatalogEntry,
        year: String,
        name: String = "",
        licensePlate: String = "",
        currentOdometerKm: String = "",
        currentOperatingHours: String = "",
    ): Vehicle = Vehicle(
        name = name.ifBlank { entry.displayTitle(year.toIntOrNull()) },
        brand = entry.brand,
        model = entry.model,
        year = year,
        vehicleType = entry.toVehicleType(),
        licensePlate = licensePlate,
        currentOdometerKm = currentOdometerKm,
        currentOperatingHours = currentOperatingHours,
        notes = buildList {
            if (entry.frameCode.isNotBlank()) add("Rahmen: ${entry.frameCode}")
            if (entry.category.isNotBlank()) add("Kategorie: ${entry.category}")
            entry.engine.engineNotes.takeIf { it.isNotBlank() }?.let { add(it) }
        }.joinToString("\n"),
        engine = VehicleEngineSpecs(
            cycleType = entry.toEngineCycleType(),
            displacementCc = entry.engine.displacementCc,
            boreMm = entry.engine.boreMm,
            strokeMm = entry.engine.strokeMm,
            compressionRatio = entry.engine.compressionRatio,
            coolingType = entry.engine.coolingType,
            cylinderCount = entry.engine.cylinderCount,
            intakeSystem = entry.engine.intakeSystem,
            portTimingNotes = entry.engine.portTimingNotes,
            exhaustSystem = entry.engine.exhaustSystem,
            engineNotes = entry.engine.engineNotes,
        ),
        carbIgnition = VehicleCarbIgnitionSpecs(
            carbType = entry.carbIgnition.carbType,
            mainJet = entry.carbIgnition.mainJet,
            pilotJet = entry.carbIgnition.pilotJet,
            needle = entry.carbIgnition.needle,
            fuelMixRatio = entry.carbIgnition.fuelMixRatio,
            fuelType = entry.carbIgnition.fuelType,
            oilType = entry.carbIgnition.oilType,
            sparkPlug = entry.carbIgnition.sparkPlug,
            ignitionTimingDeg = entry.carbIgnition.ignitionTimingDeg,
            ignitionSystem = entry.carbIgnition.ignitionSystem,
        ),
        drivetrain = VehicleDrivetrainSpecs(
            driveType = entry.drivetrain.driveType,
            clutchType = entry.drivetrain.clutchType,
            gearingPrimary = entry.drivetrain.gearingPrimary,
            gearingSecondary = entry.drivetrain.gearingSecondary,
            frontSprocketTeeth = entry.drivetrain.frontSprocketTeeth,
            rearSprocketTeeth = entry.drivetrain.rearSprocketTeeth,
            chainType = entry.drivetrain.chainType,
            finalDriveNotes = entry.drivetrain.finalDriveNotes,
        ),
        chassis = VehicleChassisSpecs(
            frontTire = entry.chassis.frontTire,
            rearTire = entry.chassis.rearTire,
            tirePressureFront = entry.chassis.tirePressureFront,
            tirePressureRear = entry.chassis.tirePressureRear,
            frontBrake = entry.chassis.frontBrake,
            rearBrake = entry.chassis.rearBrake,
            chassisNotes = entry.chassis.chassisNotes,
        ),
        electrical = VehicleElectricalSpecs(
            battery = entry.electrical.battery,
            alternator = entry.electrical.alternator,
            electricalNotes = entry.electrical.electricalNotes,
        ),
    )
}
