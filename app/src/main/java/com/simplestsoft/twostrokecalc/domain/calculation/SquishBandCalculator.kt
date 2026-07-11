package com.simplestsoft.twostrokecalc.domain.calculation

import kotlin.math.sqrt

enum class SquishBandCalculationMode {
    PERCENT,
    WIDTH,
}

data class SquishBandResult(
    val squishAreaPercent: Double,
    val squishBandWidthMm: Double,
    val domeDiameterMm: Double,
)

object SquishBandCalculator {
    private const val PI = 3.1415

    /**
     * Calculates dome diameter and squish band width for a target squish area:
     * - Dome diameter = bore × √(1 − squishAreaPercent / 100)
     * - Squish band width = (bore − domeDiameter) / 2
     */
    fun calculateFromPercent(
        boreMm: Double,
        squishAreaPercent: Double,
    ): SquishBandResult? {
        if (boreMm <= 0.0 || squishAreaPercent <= 0.0 || squishAreaPercent >= 100.0) {
            return null
        }

        val domeDiameterMm = boreMm * sqrt(1.0 - squishAreaPercent / 100.0)
        val squishBandWidthMm = (boreMm - domeDiameterMm) / 2.0

        return SquishBandResult(
            squishAreaPercent = squishAreaPercent,
            squishBandWidthMm = squishBandWidthMm,
            domeDiameterMm = domeDiameterMm,
        )
    }

    /**
     * Calculates squish area from bore and squish band width:
     * - Dome diameter = bore − 2 × squishBandWidth
     * - Squish area from bore and derived dome diameter
     */
    fun calculateFromWidth(
        boreMm: Double,
        squishBandWidthMm: Double,
    ): SquishBandResult? {
        if (boreMm <= 0.0 || squishBandWidthMm <= 0.0 || squishBandWidthMm >= boreMm / 2.0) {
            return null
        }

        val domeDiameterMm = boreMm - 2.0 * squishBandWidthMm
        val boreArea = boreMm * boreMm * PI / 4.0
        val squishAreaPercent =
            (boreArea - domeDiameterMm * domeDiameterMm * PI / 4.0) / boreArea * 100.0

        return SquishBandResult(
            squishAreaPercent = squishAreaPercent,
            squishBandWidthMm = squishBandWidthMm,
            domeDiameterMm = domeDiameterMm,
        )
    }
}
