package com.simplestsoft.twostrokecalc.domain.calculation

import kotlin.math.roundToInt

object CleaningAgentCalculator {

    const val DEFAULT_TOTAL_LITERS = 7.0
    const val DEFAULT_CONCENTRATION_PERCENT = 3.0

    const val MIN_TOTAL_LITERS = 0.5
    const val MAX_TOTAL_LITERS = 20.0
    const val TOTAL_VOLUME_STEP_LITERS = 0.1

    val totalVolumeSliderSteps: Int
        get() = ((MAX_TOTAL_LITERS - MIN_TOTAL_LITERS) / TOTAL_VOLUME_STEP_LITERS).roundToInt()

    const val MIN_CONCENTRATION_PERCENT = 0.5
    const val MAX_CONCENTRATION_PERCENT = 20.0
    const val CONCENTRATION_STEP_PERCENT = 0.1

    val concentrationSliderSteps: Int
        get() = ((MAX_CONCENTRATION_PERCENT - MIN_CONCENTRATION_PERCENT) / CONCENTRATION_STEP_PERCENT)
            .roundToInt()

    data class Input(
        val totalLiters: Double,
        val concentrationPercent: Double,
    )

    fun snapTotalLiters(totalLiters: Double): Double {
        val stepIndex = ((totalLiters - MIN_TOTAL_LITERS) / TOTAL_VOLUME_STEP_LITERS)
            .roundToInt()
        return (MIN_TOTAL_LITERS + stepIndex * TOTAL_VOLUME_STEP_LITERS)
            .coerceIn(MIN_TOTAL_LITERS, MAX_TOTAL_LITERS)
    }

    fun snapConcentrationPercent(concentrationPercent: Double): Double {
        val stepIndex = ((concentrationPercent - MIN_CONCENTRATION_PERCENT) / CONCENTRATION_STEP_PERCENT)
            .roundToInt()
        return (MIN_CONCENTRATION_PERCENT + stepIndex * CONCENTRATION_STEP_PERCENT)
            .coerceIn(MIN_CONCENTRATION_PERCENT, MAX_CONCENTRATION_PERCENT)
    }

    fun calculate(input: Input): CleaningAgentCalculatorResult? {
        if (input.totalLiters <= 0.0 || input.concentrationPercent <= 0.0) return null

        val totalLiters = snapTotalLiters(input.totalLiters)
        val concentrationPercent = snapConcentrationPercent(input.concentrationPercent)

        val cleaningAgentMl = totalLiters * concentrationPercent / 100.0 * 1000.0
        val waterLiters = totalLiters - cleaningAgentMl / 1000.0
        if (waterLiters < 0.0) return null

        return CleaningAgentCalculatorResult(
            totalLiters = totalLiters,
            concentrationPercent = concentrationPercent,
            cleaningAgentMl = cleaningAgentMl,
            waterLiters = waterLiters,
        )
    }
}
