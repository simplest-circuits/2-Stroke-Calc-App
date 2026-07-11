package com.simplestsoft.twostrokecalc.domain.portarea

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.math.PI

class PortAreaCalculatorTest {

    @Test
    fun calculateExhaust_twinTrapezoid_wikiExample() {
        val layout = PortAreaCalculator.calculateExhaustLayout(
            ExhaustPortInput(
                boreMm = 54.0,
                strokeMm = 57.0,
                type = ExhaustPortType.TWIN,
                shape = ExhaustPortShape.TRAPEZOID,
                portHeightMm = 20.0,
                totalSpanMm = 28.0,
                topSpanMm = 48.0,
                bridgeWidthMm = 8.0,
                durationDeg = 166.0,
            ),
        )!!

        assertEquals(10.0, layout.windows.first().bottomWidthMm, 0.01)
        assertEquals(20.0, layout.windows.first().topWidthMm, 0.01)
        assertEquals(20.0, layout.windows.first().heightMm, 0.01)
        assertEquals(600.0, layout.totalAreaMm2, 0.01)

        val metrics = PortAreaCalculator.exhaustChordMetrics(54.0, layout)!!
        assertEquals(48.0, metrics.topChordMm, 0.01)
        assertEquals(28.0, metrics.bottomWidthUtMm, 0.01)
        assertEquals(88.89, metrics.chordPercentOfBore, 0.1)
        assertEquals(ExhaustChordRating.SERIES_SPORT, PortAreaCalculator.rateChordPercent(metrics.chordPercentOfBore))
    }

    @Test
    fun wikiTwinDefaults_scalesWithBoreAndBridge() {
        val (bottom, chord) = PortAreaCalculator.wikiTwinDefaultsForBore(54.0, 8.0)
        assertEquals(28.0, bottom, 0.01)
        assertEquals(48.0, chord, 0.01)
    }

    @Test
    fun layoutHorizontalSpans_matchDiagramTotals() {
        val twin = PortAreaCalculator.calculateExhaustLayout(
            ExhaustPortInput(
                boreMm = 54.0,
                strokeMm = 57.0,
                type = ExhaustPortType.TWIN,
                shape = ExhaustPortShape.TRAPEZOID,
                portHeightMm = 20.0,
                totalSpanMm = 28.0,
                topSpanMm = 48.0,
                bridgeWidthMm = 8.0,
                durationDeg = 166.0,
            ),
        )!!
        assertEquals(true, PortAreaCalculator.layoutHorizontalSpansConsistent(twin))
        assertEquals(48.0, twin.totalTopSpanMm, 0.01)
        assertEquals(28.0, twin.totalBottomSpanMm, 0.01)

        val triple = PortAreaCalculator.calculateExhaustLayout(
            ExhaustPortInput(
                boreMm = 56.0,
                strokeMm = 50.6,
                type = ExhaustPortType.TRIPLE,
                portHeightMm = 20.0,
                totalSpanMm = 0.0,
                bridgeWidthMm = 6.0,
                mainBottomSpanMm = 24.0,
                mainTopSpanMm = 32.0,
                boosterTopSpanMm = 10.0,
                boosterHeightMm = 10.0,
                boosterShape = ExhaustPortShape.TRIANGULAR,
                durationDeg = 180.0,
            ),
        )!!
        assertEquals(true, PortAreaCalculator.layoutHorizontalSpansConsistent(triple))
    }

    @Test
    fun calculateExhaust_tripleCompound_shorterBoostersTopAligned() {
        val layout = PortAreaCalculator.calculateExhaustLayout(
            ExhaustPortInput(
                boreMm = 56.0,
                strokeMm = 50.6,
                type = ExhaustPortType.TRIPLE,
                portHeightMm = 20.0,
                totalSpanMm = 0.0,
                bridgeWidthMm = 6.0,
                mainBottomSpanMm = 24.0,
                mainTopSpanMm = 32.0,
                boosterTopSpanMm = 10.0,
                boosterHeightMm = 10.0,
                boosterShape = ExhaustPortShape.TRIANGULAR,
                durationDeg = 180.0,
            ),
        )!!

        assertEquals(3, layout.windows.size)
        val main = layout.windows[1]
        val booster = layout.windows[0]
        assertEquals(PortWindowRole.MAIN, main.roleLabelKey)
        assertEquals(PortWindowRole.BOOSTER, booster.roleLabelKey)
        assertEquals(20.0, main.heightMm, 0.01)
        assertEquals(10.0, booster.heightMm, 0.01)
        assertEquals(true, booster.topAlignedWithMain)
        assertEquals(560.0, main.areaMm2, 0.01)
        assertEquals(50.0, booster.areaMm2, 0.01)
        assertEquals(660.0, layout.totalAreaMm2, 0.01)
        assertEquals(10.0, layout.boosterHeightMm!!, 0.01)
    }

    @Test
    fun calculateExhaust_tripleCompound_trapezoidBoosters() {
        val layout = PortAreaCalculator.calculateExhaustLayout(
            ExhaustPortInput(
                boreMm = 54.0,
                strokeMm = 57.0,
                type = ExhaustPortType.TRIPLE,
                portHeightMm = 18.0,
                totalSpanMm = 0.0,
                bridgeWidthMm = 5.0,
                mainBottomSpanMm = 22.0,
                mainTopSpanMm = 28.0,
                boosterTopSpanMm = 8.0,
                boosterBottomSpanMm = 4.0,
                boosterHeightMm = 9.0,
                boosterShape = ExhaustPortShape.TRAPEZOID,
                durationDeg = 170.0,
            ),
        )!!

        assertEquals(54.0, layout.windows[0].areaMm2, 0.01)
        assertEquals(450.0, layout.windows[1].areaMm2, 0.01)
        assertEquals(558.0, layout.totalAreaMm2, 0.01)
    }

