package com.simplestsoft.twostrokecalc.domain.calculation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DcCableCrossSectionCalculatorTest {

    @Test
    fun currentFromPower_valid() {
        assertEquals(10.0, DcCableCrossSectionCalculator.currentFromPower(120.0, 12.0)!!, 0.001)
    }

    @Test
    fun currentFromPower_invalid() {
        assertNull(DcCableCrossSectionCalculator.currentFromPower(0.0, 12.0))
        assertNull(DcCableCrossSectionCalculator.currentFromPower(120.0, 0.0))
    }

    @Test
    fun twelveVoltTenAmpThreeMeterThreePercent_recommendsFourMm2() {
        val result = DcCableCrossSectionCalculator.calculate(
            voltage = 12.0,
            currentAmps = 10.0,
            oneWayLengthM = 3.0,
            maxDropPercent = 3.0,
        )

        assertNotNull(result)
        assertEquals(2.92, result!!.minimumCrossSectionMm2, 0.05)
        assertEquals(4.0, result.recommendedCrossSectionMm2, 0.001)
        assertEquals(10.0, result.currentAmps, 0.001)
        assertEquals(0.36, result.maxAllowedDropVolts, 0.01)
        assertEquals(2.19, result.voltageDropPercent, 0.05)
    }

    @Test
    fun powerInput_matchesCurrentCalculation() {
        val current = DcCableCrossSectionCalculator.currentFromPower(60.0, 12.0)
        val result = DcCableCrossSectionCalculator.calculate(
            voltage = 12.0,
            currentAmps = current!!,
            oneWayLengthM = 2.0,
            maxDropPercent = 5.0,
        )

        assertNotNull(result)
        assertEquals(5.0, result!!.currentAmps, 0.001)
        assertEquals(0.75, result.recommendedCrossSectionMm2, 0.001)
    }

    @Test
    fun invalidInput_returnsNull() {
        assertNull(
            DcCableCrossSectionCalculator.calculate(
                voltage = 0.0,
                currentAmps = 5.0,
                oneWayLengthM = 1.0,
                maxDropPercent = 3.0,
            ),
        )
        assertNull(
            DcCableCrossSectionCalculator.calculate(
                voltage = 12.0,
                currentAmps = 5.0,
                oneWayLengthM = 0.0,
                maxDropPercent = 3.0,
            ),
        )
    }
}
