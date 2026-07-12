package com.simplestsoft.twostrokecalc.domain.calculation

object DcCableCrossSectionCalculator {

    /** Copper resistivity at ~20 °C in Ω·mm²/m. */
    const val COPPER_RESISTIVITY_OHM_MM2_PER_M = 0.0175

    val STANDARD_CROSS_SECTIONS_MM2: List<Double> = listOf(
        0.35,
        0.5,
        0.75,
        1.0,
        1.5,
        2.5,
        4.0,
        6.0,
        10.0,
        16.0,
        25.0,
        35.0,
        50.0,
    )

    data class Result(
        val currentAmps: Double,
        val minimumCrossSectionMm2: Double,
        val recommendedCrossSectionMm2: Double,
        val voltageDropVolts: Double,
        val voltageDropPercent: Double,
        val maxAllowedDropVolts: Double,
        val maxAllowedDropPercent: Double,
    )

    fun currentFromPower(watts: Double, voltage: Double): Double? {
        if (watts <= 0.0 || voltage <= 0.0) return null
        return watts / voltage
    }

    fun minimumCrossSectionMm2(
        currentAmps: Double,
        oneWayLengthM: Double,
        maxDropVolts: Double,
    ): Double? {
        if (currentAmps <= 0.0 || oneWayLengthM <= 0.0 || maxDropVolts <= 0.0) return null
        return 2.0 * COPPER_RESISTIVITY_OHM_MM2_PER_M * oneWayLengthM * currentAmps / maxDropVolts
    }

    fun voltageDrop(
        currentAmps: Double,
        oneWayLengthM: Double,
        crossSectionMm2: Double,
    ): Double? {
        if (currentAmps <= 0.0 || oneWayLengthM <= 0.0 || crossSectionMm2 <= 0.0) return null
        return 2.0 * COPPER_RESISTIVITY_OHM_MM2_PER_M * oneWayLengthM * currentAmps / crossSectionMm2
    }

    fun recommendStandardSize(minimumMm2: Double): Double? {
        if (minimumMm2 <= 0.0) return null
        return STANDARD_CROSS_SECTIONS_MM2.firstOrNull { it >= minimumMm2 - 1e-9 }
            ?: STANDARD_CROSS_SECTIONS_MM2.last()
    }

    fun calculate(
        voltage: Double,
        currentAmps: Double,
        oneWayLengthM: Double,
        maxDropPercent: Double,
    ): Result? {
        if (voltage <= 0.0 || currentAmps <= 0.0 || oneWayLengthM <= 0.0 || maxDropPercent <= 0.0) return null
        val maxDropVolts = voltage * maxDropPercent / 100.0
        val minArea = minimumCrossSectionMm2(currentAmps, oneWayLengthM, maxDropVolts) ?: return null
        val recommended = recommendStandardSize(minArea) ?: return null
        val actualDrop = voltageDrop(currentAmps, oneWayLengthM, recommended) ?: return null
        return Result(
            currentAmps = currentAmps,
            minimumCrossSectionMm2 = minArea,
            recommendedCrossSectionMm2 = recommended,
            voltageDropVolts = actualDrop,
            voltageDropPercent = actualDrop / voltage * 100.0,
            maxAllowedDropVolts = maxDropVolts,
            maxAllowedDropPercent = maxDropPercent,
        )
    }
}
