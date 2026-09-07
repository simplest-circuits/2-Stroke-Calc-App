package com.simplestsoft.twostrokecalc.domain.calculation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sqrt

class StingerCalculatorTest {

    @Test
    fun massFlowKgPerS_typical125cc_usesAmbientDensity() {
        val ambientPressure = 1.01325
        val ambientTemp = StingerCalculator.DEFAULT_AMBIENT_TEMP_CELSIUS
        val massFlow = StingerCalculator.massFlowKgPerS(
            displacementCc = 125.0,
            rpm = 10_000.0,
            ambientPressureBarAbs = ambientPressure,
            ambientTempCelsius = ambientTemp,
        )!!

        val volumeFlow = 125.0 * 1e-6 * 10_000.0 / 60.0
        val density = ambientPressure * 100_000.0 / (287.0 * (ambientTemp + 273.15))
        assertEquals(volumeFlow * density, massFlow, 1e-9)
        assertTrue(massFlow > 0.02)
        assertTrue(massFlow < 0.03)
    }

    @Test
    fun massFlowKgPerS_scalesWithAmbientPressureNotGauge() {
        val base = StingerCalculator.massFlowKgPerS(
            displacementCc = 125.0,
            rpm = 10_000.0,
            ambientPressureBarAbs = 1.01325,
        )!!
        val withHigherAmbient = StingerCalculator.massFlowKgPerS(
            displacementCc = 125.0,
            rpm = 10_000.0,
            ambientPressureBarAbs = 1.31325,
        )!!
        assertEquals(base * (1.31325 / 1.01325), withHigherAmbient, 1e-9)
    }

    @Test
    fun speedOfSoundMs_fromExhaustTemp() {
        val expected = sqrt(1.33 * 287.0 * 873.15)
        assertEquals(expected, StingerCalculator.speedOfSoundMs(600.0, 1.33)!!, 1e-6)
    }

    @Test
    fun calculate_typical125cc_workshopDiameter() {
        val result = StingerCalculator.calculate(
            StingerInput(
                displacementCc = 125.0,
                rpm = 10_000.0,
                exhaustGasTempCelsius = 600.0,
                gaugePressureBar = StingerCalculator.DEFAULT_GAUGE_PRESSURE_BAR,
                ambientPressureBarAbs = StingerCalculator.DEFAULT_AMBIENT_PRESSURE_BAR_ABS,
            ),
        )!!

        assertEquals(StingerFlowRegime.SUBSONIC, result.flowRegime)
        assertTrue(result.innerDiameterMm in 20.0..25.0)
        assertTrue(result.massFlowKgPerS in 0.02..0.03)
    }

    @Test
    fun calculate_higherMeanPressure_givesSmallerDiameter() {
        val lowPressure = StingerCalculator.calculate(
            StingerInput(
                displacementCc = 125.0,
                rpm = 10_000.0,
                exhaustGasTempCelsius = 600.0,
                gaugePressureBar = 0.05,
                ambientPressureBarAbs = 1.01325,
            ),
        )!!
        val highPressure = StingerCalculator.calculate(
            StingerInput(
                displacementCc = 125.0,
                rpm = 10_000.0,
                exhaustGasTempCelsius = 600.0,
                gaugePressureBar = 0.30,
                ambientPressureBarAbs = 1.01325,
            ),
        )!!

        assertEquals(lowPressure.massFlowKgPerS, highPressure.massFlowKgPerS, 1e-9)
        assertTrue(highPressure.innerDiameterMm < lowPressure.innerDiameterMm)
    }

    @Test
    fun calculate_subsonicExample() {
        val gamma = 1.33
        val ambient = 1.01325
        val gauge = 0.05
        val p0 = ambient + gauge
        val displacement = 125.0
        val rpm = 10_000.0
        val exhaustTemp = 600.0

        val massFlow = StingerCalculator.massFlowKgPerS(
            displacementCc = displacement,
            rpm = rpm,
            ambientPressureBarAbs = ambient,
        )!!
        val a0 = StingerCalculator.speedOfSoundMs(exhaustTemp, gamma)!!

        val result = StingerCalculator.calculate(
            StingerInput(
                displacementCc = displacement,
                rpm = rpm,
                exhaustGasTempCelsius = exhaustTemp,
                gaugePressureBar = gauge,
                ambientPressureBarAbs = ambient,
                specificHeatRatio = gamma,
            ),
        )!!

        val expectedMach = StingerCalculator.machFromPressureRatio(ambient / p0, gamma)!!
        val expectedCritical = StingerCalculator.criticalPressureRatio(gamma)!!
        val expectedMassFlux = expectedMassFlux(p0 * 100_000.0, a0, expectedMach, gamma)
        val expectedAreaM2 = massFlow / expectedMassFlux
        val expectedDiameterMm = sqrt(4.0 * expectedAreaM2 / PI) * 1_000.0

        assertEquals(StingerFlowRegime.SUBSONIC, result.flowRegime)
        assertEquals(massFlow, result.massFlowKgPerS, 1e-9)
        assertEquals(a0, result.speedOfSoundMs, 1e-6)
        assertEquals(p0, result.stagnationPressureBarAbs, 1e-9)
        assertEquals(ambient / p0, result.pressureRatio, 1e-9)
        assertEquals(expectedCritical, result.criticalPressureRatio, 1e-9)
        assertEquals(expectedMach, result.machNumber, 1e-9)
        assertTrue(result.machNumber < 1.0)
        assertEquals(expectedMassFlux, result.massFluxKgPerM2S, 1e-6)
        assertEquals(expectedAreaM2 * 1_000_000.0, result.areaMm2, 1e-4)
        assertEquals(expectedDiameterMm, result.innerDiameterMm, 1e-6)
        assertTrue(result.exitVelocityMs > 0.0)
        assertTrue(result.exitVelocityMs < a0)
    }

