package com.simplestsoft.twostrokecalc.domain.ignitiontiming

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class IgnitionTimingCalculatorTest {

    private val strokeMm = 57.0
    private val connectingRodMm = 110.0

    @Test
    fun degreesToMm_roundTrip() {
        val degrees = 28.0
        val mm = IgnitionTimingCalculator.degreesBeforeTdcToMm(strokeMm, connectingRodMm, degrees)
        assertNotNull(mm)

        val backToDegrees = IgnitionTimingCalculator.mmBeforeTdcToDegrees(strokeMm, connectingRodMm, mm!!)
        assertEquals(degrees, backToDegrees!!, 0.01)
    }

    @Test
    fun mmToDegrees_roundTrip() {
        val mm = 2.5
        val degrees = IgnitionTimingCalculator.mmBeforeTdcToDegrees(strokeMm, connectingRodMm, mm)
        assertNotNull(degrees)

        val backToMm = IgnitionTimingCalculator.degreesBeforeTdcToMm(strokeMm, connectingRodMm, degrees!!)
        assertEquals(mm, backToMm!!, 0.01)
    }

    @Test
    fun tdc_isZeroMmAndZeroDegrees() {
        assertEquals(0.0, IgnitionTimingCalculator.degreesBeforeTdcToMm(strokeMm, connectingRodMm, 0.0)!!, 0.01)
        assertEquals(0.0, IgnitionTimingCalculator.mmBeforeTdcToDegrees(strokeMm, connectingRodMm, 0.0)!!, 0.01)
    }

    @Test
    fun bdc_isFullStrokeAnd180Degrees() {
        assertEquals(strokeMm, IgnitionTimingCalculator.degreesBeforeTdcToMm(strokeMm, connectingRodMm, 180.0)!!, 0.01)
        assertEquals(180.0, IgnitionTimingCalculator.mmBeforeTdcToDegrees(strokeMm, connectingRodMm, strokeMm)!!, 0.01)
    }

    @Test
    fun invalidInput_returnsNull() {
        assertNull(IgnitionTimingCalculator.degreesBeforeTdcToMm(0.0, connectingRodMm, 10.0))
        assertNull(IgnitionTimingCalculator.degreesBeforeTdcToMm(strokeMm, connectingRodMm, -1.0))
        assertNull(IgnitionTimingCalculator.mmBeforeTdcToDegrees(strokeMm, connectingRodMm, -1.0))
        assertNull(IgnitionTimingCalculator.mmBeforeTdcToDegrees(strokeMm, connectingRodMm, strokeMm + 1.0))
    }
}
