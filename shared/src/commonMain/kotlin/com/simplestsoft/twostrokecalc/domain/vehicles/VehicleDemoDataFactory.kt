package com.simplestsoft.twostrokecalc.domain.vehicles

import com.simplestsoft.twostrokecalc.domain.model.EngineCycleType
import com.simplestsoft.twostrokecalc.domain.model.FuelLogEntry
import com.simplestsoft.twostrokecalc.domain.model.MaintenanceEntry
import com.simplestsoft.twostrokecalc.domain.model.MaintenanceType
import com.simplestsoft.twostrokecalc.domain.model.Vehicle
import com.simplestsoft.twostrokecalc.domain.model.VehicleCarbIgnitionSpecs
import com.simplestsoft.twostrokecalc.domain.model.VehicleChassisSpecs
import com.simplestsoft.twostrokecalc.domain.model.VehicleDocumentInfo
import com.simplestsoft.twostrokecalc.domain.model.VehicleDrivetrainSpecs
import com.simplestsoft.twostrokecalc.domain.model.VehicleElectricalSpecs
import com.simplestsoft.twostrokecalc.domain.model.VehicleEngineSpecs
import com.simplestsoft.twostrokecalc.domain.model.VehicleServiceSchedule
import com.simplestsoft.twostrokecalc.domain.model.VehicleType
import com.simplestsoft.twostrokecalc.platform.randomUUID

object VehicleDemoDataFactory {
    const val ROLLER_DEMO_ID = "admin-demo-vespa-px125"
    const val MOFA_DEMO_ID = "admin-demo-puch-maxi"
    const val FREE_DEMO_VEHICLE_ID = ROLLER_DEMO_ID

    fun createFreeDemoVehicle(): Vehicle = createRollerDemo()

    fun isDemoVehicle(id: String): Boolean = id == ROLLER_DEMO_ID || id == MOFA_DEMO_ID

    fun createDemoVehicles(): List<Vehicle> = listOf(createRollerDemo(), createMofaDemo())

    private fun createRollerDemo(): Vehicle = Vehicle(
        id = ROLLER_DEMO_ID,
        name = "Demo Roller",
        brand = "Vespa",
        model = "PX 125 E",
        year = "1985",
        vehicleType = VehicleType.ROLLER,
        frameNumber = "VNX1T 123456",
        engineNumber = "M0812345",
        licensePlate = "M-VA 1985",
        color = "Rot",
        purchaseDate = "15.03.2019",
        purchasePrice = "2800",
        currentOdometerKm = "14120",
        notes = "Demodatensatz für Präsentation und Tests.",
        isActive = true,
        engine = VehicleEngineSpecs(
            cycleType = EngineCycleType.TWO_STROKE,
            displacementCc = "123",
            boreMm = "57",
            strokeMm = "48",
            compressionRatio = "7,0:1",
            exhaustSystem = "Polini Box-Collaudata",
        ),
        carbIgnition = VehicleCarbIgnitionSpecs(
            carbType = "Dell'Orto SI 20/20",
            mainJet = "98",
            fuelMixRatio = "1:50",
            sparkPlug = "NGK B8ES",
            ignitionTimingDeg = "18",
        ),
        drivetrain = VehicleDrivetrainSpecs(
            driveType = "Variomatik",
            variatorBrand = "Malossi Multivar 2000",
            gearingPrimary = "21/68",
            gearingSecondary = "13/42",
        ),
        chassis = VehicleChassisSpecs(
            frontTire = "120/70-10 Michelin City Grip",
            rearTire = "120/70-10 Michelin City Grip",
            frontBrake = "Trommel 150 mm",
            rearBrake = "Trommel 125 mm",
        ),
        electrical = VehicleElectricalSpecs(
            battery = "12V 5 Ah",
            headlightBulb = "H4 55/60W",
        ),
        documents = VehicleDocumentInfo(
            insuranceCompany = "DEVK",
            insurancePolicyNumber = "DEMO-123456789",
        ),
        serviceSchedule = VehicleServiceSchedule(
            oilChangeIntervalKm = "3000",
            tuvInspectionDate = "15.09.2026",
            insuranceExpiryDate = "01.04.2026",
            lastOilChangeDate = "12.01.2026",
            lastOilChangeKm = "13980",
        ),
        maintenanceLog = listOf(
            maintenance("12.01.2026", "13980", MaintenanceType.OIL_CHANGE, "Getriebeöl + Zündkerze", "57"),
            maintenance("05.12.2025", "13460", MaintenanceType.ELECTRICAL, "Gleichrichter getauscht", "32"),
        ),
        fuelLog = demoFuelLog(startKm = 12450, startLiters = 4.2),
    )

    private fun createMofaDemo(): Vehicle = Vehicle(
        id = MOFA_DEMO_ID,
        name = "Demo Mofa",
        brand = "Puch",
        model = "Maxi S",
        year = "1978",
        vehicleType = VehicleType.MOFA,
        licensePlate = "M-PM 1978",
        currentOdometerKm = "10240",
        notes = "Demodatensatz Mofa.",
        engine = VehicleEngineSpecs(
            cycleType = EngineCycleType.TWO_STROKE,
            displacementCc = "49,9",
            intakeSystem = "Membran Bing",
        ),
        carbIgnition = VehicleCarbIgnitionSpecs(carbType = "Bing 12/12", mainJet = "52", fuelMixRatio = "1:50"),
        drivetrain = VehicleDrivetrainSpecs(driveType = "Kette", frontSprocketTeeth = "11", rearSprocketTeeth = "52"),
        fuelLog = demoFuelLog(startKm = 8920, startLiters = 2.5),
    )

    private fun maintenance(
        date: String,
        odometerKm: String,
        type: MaintenanceType,
        description: String,
        cost: String,
    ) = MaintenanceEntry(
        id = randomUUID(),
        date = date,
        odometerKm = odometerKm,
        type = type,
        entryDescription = description,
        cost = cost,
    )

    private fun demoFuelLog(startKm: Int, startLiters: Double): List<FuelLogEntry> =
        (0 until 8).map { index ->
            FuelLogEntry(
                id = randomUUID(),
                date = "${(index + 1).toString().padStart(2, '0')}.2025",
                odometerKm = (startKm + index * 120).toString(),
                liters = (startLiters + index * 0.1).toString(),
                price = ((startLiters + index * 0.1) * 1.75).toString(),
            )
        }
}
