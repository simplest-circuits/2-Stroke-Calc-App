package com.simplestsoft.twostrokecalc.domain.calculation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CarbJetCorrectionCalculatorTest {

    @Test
    fun sameConditions_noCorrection() {
        val result = CarbJetCorrectionCalculator.calculate(
            baseMainJet = 165,
            referenceAltitudeM = 0.0,
            referenceTemperatureC = 15.0,
            targetAltitudeM = 0.0,
            targetTemperatureC = 15.0,
        )

        assertNotNull(result)
        assertEquals(1.0, result!!.correctionFactor, 0.001)
        assertEquals(165, result.correctedMainJet)
    }

    @Test
    fun higherAltitudeAndTemperature_smallerJet() {
        val result = CarbJetCorrectionCalculator.calculate(
            baseMainJet = 165,
            referenceAltitudeM = 0.0,
            referenceTemperatureC = 15.0,
            targetAltitudeM = 2073.0,
            targetTemperatureC = 20.0,
        )

        assertNotNull(result)
        assertEquals(154, result!!.correctedMainJet)
        assertEquals(0.94, result.correctionFactor, 0.01)
    }

    @Test
    fun mediumAltitude_matchesTypicalChart() {
        val result = CarbJetCorrectionCalculator.calculate(
            baseMainJet = 260,
            referenceAltitudeM = 0.0,
            referenceTemperatureC = 15.0,
            targetAltitudeM = 2000.0,
            targetTemperatureC = 15.0,
        )

        assertNotNull(result)
        assertEquals(245, result!!.correctedMainJet)
    }

    @Test
    fun invalidInput() {
        assertNull(
            CarbJetCorrectionCalculator.calculate(
                baseMainJet = 0,
                referenceAltitudeM = 0.0,
                referenceTemperatureC = 15.0,
                targetAltitudeM = 1000.0,
                targetTemperatureC = 20.0,
            ),
        )
        assertNull(
            CarbJetCorrectionCalculator.calculate(
                baseMainJet = 165,
                referenceAltitudeM = 0.0,
                referenceTemperatureC = -60.0,
                targetAltitudeM = 1000.0,
                targetTemperatureC = 20.0,
            ),
        )
    }
}
