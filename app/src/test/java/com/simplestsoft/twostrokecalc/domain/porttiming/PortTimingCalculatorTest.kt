package com.simplestsoft.twostrokecalc.domain.porttiming

import com.simplestsoft.twostrokecalc.ui.porttiming.formatPortTimingDegrees
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PortTimingCalculatorTest {

    @Test
    fun defaultExample_matchesReferenceValues() {
        val result = PortTimingCalculator.calculate(
            PortTimingInput(
                strokeMm = 57.0,
                connectingRodMm = 110.0,
                exhaustPortMm = 36.2,
                transferPortMm = 47.5,
                intakeOpenMm = 44.2,
                intakeCloseMm = 15.9,
                pistonDeckMm = -0.5,
                intakeSystem = IntakeSystem.ROTARY_VALVE,
            ),
        )

        assertNotNull(result.exhaust)
        assertNotNull(result.transfer)
        assertNotNull(result.intake)
        assertEquals(165.89, result.exhaust!!.duration, 0.01)
        assertEquals(112.03, result.transfer!!.duration, 0.01)
        assertEquals(26.93, result.blowdown!!, 0.01)
        assertEquals(115.20, result.intake!!.openBeforeTdc, 0.01)
        assertEquals(56.57, result.intake!!.closeAfterTdc, 0.01)
        assertEquals(171.77, result.intake!!.duration, 0.01)
    }

    @Test
    fun reedValve_omitsIntakeTimings() {
        val result = PortTimingCalculator.calculate(
            PortTimingInput(intakeSystem = IntakeSystem.REED_VALVE),
        )

        assertNotNull(result.exhaust)
        assertNotNull(result.transfer)
        assertNull(result.intake)
    }

    @Test
    fun formatDegrees_roundsWhenRequested() {
        assertEquals("166°", formatPortTimingDegrees(165.89, round = true))
        assertEquals("165,89°", formatPortTimingDegrees(165.89, round = false))
    }
}
