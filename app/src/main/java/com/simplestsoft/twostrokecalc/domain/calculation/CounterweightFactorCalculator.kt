package com.simplestsoft.twostrokecalc.domain.calculation

object CounterweightFactorCalculator {
    /**
     * Calculates the counterweight factor (Wuchtfaktor) in percent:
     * (bigEndWeight + connectingRodHalf) / (pistonWeight + connectingRodHalf) × 100
     */
    fun factorPercent(
        pistonWeightGrams: Double,
        connectingRodHalfGrams: Double,
        bigEndWeightGrams: Double,
    ): Double? {
        if (pistonWeightGrams <= 0.0 || connectingRodHalfGrams < 0.0 || bigEndWeightGrams < 0.0) {
            return null
        }
        val denominator = pistonWeightGrams + connectingRodHalfGrams
        if (denominator <= 0.0) return null
        return (bigEndWeightGrams + connectingRodHalfGrams) / denominator * 100.0
    }
}
