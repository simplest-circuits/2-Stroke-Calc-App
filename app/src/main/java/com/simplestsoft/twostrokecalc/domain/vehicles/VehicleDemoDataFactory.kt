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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

object VehicleDemoDataFactory {

    const val ROLLER_DEMO_ID = "admin-demo-vespa-px125"
    const val MOFA_DEMO_ID = "admin-demo-puch-maxi"
    const val FREE_DEMO_VEHICLE_ID = ROLLER_DEMO_ID

    fun createFreeDemoVehicle(): Vehicle = createRollerDemo()

    fun isDemoVehicle(id: String): Boolean = id == ROLLER_DEMO_ID || id == MOFA_DEMO_ID

    private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.uuuu", Locale.GERMANY)

    fun createDemoVehicles(): List<Vehicle> = listOf(
        createRollerDemo(),
        createMofaDemo(),
    )

    private fun createRollerDemo(): Vehicle {
        val fuelLog = buildFuelLog(
            startDate = LocalDate.now().minusMonths(14),
            startOdometerKm = 12_450,
            kmStepRange = 95..145,
            litersRange = 3.8..5.2,
            pricePerLiterRange = 1.72..1.89,
        )
        val currentOdometer = fuelLog.last().odometerKm

        return Vehicle(
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
            currentOdometerKm = currentOdometer,
            notes = "Demodatensatz für Präsentation und Tests. Vespa PX 125 mit typischer Wartungshistorie.",
            isActive = true,
            engine = VehicleEngineSpecs(
                cycleType = EngineCycleType.TWO_STROKE,
                displacementCc = "123",
                boreMm = "57",
                strokeMm = "48",
                compressionRatio = "7,0:1",
                squishClearanceMm = "0,8",
                cylinderHead = "Standard PX 125",
                piston = "Polini 123 ccm",
                reedValve = "Membran",
                intakeSystem = "Membranansaugung",
                coolingType = "Luft",
                portTimingNotes = "Standard PX-Steuerzeiten, leicht sportlicher Auslass",
                exhaustSystem = "Polini Box-Collaudata",
                engineNotes = "Motor frisch überholt, läuft ruhig im Stadtverkehr.",
            ),
            carbIgnition = VehicleCarbIgnitionSpecs(
                carbType = "Dell'Orto SI 20/20",
                mainJet = "98",
                pilotJet = "55",
                needle = "BE 266",
                fuelMixRatio = "1:50",
                fuelType = "Super E10",
                oilType = "2-Takt",
                oilBrand = "Motul 800 2T Factory Line",
                sparkPlug = "NGK B8ES",
                ignitionTimingDeg = "18",
                ignitionSystem = "CDI",
                cdiBox = "Standard PX",
                tuningNotes = "Hauptdüse 98 bei 18° OT, sauberer Durchzug bis 80 km/h.",
            ),
            drivetrain = VehicleDrivetrainSpecs(
                driveType = "Variomatik",
                variatorBrand = "Malossi Multivar 2000",
                variatorWeightsG = "6,5",
                variatorRollers = "15×13 mm, 6,5 g",
                variatorBelt = "Polini Speed Belt",
                variatorGuide = "Standard",
                clutchType = "Automatik",
                clutchSprings = "Standard",
                gearingPrimary = "21/68",
                gearingSecondary = "13/42",
                finalDriveNotes = "Gang 3 bei ca. 75 km/h, Endgeschwindigkeit ~95 km/h.",
            ),
            chassis = VehicleChassisSpecs(
                frontTire = "120/70-10 Michelin City Grip",
                rearTire = "120/70-10 Michelin City Grip",
                tirePressureFront = "2,0",
                tirePressureRear = "2,2",
                rimFront = "10\" Stahl",
                rimRear = "10\" Stahl",
                frontBrake = "Trommel 150 mm",
                rearBrake = "Trommel 125 mm",
                brakePadsFront = "Standard",
                brakePadsRear = "Standard",
                forkOil = "SAE 10W",
                rearShock = "Standard Federbein",
                chassisNotes = "Federbeine und Gabel undichtigkeitsfrei.",
            ),
            electrical = VehicleElectricalSpecs(
                battery = "12V 5 Ah",
                batteryYear = "2024",
                alternator = "6V Lichtmaschine",
                regulator = "Gleichrichter 12V Wandler",
                ignitionCoil = "Standard CDI",
                headlightBulb = "H4 55/60W",
                tailLightBulb = "5W",
                electricalNotes = "Lichtanlage auf 12V umgerüstet.",
            ),
            documents = VehicleDocumentInfo(
                insuranceCompany = "DEVK",
                insurancePolicyNumber = "DEMO-123456789",
                insuranceType = "Teilkasko",
                registrationCertNumber = "ZB II DEMO 001",
                ownerOnPaper = "Admin Demo",
                purchaseFrom = "Privat, München",
                storageLocation = "Garage",
                documentNotes = "Fahrzeugpapiere und HU-Berichte im Ordner.",
            ),
            serviceSchedule = VehicleServiceSchedule(
                oilChangeIntervalKm = "3000",
                sparkPlugIntervalKm = "5000",
                variatorServiceIntervalKm = "8000",
                brakeServiceIntervalKm = "10000",
                tireServiceIntervalKm = "12000",
                tuvInspectionDate = "15.09.2026",
                insuranceExpiryDate = "01.04.2026",
                lastOilChangeDate = "12.01.2026",
                lastOilChangeKm = "13980",
                nextServiceDueKm = "16980",
                nextServiceDueDate = "01.07.2026",
            ),
            maintenanceLog = listOf(
                maintenance("2024-03-18", "11200", MaintenanceType.INSPECTION_TUV, "Hauptuntersuchung bestanden", "85", "HU-Prüfung", "TÜV Süd"),
                maintenance("2024-05-02", "11480", MaintenanceType.OIL_CHANGE, "Getriebeöl gewechselt", "45", "Motul Gear 80W-90", "Eigenleistung"),
                maintenance("2024-07-14", "11720", MaintenanceType.VARIATOR, "Variomatik gewartet", "89", "Rollen, Keilriemen, Führungsstück", "Vespa Werkstatt Müller"),
                maintenance("2024-09-08", "11950", MaintenanceType.CARBURETOR, "Vergaser gereinigt, Düsen geprüft", "65", "SI 20/20 Dichtsatz", "Eigenleistung"),
                maintenance("2024-11-22", "12100", MaintenanceType.TIRES, "Sommerreifen montiert", "118", "Michelin City Grip 120/70-10", "Reifen Schmidt"),
                maintenance("2025-01-15", "12280", MaintenanceType.SPARK_PLUG, "Zündkerze erneuert", "12", "NGK B8ES", "Eigenleistung"),
                maintenance("2025-04-03", "12540", MaintenanceType.BRAKES, "Bremsbeläge vorne und hinten", "42", "Bremsbelag-Satz PX", "Vespa Werkstatt Müller"),
                maintenance("2025-06-19", "12790", MaintenanceType.BEARING, "Vorderrad-Kugellager erneuert", "38", "SKF 6203", "Eigenleistung"),
                maintenance("2025-08-30", "13020", MaintenanceType.EXHAUST, "Polini Krümmer montiert", "210", "Polini Box-Collaudata", "Tuning Point"),
                maintenance("2025-10-11", "13210", MaintenanceType.TOP_END, "Zylinderkopfdichtung erneuert", "95", "Dichtsatz original", "Vespa Werkstatt Müller"),
                maintenance("2025-12-05", "13460", MaintenanceType.ELECTRICAL, "Gleichrichter/Regler getauscht", "32", "12V Wandler", "Eigenleistung"),
                maintenance("2026-01-12", "13980", MaintenanceType.OIL_CHANGE, "Getriebeöl + Zündkerze", "57", "Öl, Zündkerze", "Eigenleistung"),
            ),
            fuelLog = fuelLog,
        )
    }

