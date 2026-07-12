package com.simplestsoft.twostrokecalc.domain.model

import com.simplestsoft.twostrokecalc.platform.currentTimeMillis
import com.simplestsoft.twostrokecalc.platform.randomUUID
import kotlinx.serialization.Serializable

@Serializable
enum class MaintenanceType {
    OIL_CHANGE,
    SPARK_PLUG,
    VARIATOR,
    CARBURETOR,
    EXHAUST,
    TIRES,
    BRAKES,
    BEARING,
    TOP_END,
    BOTTOM_END,
    ELECTRICAL,
    INSPECTION_TUV,
    GENERAL,
    OTHER,
}

@Serializable
enum class VehicleType {
    MOFA,
    MOKICK,
    ROLLER,
    CROSS,
    FOUR_WHEEL,
    OTHER,
}

@Serializable
enum class EngineCycleType {
    TWO_STROKE,
    FOUR_STROKE,
}

@Serializable
data class MaintenanceEntry(
    val id: String = randomUUID(),
    val date: String = "",
    val odometerKm: String = "",
    val type: MaintenanceType = MaintenanceType.GENERAL,
    val description: String = "",
    val cost: String = "",
    val partsUsed: String = "",
    val workshop: String = "",
)

@Serializable
data class FuelLogEntry(
    val id: String = randomUUID(),
    val date: String = "",
    val odometerKm: String = "",
    val operatingHours: String = "",
    val liters: String = "",
    val price: String = "",
)

@Serializable
data class VehicleEngineSpecs(
    val cycleType: EngineCycleType = EngineCycleType.TWO_STROKE,
    val displacementCc: String = "",
    val boreMm: String = "",
    val strokeMm: String = "",
    val compressionRatio: String = "",
    val squishClearanceMm: String = "",
    val cylinderHead: String = "",
    val piston: String = "",
    val reedValve: String = "",
    val intakeSystem: String = "",
    val coolingType: String = "",
    val portTimingNotes: String = "",
    val cylinderCount: String = "1",
    val valveClearance: String = "",
    val valveClearances: List<String> = emptyList(),
    val exhaustSystem: String = "",
    val engineNotes: String = "",
)

@Serializable
data class VehicleCarbIgnitionSpecs(
    val carbType: String = "",
    val mainJet: String = "",
    val pilotJet: String = "",
    val needle: String = "",
    val fuelMixRatio: String = "",
    val fuelType: String = "",
    val oilType: String = "",
    val oilBrand: String = "",
    val sparkPlug: String = "",
    val ignitionTimingDeg: String = "",
    val ignitionSystem: String = "",
    val cdiBox: String = "",
    val tuningNotes: String = "",
)

@Serializable
data class VehicleDrivetrainSpecs(
    val driveType: String = "",
    val variatorBrand: String = "",
    val variatorWeightsG: String = "",
    val variatorRollers: String = "",
    val variatorBelt: String = "",
    val variatorGuide: String = "",
    val clutchType: String = "",
    val clutchSprings: String = "",
    val gearingPrimary: String = "",
    val gearingSecondary: String = "",
    val frontSprocketTeeth: String = "",
    val rearSprocketTeeth: String = "",
    val chainType: String = "",
    val chainLinks: String = "",
    val finalDriveNotes: String = "",
)

@Serializable
data class VehicleChassisSpecs(
    val frontTire: String = "",
    val rearTire: String = "",
    val tirePressureFront: String = "",
    val tirePressureRear: String = "",
    val rimFront: String = "",
    val rimRear: String = "",
    val frontBrake: String = "",
    val rearBrake: String = "",
    val brakePadsFront: String = "",
    val brakePadsRear: String = "",
    val brakeFluid: String = "",
    val forkOil: String = "",
    val rearShock: String = "",
    val steeringBearing: String = "",
    val chassisNotes: String = "",
)

@Serializable
data class VehicleElectricalSpecs(
    val battery: String = "",
    val batteryYear: String = "",
    val alternator: String = "",
    val regulator: String = "",
    val ignitionCoil: String = "",
    val wiringNotes: String = "",
    val headlightBulb: String = "",
    val tailLightBulb: String = "",
    val horn: String = "",
    val electricalNotes: String = "",
)

@Serializable
enum class VehicleAttachmentCategory {
    INSURANCE,
    REGISTRATION,
    TUV,
    PURCHASE,
    INVOICE,
    SERVICE,
    OTHER,
}

@Serializable
data class VehicleAttachment(
    val id: String = randomUUID(),
    val fileName: String = "",
    val displayName: String = "",
    val mimeType: String = "application/octet-stream",
    val category: VehicleAttachmentCategory = VehicleAttachmentCategory.OTHER,
    val notes: String = "",
    val fileSizeBytes: Long = 0L,
    val addedAtMs: Long = currentTimeMillis(),
)

@Serializable
data class VehicleDocumentInfo(
    val insuranceCompany: String = "",
    val insurancePolicyNumber: String = "",
    val insuranceType: String = "",
    val registrationCertNumber: String = "",
    val ownerOnPaper: String = "",
    val purchaseFrom: String = "",
    val storageLocation: String = "",
    val keyLocation: String = "",
    val documentNotes: String = "",
)

