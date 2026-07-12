package com.simplestsoft.twostrokecalc.domain.calculation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CleaningAgentCalculatorTest {

    @Test
    fun calculate_atSevenLitersAndThreePercent() {
        val result = CleaningAgentCalculator.calculate(
            CleaningAgentCalculator.Input(
                totalLiters = 7.0,
                concentrationPercent = 3.0,
            ),
        )!!

        assertEquals(7.0, result.totalLiters, 0.001)
        assertEquals(3.0, result.concentrationPercent, 0.001)
        assertEquals(210.0, result.cleaningAgentMl, 0.01)
        assertEquals(6.79, result.waterLiters, 0.01)
    }

    @Test
    fun calculate_snapsVolumeAndConcentration() {
        val result = CleaningAgentCalculator.calculate(
            CleaningAgentCalculator.Input(
                totalLiters = 7.23,
                concentrationPercent = 2.87,
            ),
        )!!

        assertEquals(7.2, result.totalLiters, 0.001)
        assertEquals(2.9, result.concentrationPercent, 0.001)
    }

    @Test
    fun calculate_returnsNullForInvalidInput() {
        assertNull(
            CleaningAgentCalculator.calculate(
                CleaningAgentCalculator.Input(
                    totalLiters = 0.0,
                    concentrationPercent = 3.0,
                ),
            ),
        )
    }
}