    private fun createMofaDemo(): Vehicle {
        val fuelLog = buildFuelLog(
            startDate = LocalDate.now().minusMonths(14),
            startOdometerKm = 8_920,
            kmStepRange = 55..95,
            litersRange = 2.2..3.1,
            pricePerLiterRange = 1.68..1.85,
        )
        val currentOdometer = fuelLog.last().odometerKm

        return Vehicle(
            id = MOFA_DEMO_ID,
            name = "Demo Mofa",
            brand = "Puch",
            model = "Maxi S",
            year = "1978",
            vehicleType = VehicleType.MOFA,
            frameNumber = "7801234",
            engineNumber = "5021234",
            licensePlate = "M-PM 1978",
            color = "Grün",
            purchaseDate = "22.06.2017",
            purchasePrice = "650",
            currentOdometerKm = currentOdometer,
            notes = "Demodatensatz für Präsentation und Tests. Klassisches 50er Mofa mit typischer Wartungshistorie.",
            isActive = true,
            engine = VehicleEngineSpecs(
                cycleType = EngineCycleType.TWO_STROKE,
                displacementCc = "49,9",
                boreMm = "38",
                strokeMm = "43,8",
                compressionRatio = "6,5:1",
                squishClearanceMm = "0,6",
                cylinderHead = "Standard Maxi",
                piston = "Original 50 ccm",
                intakeSystem = "Membran Bing",
                coolingType = "Luft",
                portTimingNotes = "Standard Maxi-Steuerzeiten",
                exhaustSystem = "Standard Auspuff",
                engineNotes = "Motor läuft zuverlässig, leichte Ölschlieren am Auspuff.",
            ),
            carbIgnition = VehicleCarbIgnitionSpecs(
                carbType = "Bing 12/12",
                mainJet = "52",
                pilotJet = "35",
                needle = "Standard",
                fuelMixRatio = "1:50",
                fuelType = "Super E10",
                oilType = "2-Takt",
                oilBrand = "Castrol Power 1 Racing 2T",
                sparkPlug = "Bosch W8AC",
                ignitionTimingDeg = "2,4",
                ignitionSystem = "Kontakt",
                tuningNotes = "Vergaser eingestellt für 25 km/h Dauerleistung.",
            ),
            drivetrain = VehicleDrivetrainSpecs(
                driveType = "Kette",
                frontSprocketTeeth = "11",
                rearSprocketTeeth = "52",
                chainType = "1/2\" × 3/16\"",
                chainLinks = "98",
                finalDriveNotes = "Kette alle 500 km schmieren.",
            ),
            chassis = VehicleChassisSpecs(
                frontTire = "2,25-17 Schwalbe HS113",
                rearTire = "2,50-17 Schwalbe HS113",
                tirePressureFront = "2,5",
                tirePressureRear = "2,8",
                rimFront = "17\" Stahl",
                rimRear = "17\" Stahl",
                frontBrake = "Trommel",
                rearBrake = "Trommel",
                chassisNotes = "Lenkkopflager ohne Spiel.",
            ),
            electrical = VehicleElectricalSpecs(
                battery = "6V 4 Ah",
                batteryYear = "2023",
                alternator = "Magnetzünder",
                ignitionCoil = "Standard",
                headlightBulb = "6V 25/25W",
                tailLightBulb = "6V 5W",
                electricalNotes = "Lichtanlage funktionsfähig, Blinker nachgerüstet.",
            ),
            documents = VehicleDocumentInfo(
                insuranceCompany = "HUK24",
                insurancePolicyNumber = "DEMO-987654321",
                insuranceType = "Haftpflicht",
                registrationCertNumber = "ZB II DEMO 002",
                ownerOnPaper = "Admin Demo",
                purchaseFrom = "Bauernhof, Oberbayern",
                storageLocation = "Schuppen",
                documentNotes = "Kaufvertrag und TÜV-Berichte vorhanden.",
            ),
            serviceSchedule = VehicleServiceSchedule(
                oilChangeIntervalKm = "2000",
                sparkPlugIntervalKm = "3000",
                brakeServiceIntervalKm = "5000",
                tireServiceIntervalKm = "8000",
                tuvInspectionDate = "20.05.2026",
                insuranceExpiryDate = "15.08.2026",
                lastOilChangeDate = "08.02.2026",
                lastOilChangeKm = "9850",
                nextServiceDueKm = "11850",
                nextServiceDueDate = "01.08.2026",
            ),
            maintenanceLog = listOf(
                maintenance("2024-02-10", "7650", MaintenanceType.INSPECTION_TUV, "TÜV bestanden", "55", "HU-Prüfung", "DEKRA"),
                maintenance("2024-04-18", "7890", MaintenanceType.OIL_CHANGE, "Mixöl im Tank erneuert", "8", "Castrol 2T", "Eigenleistung"),
                maintenance("2024-06-25", "8120", MaintenanceType.CARBURETOR, "Bing 12 überholt", "55", "Reparatursatz Bing", "Mofa-Service Huber"),
                maintenance("2024-08-14", "8340", MaintenanceType.TIRES, "Reifen vorne/hinten", "68", "Schwalbe HS113", "Reifen Schmidt"),
                maintenance("2024-10-03", "8560", MaintenanceType.BRAKES, "Trommelbremsen eingestellt", "25", "Bremsseil, Einstellung", "Eigenleistung"),
                maintenance("2024-12-12", "8780", MaintenanceType.BEARING, "Laufrad gelagert", "45", "Kugellager-Satz", "Mofa-Service Huber"),
                maintenance("2025-02-28", "9010", MaintenanceType.BOTTOM_END, "Kurbelwellenlager erneuert", "180", "Lager, Dichtungen", "Mofa-Service Huber"),
                maintenance("2025-05-07", "9280", MaintenanceType.EXHAUST, "Standard Auspuff getauscht", "75", "Ersatz-Auspuff Maxi", "Eigenleistung"),
                maintenance("2025-07-19", "9520", MaintenanceType.TOP_END, "Kolbenringe erneuert", "62", "Kolbenring-Satz 50cc", "Mofa-Service Huber"),
                maintenance("2025-09-30", "9760", MaintenanceType.SPARK_PLUG, "Zündkerze und Zündung eingestellt", "18", "Bosch W8AC", "Eigenleistung"),
                maintenance("2025-11-15", "9980", MaintenanceType.GENERAL, "Kette, Licht, Schrauben", "28", "Kette, Birnen", "Eigenleistung"),
                maintenance("2026-02-08", "10240", MaintenanceType.OTHER, "Tachowelle und Antrieb ersetzt", "22", "Tachowelle Maxi", "Mofa-Service Huber"),
            ),
            fuelLog = fuelLog,
        )
    }

