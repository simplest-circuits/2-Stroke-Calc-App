package com.simplestsoft.twostrokecalc.domain.calculation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CompressionCalculatorTest {

    @Test
    fun calculate_excelExample() {
        val result = CompressionCalculator.calculate(
            boreMm = 72.0,
            domeDiameterMm = 50.0,
            strokeMm = 62.0,
            domeVolumeMl = 18.0,
            squishBandMm = 1.2,
        )!!

        assertEquals(252.425808, result.displacementMl, 0.001)
        assertEquals(4.8856608, result.squishBandVolumeMl, 0.001)
        assertEquals(12.02986757542085, result.compressionRatio, 0.001)
        assertEquals(51.7746913580247, result.squishAreaPercent, 0.001)
        assertEquals(1.1612903225806452, result.boreStrokeRatio, 0.001)
    }

    @Test
    fun calculate_invalidInput() {
        assertNull(
            CompressionCalculator.calculate(
                boreMm = 0.0,
                domeDiameterMm = 50.0,
                strokeMm = 62.0,
                domeVolumeMl = 18.0,
                squishBandMm = 1.2,
            ),
        )
        assertNull(
            CompressionCalculator.calculate(
                boreMm = 72.0,
                domeDiameterMm = 50.0,
                strokeMm = 62.0,
                domeVolumeMl = 0.0,
                squishBandMm = 0.0,
            ),
        )
    }

    @Test
    fun calculateTarget_kr26Example() {
        val result = CompressionCalculator.calculateTarget(
            boreMm = 67.0,
            strokeMm = 70.0,
            targetCompressionRatio = 7.5,
            pistonConstantMl = 2.3,
        )!!

        assertEquals(246.78838625, result.displacementMl, 0.001)
        assertEquals(37.96675173, result.totalChamberVolumeMl, 0.01)
        assertEquals(35.66675173, result.domeVolumeMl!!, 0.01)
    }

    @Test
    fun calculateTarget_withoutConstant() {
        val result = CompressionCalculator.calculateTarget(
            boreMm = 67.0,
            strokeMm = 70.0,
            targetCompressionRatio = 6.2,
        )!!

        assertEquals(47.45930505, result.totalChamberVolumeMl, 0.01)
        assertEquals(null, result.domeVolumeMl)
    }

    @Test
    fun calculateTarget_invalidInput() {
        assertNull(
            CompressionCalculator.calculateTarget(
                boreMm = 67.0,
                strokeMm = 70.0,
                targetCompressionRatio = 1.0,
            ),
        )
        assertNull(
            CompressionCalculator.calculateTarget(
                boreMm = 67.0,
                strokeMm = 70.0,
                targetCompressionRatio = 7.5,
                pistonConstantMl = -1.0,
            ),
        )
    }

    @Test
    fun calculateChange_kr26Example() {
        val result = CompressionCalculator.calculateChange(
            boreMm = 67.0,
            strokeMm = 70.0,
            currentCompressionRatio = 7.2,
            targetCompressionRatio = 7.5,
        )!!

        assertEquals(246.78838625, result.displacementMl, 0.001)
        assertEquals(39.80457843, result.currentChamberVolumeMl, 0.01)
        assertEquals(37.96675173, result.targetChamberVolumeMl, 0.01)
        assertEquals(1.8378267, result.volumeToRemoveMl, 0.01)
        assertEquals(0.52, result.millingDepthMm, 0.01)
    }

    @Test
    fun calculateChange_lowerTargetRequiresAddingVolume() {
        val result = CompressionCalculator.calculateChange(
            boreMm = 72.0,
            strokeMm = 62.0,
            currentCompressionRatio = 12.0,
            targetCompressionRatio = 10.0,
        )!!

        assertTrue(result.volumeToRemoveMl < 0.0)
    }

    @Test
    fun calculateChange_invalidInput() {
        assertNull(
            CompressionCalculator.calculateChange(
                boreMm = 67.0,
                strokeMm = 70.0,
                currentCompressionRatio = 0.5,
                targetCompressionRatio = 7.5,
            ),
        )
    }
}