@Serializable
data class VehicleTuningSpecs(
    val displacementCc: String = "",
    val boreMm: String = "",
    val strokeMm: String = "",
    val carbType: String = "",
    val mainJet: String = "",
    val pilotJet: String = "",
    val needle: String = "",
    val fuelMixRatio: String = "",
    val sparkPlug: String = "",
    val ignitionTimingDeg: String = "",
    val compressionRatio: String = "",
    val squishClearanceMm: String = "",
    val exhaustSystem: String = "",
    val variatorWeightsG: String = "",
    val clutchSprings: String = "",
    val gearingPrimary: String = "",
    val gearingSecondary: String = "",
    val portTimingNotes: String = "",
    val additionalTuningNotes: String = "",
)

@Serializable
data class VehicleServiceSchedule(
    val oilChangeIntervalKm: String = "",
    val sparkPlugIntervalKm: String = "",
    val variatorServiceIntervalKm: String = "",
    val brakeServiceIntervalKm: String = "",
    val tireServiceIntervalKm: String = "",
    val tuvInspectionDate: String = "",
    val insuranceExpiryDate: String = "",
    val lastOilChangeDate: String = "",
    val lastOilChangeKm: String = "",
    val nextServiceDueKm: String = "",
    val nextServiceDueDate: String = "",
)

@Serializable
data class Vehicle(
    val id: String = randomUUID(),
    val name: String = "",
    val brand: String = "",
    val model: String = "",
    val year: String = "",
    val vehicleType: VehicleType = VehicleType.MOFA,
    val frameNumber: String = "",
    val engineNumber: String = "",
    val licensePlate: String = "",
    val color: String = "",
    val purchaseDate: String = "",
    val purchasePrice: String = "",
    val currentOdometerKm: String = "",
    val currentOperatingHours: String = "",
    val notes: String = "",
    val profileImageFileName: String? = null,
    val isActive: Boolean = true,
    val tuning: VehicleTuningSpecs? = null,
    val engine: VehicleEngineSpecs = VehicleEngineSpecs(),
    val carbIgnition: VehicleCarbIgnitionSpecs = VehicleCarbIgnitionSpecs(),
    val drivetrain: VehicleDrivetrainSpecs = VehicleDrivetrainSpecs(),
    val chassis: VehicleChassisSpecs = VehicleChassisSpecs(),
    val electrical: VehicleElectricalSpecs = VehicleElectricalSpecs(),
    val documents: VehicleDocumentInfo = VehicleDocumentInfo(),
    val attachments: List<VehicleAttachment> = emptyList(),
    val serviceSchedule: VehicleServiceSchedule = VehicleServiceSchedule(),
    val maintenanceLog: List<MaintenanceEntry> = emptyList(),
    val fuelLog: List<FuelLogEntry> = emptyList(),
    val createdAtMs: Long = currentTimeMillis(),
    val updatedAtMs: Long = currentTimeMillis(),
) {
    fun displayTitle(): String = name.ifBlank {
        listOf(brand, model).filter { it.isNotBlank() }.joinToString(" ").ifBlank { "—" }
    }

    fun displaySubtitle(): String = buildList {
        if (licensePlate.isNotBlank()) add(licensePlate)
        if (year.isNotBlank()) add(year)
        if (currentOdometerKm.isNotBlank()) add("${currentOdometerKm} km")
        if (currentOperatingHours.isNotBlank()) add("${currentOperatingHours} h")
    }.joinToString(" · ")

    fun withLegacyMigration(): Vehicle {
        val legacy = tuning ?: return this
        if (engine.displacementCc.isNotBlank() || carbIgnition.carbType.isNotBlank()) {
            return copy(tuning = null)
        }
        return copy(
            tuning = null,
            engine = engine.copy(
                displacementCc = legacy.displacementCc,
                boreMm = legacy.boreMm,
                strokeMm = legacy.strokeMm,
                compressionRatio = legacy.compressionRatio,
                squishClearanceMm = legacy.squishClearanceMm,
                portTimingNotes = legacy.portTimingNotes,
                exhaustSystem = legacy.exhaustSystem,
                engineNotes = legacy.additionalTuningNotes,
            ),
            carbIgnition = carbIgnition.copy(
                carbType = legacy.carbType,
                mainJet = legacy.mainJet,
                pilotJet = legacy.pilotJet,
                needle = legacy.needle,
                fuelMixRatio = legacy.fuelMixRatio,
                sparkPlug = legacy.sparkPlug,
                ignitionTimingDeg = legacy.ignitionTimingDeg,
            ),
            drivetrain = drivetrain.copy(
                variatorWeightsG = legacy.variatorWeightsG,
                clutchSprings = legacy.clutchSprings,
                gearingPrimary = legacy.gearingPrimary,
                gearingSecondary = legacy.gearingSecondary,
            ),
        )
    }
}
