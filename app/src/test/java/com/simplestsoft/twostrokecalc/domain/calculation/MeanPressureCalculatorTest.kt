package com.simplestsoft.twostrokecalc.domain.calculation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MeanPressureCalculatorTest {

    @Test
    fun calculate_excelExample() {
        val result = MeanPressureCalculator.calculate(
            powerPs = 100.0,
            rpm = 10500.0,
            boreMm = 90.0,
            strokeMm = 64.0,
        )!!

        assertEquals(73529.41176470589, result.powerWatts, 0.01)
        assertEquals(175.0, result.rpmPerSecond, 0.01)
        assertEquals(66.87182596923782, result.torqueNm, 0.001)
        assertEquals(407.15040096, result.displacementCcm, 0.001)
        assertEquals(0.40715040096, result.displacementDm3, 0.0000001)
        assertEquals(10.31972622982065, result.meanPressureBar, 0.001)
        assertEquals(245.6094842697315, result.specificPowerPsPerLiter, 0.001)
    }

    @Test
    fun calculate_invalidInput() {
        assertNull(
            MeanPressureCalculator.calculate(
                powerPs = 0.0,
                rpm = 10500.0,
                boreMm = 90.0,
                strokeMm = 64.0,
            ),
        )
        assertNull(
            MeanPressureCalculator.calculate(
                powerPs = 100.0,
                rpm = 10500.0,
                boreMm = 0.0,
                strokeMm = 64.0,
            ),
        )
    }
}
