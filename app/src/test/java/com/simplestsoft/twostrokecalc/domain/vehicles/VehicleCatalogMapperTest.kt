package com.simplestsoft.twostrokecalc.domain.vehicles

import com.simplestsoft.twostrokecalc.domain.model.EngineCycleType
import com.simplestsoft.twostrokecalc.domain.model.VehicleCatalogCarbIgnition
import com.simplestsoft.twostrokecalc.domain.model.VehicleCatalogEngine
import com.simplestsoft.twostrokecalc.domain.model.VehicleCatalogEntry
import com.simplestsoft.twostrokecalc.domain.model.VehicleType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VehicleCatalogMapperTest {

    @Test
    fun `maps catalog entry to vehicle with engine and carb specs`() {
        val entry = VehicleCatalogEntry(
            id = "vespa_px_125_e",
            brand = "Vespa",
            model = "PX 125 E",
            variant = "Elestart",
            frameCode = "VNX2T",
            category = "Klassik Vespa",
            yearFrom = 1981,
            yearTo = 1997,
            vehicleType = "ROLLER",
            cycleType = "TWO_STROKE",
            engine = VehicleCatalogEngine(
                displacementCc = "123,4",
                boreMm = "52,5",
                strokeMm = "57",
                compressionRatio = "8,2:1",
                coolingType = "Luft",
            ),
            carbIgnition = VehicleCatalogCarbIgnition(
                carbType = "Dell'Orto SI 20/20D",
                mainJet = "99",
                pilotJet = "45",
                sparkPlug = "NGK B6HS",
            ),
        )

        val vehicle = VehicleCatalogMapper.toVehicle(entry, year = "1985")

        assertEquals("Vespa", vehicle.brand)
        assertEquals("PX 125 E", vehicle.model)
        assertEquals("1985", vehicle.year)
        assertEquals(VehicleType.ROLLER, vehicle.vehicleType)
        assertEquals(EngineCycleType.TWO_STROKE, vehicle.engine.cycleType)
        assertEquals("123,4", vehicle.engine.displacementCc)
        assertEquals("52,5", vehicle.engine.boreMm)
        assertEquals("57", vehicle.engine.strokeMm)
        assertEquals("Dell'Orto SI 20/20D", vehicle.carbIgnition.carbType)
        assertEquals("99", vehicle.carbIgnition.mainJet)
        assertTrue(vehicle.notes.contains("VNX2T"))
    }
}
