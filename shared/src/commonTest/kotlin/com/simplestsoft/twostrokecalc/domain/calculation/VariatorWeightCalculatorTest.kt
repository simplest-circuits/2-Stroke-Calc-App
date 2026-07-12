package com.simplestsoft.twostrokecalc.domain.calculation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VariatorWeightCalculatorTest {

    @Test
    fun weightForTargetRpmPhysics_8g_7000_to_7500() {
        val result = VariatorWeightCalculator.weightForTargetRpmPhysics(
            currentGrams = 8.0,
            currentRpm = 7000.0,
            targetRpm = 7500.0,
        )
        assertEquals(6.97, result!!, 0.05)
    }

    @Test
    fun weightForRpmDeltaEmpirical_8g_plus500_rpm_rollers() {
        val result = VariatorWeightCalculator.weightForRpmDeltaEmpirical(
            currentGrams = 8.0,
            rpmDelta = 500.0,
            rollerType = VariatorWeightRollerType.ROLLERS,
        )
        assertEquals(7.09, result!!, 0.05)
    }

    @Test
    fun rpmDeltaForWeightChangeEmpirical_halfGramLighter_rollers() {
        val rpmDelta = VariatorWeightCalculator.rpmDeltaForWeightChangeEmpirical(
            weightDeltaGrams = -0.5,
            rollerType = VariatorWeightRollerType.ROLLERS,
        )
        assertEquals(275.0, rpmDelta, 0.01)
    }

    @Test
    fun weightForRpmDeltaEmpirical_sliders_lessSensitive() {
        val result = VariatorWeightCalculator.weightForRpmDeltaEmpirical(
            currentGrams = 8.0,
            rpmDelta = 500.0,
            rollerType = VariatorWeightRollerType.SLIDERS,
        )
        assertEquals(6.18, result!!, 0.05)
    }

    @Test
    fun calculate_combinesBothModes() {
        val result = VariatorWeightCalculator.calculate(
            currentGrams = 8.0,
            currentRpm = 7000.0,
            targetRpm = 7500.0,
            rollerType = VariatorWeightRollerType.ROLLERS,
        )
        assertEquals(6.97, result!!.physicsWeightGrams, 0.05)
        assertEquals(7.09, result.empiricalWeightGrams, 0.05)
        assertEquals(7500.0, result.targetRpm, 0.01)
        assertEquals(500.0, result.rpmDelta, 0.01)
    }

    @Test
    fun invalidInput_returnsNull() {
        assertNull(
            VariatorWeightCalculator.weightForTargetRpmPhysics(
                currentGrams = 0.0,
                currentRpm = 7000.0,
                targetRpm = 7500.0,
            ),
        )
        assertNull(
            VariatorWeightCalculator.weightForRpmDeltaEmpirical(
                currentGrams = 0.5,
                rpmDelta = 2000.0,
                rollerType = VariatorWeightRollerType.ROLLERS,
            ),
        )
        assertNull(
            VariatorWeightCalculator.targetRpmFromDelta(
                currentRpm = 1000.0,
                rpmDelta = -2000.0,
            ),
        )
    }

    @Test
    fun roundToTenth_roundsCorrectly() {
        assertEquals(7.0, VariatorWeightCalculator.roundToTenth(6.97), 0.001)
        assertEquals(7.1, VariatorWeightCalculator.roundToTenth(7.09), 0.001)
    }
}
