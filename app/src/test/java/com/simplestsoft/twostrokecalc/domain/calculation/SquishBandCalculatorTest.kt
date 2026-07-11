package com.simplestsoft.twostrokecalc.domain.calculation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SquishBandCalculatorTest {

    @Test
    fun calculateFromPercent_excelExample() {
        val result = SquishBandCalculator.calculateFromPercent(
            boreMm = 72.0,
            squishAreaPercent = 50.0,
        )!!

        assertEquals(50.0, result.squishAreaPercent, 0.001)
        assertEquals(50.912, result.domeDiameterMm, 0.01)
        assertEquals(10.544, result.squishBandWidthMm, 0.01)
    }

    @Test
    fun calculateFromWidth_excelExample() {
        val result = SquishBandCalculator.calculateFromWidth(
            boreMm = 72.0,
            squishBandWidthMm = 11.0,
        )!!

        assertEquals(51.7746913580247, result.squishAreaPercent, 0.001)
        assertEquals(11.0, result.squishBandWidthMm, 0.001)
        assertEquals(50.0, result.domeDiameterMm, 0.001)
    }

    @Test
    fun calculateFromPercent_invalidInput() {
        assertNull(
            SquishBandCalculator.calculateFromPercent(
                boreMm = 0.0,
                squishAreaPercent = 50.0,
            ),
        )
        assertNull(
            SquishBandCalculator.calculateFromPercent(
                boreMm = 72.0,
                squishAreaPercent = 100.0,
            ),
        )
    }

    @Test
    fun calculateFromWidth_invalidInput() {
        assertNull(
            SquishBandCalculator.calculateFromWidth(
                boreMm = 72.0,
                squishBandWidthMm = 0.0,
            ),
        )
        assertNull(
            SquishBandCalculator.calculateFromWidth(
                boreMm = 72.0,
                squishBandWidthMm = 36.0,
            ),
        )
    }
}
