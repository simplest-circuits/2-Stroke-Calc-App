package com.simplestsoft.twostrokecalc.domain.calculation

data class MeanPressureResult(
    val powerWatts: Double,
    val rpmPerSecond: Double,
    val torqueNm: Double,
    val displacementCcm: Double,
    val displacementDm3: Double,
    val meanPressureBar: Double,
    val specificPowerPsPerLiter: Double,
)

object MeanPressureCalculator {
    private const val PI = 3.1415926
    private const val PS_TO_KW_FACTOR = 1.36

    /**
     * Calculates mean effective pressure and related values from the Excel sheet "Mitteldruck":
     * - Power [W] = (powerPs / 1.36) × 1000
     * - RPM [1/s] = rpm / 60
     * - Torque [Nm] = powerW / (2 × π × rpmPerSecond)
     * - Displacement [ccm] = (bore² × π/4 × stroke) / 1000
     * - Displacement [dm³] = displacementCcm / 1000
     * - Mean pressure [bar] = (2 × π × torque) / displacementDm3 / 100
     * - Specific power [PS/L] = powerPs / displacementCcm × 1000
     */
    fun calculate(
        powerPs: Double,
        rpm: Double,
        boreMm: Double,
        strokeMm: Double,
    ): MeanPressureResult? {
        if (powerPs <= 0.0 || rpm <= 0.0 || boreMm <= 0.0 || strokeMm <= 0.0) {
            return null
        }

        val powerWatts = powerPs / PS_TO_KW_FACTOR * 1000.0
        val rpmPerSecond = rpm / 60.0
        val torqueNm = powerWatts / (2.0 * PI * rpmPerSecond)
        val displacementCcm = boreMm * boreMm * PI / 4.0 * strokeMm / 1000.0
        if (displacementCcm <= 0.0) return null

        val displacementDm3 = displacementCcm / 1000.0
        val meanPressureBar = 2.0 * PI * torqueNm / displacementDm3 / 100.0
        val specificPowerPsPerLiter = powerPs / displacementCcm * 1000.0

        return MeanPressureResult(
            powerWatts = powerWatts,
            rpmPerSecond = rpmPerSecond,
            torqueNm = torqueNm,
            displacementCcm = displacementCcm,
            displacementDm3 = displacementDm3,
            meanPressureBar = meanPressureBar,
            specificPowerPsPerLiter = specificPowerPsPerLiter,
        )
    }
}
