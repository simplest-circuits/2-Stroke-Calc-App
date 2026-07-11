package com.simplestsoft.twostrokecalc.domain.vehicles

import com.simplestsoft.twostrokecalc.domain.model.FuelLogEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class VehicleOdometerSyncTest {

    @Test
    fun syncFromFuelLog_updatesOdometerWhenFuelEntryIsHigher() {
        val fuelLog = listOf(
            FuelLogEntry(odometerKm = "12000"),
            FuelLogEntry(odometerKm = "12500"),
        )

        val result = VehicleOdometerSync.syncFromFuelLog("10000", "", fuelLog)

        assertEquals("12500", result.currentOdometerKm)
        assertEquals("", result.currentOperatingHours)
    }

    @Test
    fun syncFromFuelLog_keepsHigherManualOdometer() {
        val fuelLog = listOf(FuelLogEntry(odometerKm = "12000"))

        val result = VehicleOdometerSync.syncFromFuelLog("15000", "", fuelLog)

        assertEquals("15000", result.currentOdometerKm)
    }

    @Test
    fun syncFromFuelLog_updatesOperatingHoursWhenFuelEntryIsHigher() {
        val fuelLog = listOf(
            FuelLogEntry(operatingHours = "120"),
            FuelLogEntry(operatingHours = "145"),
        )

        val result = VehicleOdometerSync.syncFromFuelLog("", "100", fuelLog)

        assertEquals("", result.currentOdometerKm)
        assertEquals("145", result.currentOperatingHours)
    }

    @Test
    fun syncFromFuelLog_keepsHigherManualOperatingHours() {
        val fuelLog = listOf(FuelLogEntry(operatingHours = "120"))

        val result = VehicleOdometerSync.syncFromFuelLog("", "200", fuelLog)

        assertEquals("200", result.currentOperatingHours)
    }

    @Test
    fun syncFromFuelLog_syncsKmAndHoursIndependently() {
        val fuelLog = listOf(
            FuelLogEntry(odometerKm = "8000"),
            FuelLogEntry(operatingHours = "42"),
        )

        val result = VehicleOdometerSync.syncFromFuelLog("7500", "30", fuelLog)

        assertEquals("8000", result.currentOdometerKm)
        assertEquals("42", result.currentOperatingHours)
    }

    @Test
    fun syncFromFuelLog_setsValueWhenCurrentIsBlank() {
        val fuelLog = listOf(FuelLogEntry(odometerKm = "5000"))

        val result = VehicleOdometerSync.syncFromFuelLog("", "", fuelLog)

        assertEquals("5000", result.currentOdometerKm)
    }
}