    private fun maintenance(
        isoDate: String,
        odometerKm: String,
        type: MaintenanceType,
        description: String,
        cost: String,
        partsUsed: String,
        workshop: String,
    ): MaintenanceEntry {
        val date = LocalDate.parse(isoDate).format(dateFormatter)
        return MaintenanceEntry(
            id = UUID.randomUUID().toString(),
            date = date,
            odometerKm = odometerKm,
            type = type,
            description = description,
            cost = cost,
            partsUsed = partsUsed,
            workshop = workshop,
        )
    }

    private fun buildFuelLog(
        startDate: LocalDate,
        startOdometerKm: Int,
        kmStepRange: IntRange,
        litersRange: ClosedFloatingPointRange<Double>,
        pricePerLiterRange: ClosedFloatingPointRange<Double>,
        count: Int = 15,
    ): List<FuelLogEntry> {
        var odometer = startOdometerKm
        var date = startDate
        val entries = mutableListOf<FuelLogEntry>()

        repeat(count) { index ->
            if (index > 0) {
                odometer += kmStepRange.first + (index * 7) % (kmStepRange.last - kmStepRange.first + 1)
                date = date.plusWeeks(4)
            }
            val liters = litersRange.start + (index % 5) * 0.15
            val pricePerLiter = pricePerLiterRange.start + (index % 4) * 0.04
            val totalPrice = liters * pricePerLiter

            entries += FuelLogEntry(
                id = UUID.randomUUID().toString(),
                date = date.format(dateFormatter),
                odometerKm = odometer.toString(),
                liters = formatDecimal(liters, 1),
                price = formatDecimal(totalPrice, 2),
            )
        }

        return entries
    }

    private fun formatDecimal(value: Double, decimals: Int): String {
        return String.format(Locale.GERMANY, "%.${decimals}f", value)
    }
}
