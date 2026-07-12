package com.simplestsoft.twostrokecalc.domain.calculation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CounterweightFactorCalculatorTest {

    @Test
    fun factorPercent_excelExample() {
        val result = CounterweightFactorCalculator.factorPercent(
            pistonWeightGrams = 303.0,
            connectingRodHalfGrams = 60.0,
            bigEndWeightGrams = 71.0,
        )
        assertEquals(36.09, result!!, 0.01)
    }

    @Test
    fun factorPercent_invalidInput() {
        assertNull(
            CounterweightFactorCalculator.factorPercent(
                pistonWeightGrams = 0.0,
                connectingRodHalfGrams = 60.0,
                bigEndWeightGrams = 71.0,
            ),
        )
        assertNull(
            CounterweightFactorCalculator.factorPercent(
                pistonWeightGrams = -10.0,
                connectingRodHalfGrams = 60.0,
                bigEndWeightGrams = 71.0,
            ),
        )
    }
}