    @Test
    fun calculate_chokedExample() {
        val gamma = 1.33
        val ambient = 1.01325
        val gauge = 1.50
        val p0 = ambient + gauge
        val displacement = 250.0
        val rpm = 12_000.0
        val exhaustTemp = 650.0

        val massFlow = StingerCalculator.massFlowKgPerS(
            displacementCc = displacement,
            rpm = rpm,
            ambientPressureBarAbs = ambient,
        )!!
        val a0 = StingerCalculator.speedOfSoundMs(exhaustTemp, gamma)!!

        val result = StingerCalculator.calculate(
            StingerInput(
                displacementCc = displacement,
                rpm = rpm,
                exhaustGasTempCelsius = exhaustTemp,
                gaugePressureBar = gauge,
                ambientPressureBarAbs = ambient,
                specificHeatRatio = gamma,
            ),
        )!!

        val expectedCritical = StingerCalculator.criticalPressureRatio(gamma)!!
        assertTrue(ambient / p0 <= expectedCritical)
        assertEquals(StingerFlowRegime.CHOKED, result.flowRegime)
        assertEquals(1.0, result.machNumber, 1e-12)
        assertEquals(massFlow, result.massFlowKgPerS, 1e-9)

        val expectedMassFlux = expectedMassFlux(p0 * 100_000.0, a0, 1.0, gamma)
        assertEquals(expectedMassFlux, result.massFluxKgPerM2S, 1e-6)
        assertEquals(massFlow / expectedMassFlux * 1_000_000.0, result.areaMm2, 1e-4)
    }

    @Test
    fun calculate_atCriticalPressureRatio_isChoked() {
        val gamma = 1.33
        val ambient = 1.01325
        val critical = StingerCalculator.criticalPressureRatio(gamma)!!
        val p0 = ambient / critical
        val gauge = p0 - ambient

        val result = StingerCalculator.calculate(
            StingerInput(
                displacementCc = 125.0,
                rpm = 10_000.0,
                exhaustGasTempCelsius = 600.0,
                gaugePressureBar = gauge,
                ambientPressureBarAbs = ambient,
                specificHeatRatio = gamma,
            ),
        )!!

        assertEquals(StingerFlowRegime.CHOKED, result.flowRegime)
        assertEquals(1.0, result.machNumber, 1e-9)
        assertEquals(critical, result.pressureRatio, 1e-9)
    }

    @Test
    fun criticalPressureRatio_gamma133() {
        val expected = (2.0 / 2.33).pow(1.33 / 0.33)
        assertEquals(expected, StingerCalculator.criticalPressureRatio(1.33)!!, 1e-12)
    }

    @Test
    fun calculate_invalidInput() {
        assertNull(
            StingerCalculator.calculate(
                StingerInput(
                    displacementCc = 0.0,
                    rpm = 10_000.0,
                    exhaustGasTempCelsius = 600.0,
                    gaugePressureBar = 0.05,
                    ambientPressureBarAbs = 1.01325,
                ),
            ),
        )
        assertNull(
            StingerCalculator.calculate(
                StingerInput(
                    displacementCc = 125.0,
                    rpm = 10_000.0,
                    exhaustGasTempCelsius = 600.0,
                    gaugePressureBar = 0.0,
                    ambientPressureBarAbs = 1.01325,
                ),
            ),
        )
        assertNull(
            StingerCalculator.calculate(
                StingerInput(
                    displacementCc = 125.0,
                    rpm = 10_000.0,
                    exhaustGasTempCelsius = 600.0,
                    gaugePressureBar = 0.05,
                    ambientPressureBarAbs = 1.01325,
                    specificHeatRatio = 1.0,
                ),
            ),
        )
    }

    private fun expectedMassFlux(
        stagnationPressurePa: Double,
        speedOfSoundMs: Double,
        mach: Double,
        gamma: Double,
    ): Double {
        val factor = 1.0 + (gamma - 1.0) / 2.0 * mach * mach
        val exponent = -(gamma + 1.0) / (2.0 * (gamma - 1.0))
        return stagnationPressurePa * (gamma / speedOfSoundMs) * mach * factor.pow(exponent)
    }
}
