package com.simplestsoft.twostrokecalc.domain.calculation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FuelMixCalculatorTest {

    @Test
    fun oilFromFuel_standardRatio() {
        assertEquals(100.0, FuelMixCalculator.oilMillilitersFromFuelLiters(5.0, 50)!!, 0.001)
    }

    @Test
    fun oilFromFuel_invalidInput() {
        assertNull(FuelMixCalculator.oilMillilitersFromFuelLiters(0.0, 50))
        assertNull(FuelMixCalculator.oilMillilitersFromFuelLiters(5.0, 0))
    }

    @Test
    fun fuelFromOil_standardRatio() {
        assertEquals(5.0, FuelMixCalculator.fuelLitersFromOilMilliliters(100.0, 50)!!, 0.001)
    }

    @Test
    fun snapFuelLiters_clampsToRange() {
        assertEquals(0.5, FuelMixCalculator.snapFuelLiters(0.1), 0.001)
        assertEquals(20.0, FuelMixCalculator.snapFuelLiters(25.0), 0.001)
        assertEquals(5.0, FuelMixCalculator.snapFuelLiters(5.04), 0.001)
    }

    @Test
    fun snapOilMl_clampsToRange() {
        assertEquals(5.0, FuelMixCalculator.snapOilMl(2.0), 0.001)
        assertEquals(800.0, FuelMixCalculator.snapOilMl(900.0), 0.001)
        assertEquals(100.0, FuelMixCalculator.snapOilMl(100.4), 0.001)
    }
}
