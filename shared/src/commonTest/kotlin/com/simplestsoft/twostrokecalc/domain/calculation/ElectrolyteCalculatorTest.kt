package com.simplestsoft.twostrokecalc.domain.calculation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ElectrolyteCalculatorTest {

    @Test
    fun baseRecipe_atReferenceBatch() {
        val state = ElectrolyteCalculator.referenceSliderState()
        val result = ElectrolyteCalculator.calculate(state)!!

        assertEquals(500.0, result.aceticAcidMl, 0.01)
        assertEquals(2500.0, result.waterMl, 0.01)
        assertEquals(2.5, result.waterLiters, 0.001)
        assertEquals(122.0, result.saltG, 0.01)
        assertEquals(1.0, result.scaleFactor, 0.001)
        assertEquals(10.0, result.finalAceticAcidPercent, 0.01)
        assertEquals(ElectrolyteCalculator.BASE_SALT_G_PER_L, result.saltGPerL, 0.01)
        assertEquals(ElectrolyteCalculator.BASE_SALT_G_PER_WATER_L, result.saltGPerWaterL, 0.01)
    }

    @Test
    fun syncFromTotal_updatesComposition_keepsSalt() {
        val synced = ElectrolyteCalculator.syncSliders(
            driver = ElectrolyteCalculator.SliderDriver.TOTAL,
            totalLiters = 6.0,
            waterLiters = 2.5,
            aceticAcidMl = 500.0,
            saltG = 122.0,
            acidConcentrationPercent = 60.0,
            electrificationCurrentAmps = 1.5,
        )!!

        assertEquals(6.0, synced.totalLiters, 0.01)
        assertEquals(5.0, synced.waterLiters, 0.01)
        assertEquals(1000.0, synced.aceticAcidMl, 0.01)
        assertEquals(122.0, synced.saltG, 0.01)
        assertEquals(1.5, synced.electrificationCurrentAmps, 0.01)
    }

    @Test
    fun syncFromSalt_onlyUpdatesSalt() {
        val synced = ElectrolyteCalculator.syncSliders(
            driver = ElectrolyteCalculator.SliderDriver.SALT,
            totalLiters = 3.0,
            waterLiters = 2.5,
            aceticAcidMl = 500.0,
            saltG = 244.0,
            acidConcentrationPercent = 60.0,
            electrificationCurrentAmps = 3.0,
        )!!

        assertEquals(3.0, synced.totalLiters, 0.01)
        assertEquals(500.0, synced.aceticAcidMl, 0.01)
        assertEquals(2.5, synced.waterLiters, 0.01)
        assertEquals(244.0, synced.saltG, 0.01)
        assertEquals(3.0, synced.electrificationCurrentAmps, 0.01)
    }

    @Test
    fun syncFromAcidConcentration_keepsTotalSaltAndCurrent() {
        val synced = ElectrolyteCalculator.syncSliders(
            driver = ElectrolyteCalculator.SliderDriver.ACID_CONCENTRATION,
            totalLiters = 3.0,
            waterLiters = 2.5,
            aceticAcidMl = 500.0,
            saltG = 122.0,
            acidConcentrationPercent = 40.0,
            electrificationCurrentAmps = 2.0,
        )!!

        assertEquals(3.0, synced.totalLiters, 0.01)
        assertEquals(122.0, synced.saltG, 0.01)
        assertEquals(2.0, synced.electrificationCurrentAmps, 0.01)
        assertEquals(750.0, synced.aceticAcidMl, 0.01)
        assertEquals(2.25, synced.waterLiters, 0.01)
    }

    @Test
    fun customSalt_updatesSaltConcentration() {
        val state = ElectrolyteCalculator.referenceSliderState().copy(saltG = 150.0)
        val result = ElectrolyteCalculator.calculate(state)!!

        assertEquals(150.0, result.saltG, 0.01)
        assertEquals(50.0, result.saltGPerL, 0.01)
    }

    @Test
    fun zincDissolution_scalesWithCurrent() {
        val at1A = ElectrolyteCalculator.calculateZincDissolution(
            totalLiquidLiters = 3.0,
            currentAmps = 1.0,
        )!!
        val at2A = ElectrolyteCalculator.calculateZincDissolution(
            totalLiquidLiters = 3.0,
            currentAmps = 2.0,
        )!!

        assertEquals(75.0, at1A.targetDissolvedZincG, 0.01)
        assertEquals(3.8875, at1A.voltageVolts, 0.01)
        assertEquals(4.575, at2A.voltageVolts, 0.01)
        assertEquals(at1A.electrificationHours / 2.0, at2A.electrificationHours, 0.01)
    }

    @Test
    fun voltageForLoad_atReferenceBatch() {
        val voltage = ElectrolyteCalculator.voltageForLoad(1.6, 3.0)!!
        assertEquals(4.3, voltage, 0.01)
    }

    @Test
    fun voltageForLoad_scalesWithCurrentAndVolume() {
        val atHalfCurrent = ElectrolyteCalculator.voltageForLoad(0.8, 3.0)!!
        val atDoubleVolume = ElectrolyteCalculator.voltageForLoad(1.6, 6.0)!!

        assertEquals(3.75, atHalfCurrent, 0.01)
        assertEquals(3.978, atDoubleVolume, 0.01)
    }

    @Test
    fun snapTotalLiters_roundsTo100MlSteps() {
        assertEquals(2.3, ElectrolyteCalculator.snapTotalLiters(2.25), 0.001)
        assertEquals(3.0, ElectrolyteCalculator.snapTotalLiters(3.0), 0.001)
    }

    @Test
    fun invalidInput_returnsNull() {
        assertNull(
            ElectrolyteCalculator.calculate(
                ElectrolyteCalculator.SliderState(0.0, 2.5, 500.0, 122.0, 60.0, 2.0),
            ),
        )
        assertNull(ElectrolyteCalculator.calculateZincDissolution(3.0, 0.0))
    }
}