    @Test
    fun calculateExhaust_tripleCompound_asymmetricBoosterShapes() {
        val layout = PortAreaCalculator.calculateExhaustLayout(
            ExhaustPortInput(
                boreMm = 54.0,
                strokeMm = 57.0,
                type = ExhaustPortType.TRIPLE,
                portHeightMm = 20.0,
                totalSpanMm = 0.0,
                bridgeWidthMm = 6.0,
                mainBottomSpanMm = 24.0,
                mainTopSpanMm = 32.0,
                boosterTopSpanMm = 10.0,
                boosterHeightMm = 10.0,
                leftBoosterShape = ExhaustPortShape.TRIANGULAR,
                rightBoosterShape = ExhaustPortShape.TRAPEZOID,
                rightBoosterBottomSpanMm = 4.0,
                durationDeg = 180.0,
            ),
        )!!

        assertEquals(ExhaustPortShape.TRIANGULAR, layout.windows[0].shape)
        assertEquals(ExhaustPortShape.TRAPEZOID, layout.windows[2].shape)
        assertEquals(50.0, layout.windows[0].areaMm2, 0.01)
        assertEquals(70.0, layout.windows[2].areaMm2, 0.01)
        assertEquals(4.0, layout.windows[2].bottomWidthMm, 0.01)
        assertEquals(0.0, layout.windows[0].bottomWidthMm, 0.01)
        assertEquals(true, PortAreaCalculator.layoutHorizontalSpansConsistent(layout))
    }

    @Test
    fun calculateExhaust_twinVShape_apexAtBridge() {
        val layout = PortAreaCalculator.calculateExhaustLayout(
            ExhaustPortInput(
                boreMm = 54.0,
                strokeMm = 57.0,
                type = ExhaustPortType.TWIN,
                shape = ExhaustPortShape.V_SHAPE,
                portHeightMm = 20.0,
                totalSpanMm = 0.0,
                topSpanMm = 48.0,
                bridgeWidthMm = 8.0,
                durationDeg = 166.0,
            ),
        )!!

        assertEquals(ExhaustPortShape.V_SHAPE, layout.windows[0].shape)
        assertEquals(ExhaustPortShape.V_SHAPE, layout.windows[1].shape)
        assertEquals(true, layout.windows[0].vShapeApexAtEnd)
        assertEquals(false, layout.windows[1].vShapeApexAtEnd)
        assertEquals(8.0, layout.totalBottomSpanMm, 0.01)
        assertEquals(200.0, layout.windows[0].areaMm2, 0.01)
        assertEquals(400.0, layout.totalAreaMm2, 0.01)
        assertEquals(true, PortAreaCalculator.layoutHorizontalSpansConsistent(layout))
    }

    @Test
    fun tripleBooster_utCornerRadius_isNotStrippedByCoercion() {
        val layout = PortAreaCalculator.calculateExhaustLayout(
            ExhaustPortInput(
                boreMm = 56.0,
                strokeMm = 50.6,
                type = ExhaustPortType.TRIPLE,
                portHeightMm = 20.0,
                totalSpanMm = 0.0,
                bridgeWidthMm = 6.0,
                mainBottomSpanMm = 24.0,
                mainTopSpanMm = 32.0,
                boosterTopSpanMm = 10.0,
                boosterHeightMm = 10.0,
                boosterCornerRadiusMm = 2.0,
                leftBoosterShape = ExhaustPortShape.TRIANGULAR,
                rightBoosterShape = ExhaustPortShape.TRIANGULAR,
                durationDeg = 180.0,
            ),
        )!!

        val booster = layout.windows.first()
        assertEquals(2.0, booster.radii.utCornerRadiusMm, 0.01)
    }

    @Test
    fun calculateExhaust_triple_rejectsBoosterHeightEqualToMain() {
        assertNull(
            PortAreaCalculator.calculateExhaustLayout(
                ExhaustPortInput(
                    boreMm = 56.0,
                    strokeMm = 50.6,
                    type = ExhaustPortType.TRIPLE,
                    portHeightMm = 20.0,
                    totalSpanMm = 0.0,
                    bridgeWidthMm = 6.0,
                    mainBottomSpanMm = 24.0,
                    mainTopSpanMm = 32.0,
                    boosterTopSpanMm = 10.0,
                    boosterHeightMm = 20.0,
                    boosterShape = ExhaustPortShape.TRIANGULAR,
                    durationDeg = 180.0,
                ),
            ),
        )
    }

    @Test
    fun portWindowArea_triangular_baseAtTop() {
        val area = PortAreaCalculator.portWindowAreaMm2(
            shape = ExhaustPortShape.TRIANGULAR,
            bottomWidthMm = 0.0,
            topWidthMm = 20.0,
            heightMm = 18.0,
        )!!
        assertEquals(180.0, area, 0.01)
    }

    @Test
    fun invalidInput_returnsNull() {
        assertNull(
            PortAreaCalculator.calculateExhaustLayout(
                ExhaustPortInput(
                    boreMm = 54.0,
                    strokeMm = 57.0,
                    type = ExhaustPortType.TRIPLE,
                    portHeightMm = 20.0,
                    totalSpanMm = 0.0,
                    bridgeWidthMm = 6.0,
                    mainBottomSpanMm = 30.0,
                    mainTopSpanMm = 24.0,
                    boosterTopSpanMm = 10.0,
                    boosterHeightMm = 10.0,
                    boosterShape = ExhaustPortShape.TRIANGULAR,
                    durationDeg = 180.0,
                ),
            ),
        )
    }
}
