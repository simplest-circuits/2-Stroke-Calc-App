package com.simplestsoft.twostrokecalc.domain.portarea

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExhaustPortOptimizerTest {

    @Test
    fun optimize_twin_series125_matchesWikiChord() {
        val result = ExhaustPortOptimizer.optimize(
            ExhaustPortOptimizer.Request(
                boreMm = 54.0,
                strokeMm = 57.0,
                durationDeg = 166.0,
                portHeightMm = 20.0,
                type = ExhaustPortType.TWIN,
                ringProfile = ExhaustPortOptimizer.RingProfile.SERIES_SPORT,
                bridgeWidthMm = 8.0,
                topEdgeRadiusMm = 3.0,
            ),
        )!!

        assertEquals(48.0, result.chordMetrics.topChordMm, 0.5)
        assertEquals(ExhaustPortType.TWIN, result.input.type)
        assertEquals(ExhaustPortShape.TRAPEZOID, result.input.shape)
        assertTrue(PortAreaCalculator.layoutHorizontalSpansConsistent(result.layout))
    }

    @Test
    fun optimize_triple_respectsChordLimit() {
        val result = ExhaustPortOptimizer.optimize(
            ExhaustPortOptimizer.Request(
                boreMm = 54.0,
                strokeMm = 57.0,
                durationDeg = 166.0,
                portHeightMm = 20.0,
                type = ExhaustPortType.TRIPLE,
                ringProfile = ExhaustPortOptimizer.RingProfile.CHROME,
                bridgeWidthMm = 6.0,
            ),
        )!!

        assertTrue(result.chordMetrics.chordPercentOfBore <= 70.5)
        assertEquals(3, result.layout.windows.size)
    }

    @Test
    fun topEdgeAreaCorrection_increasesArea() {
        val flat = PortAreaCalculator.portWindowAreaMm2(
            shape = ExhaustPortShape.TRAPEZOID,
            bottomWidthMm = 10.0,
            topWidthMm = 20.0,
            heightMm = 20.0,
        )!!
        val curved = PortAreaCalculator.portWindowAreaMm2(
            shape = ExhaustPortShape.TRAPEZOID,
            bottomWidthMm = 10.0,
            topWidthMm = 20.0,
            heightMm = 20.0,
            radii = PortWindowRadii(topEdgeRadiusMm = 3.0),
        )!!
        assertTrue(curved > flat)
    }
}
