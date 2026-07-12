package com.simplestsoft.twostrokecalc.domain.calculation

import kotlin.math.pow
import kotlin.math.roundToInt

data class CarbJetCorrectionResult(
    val correctedMainJet: Int,
    val correctedMainJetExact: Double,
    val correctionFactor: Double,
    val referenceAirDensity: Double,
    val targetAirDensity: Double,
    val referencePressureMbar: Double,
    val targetPressureMbar: Double,
)

object CarbJetCorrectionCalculator {
    /** Sea-level pressure in the ICAO standard atmosphere (mbar). */
    private const val SEA_LEVEL_PRESSURE_MBAR = 1013.25

    /** Temperature coefficient for volumetric air mass (Bonavolta / ideal gas). */
    private const val TEMP_COEFF = 0.00367

    private const val MIN_TEMPERATURE_C = -50.0
    private const val MAX_ALTITUDE_M = 12_000.0
    private const val MIN_ALTITUDE_M = -500.0

    /**
     * Volumetric air mass proportional to p / (1 + 0.00367 × t°C).
     * See Bonavolta (2-stroke jetting) and Bell, "Two-Stroke Performance Tuning".
     */
    fun airDensityFactor(pressureMbar: Double, temperatureC: Double): Double? {
        if (pressureMbar <= 0.0 || temperatureC < MIN_TEMPERATURE_C) return null
        return pressureMbar / (1.0 + TEMP_COEFF * temperatureC)
    }

    /**
     * Barometric pressure from altitude using the ICAO standard atmosphere (h in meters).
     */
    fun pressureAtAltitudeMeters(altitudeM: Double): Double? {
        if (altitudeM < MIN_ALTITUDE_M || altitudeM > MAX_ALTITUDE_M) return null
        val ratio = (1.0 - 0.0000225577 * altitudeM).coerceAtLeast(0.01)
        return SEA_LEVEL_PRESSURE_MBAR * ratio.pow(5.25588)
    }

    /**
     * Corrects a main jet size for altitude and temperature changes.
     *
     * The jet diameter scales with the fourth root of the air-density ratio
     * (Poiseuille / orifice flow, as used in 2-stroke tuning literature):
     *
     *   D₂ = D₁ × ⁴√(ρ₂ / ρ₁)
     *
     * where ρ ∝ p / (1 + 0.00367 × t°C).
     */
    fun calculate(
        baseMainJet: Int,
        referenceAltitudeM: Double,
        referenceTemperatureC: Double,
        targetAltitudeM: Double,
        targetTemperatureC: Double,
    ): CarbJetCorrectionResult? {
        if (baseMainJet <= 0) return null

        val referencePressure = pressureAtAltitudeMeters(referenceAltitudeM) ?: return null
        val targetPressure = pressureAtAltitudeMeters(targetAltitudeM) ?: return null
        val referenceDensity = airDensityFactor(referencePressure, referenceTemperatureC) ?: return null
        val targetDensity = airDensityFactor(targetPressure, targetTemperatureC) ?: return null
        if (referenceDensity <= 0.0 || targetDensity <= 0.0) return null

        val densityRatio = targetDensity / referenceDensity
        val correctionFactor = densityRatio.pow(0.25)
        val correctedExact = baseMainJet * correctionFactor
        val correctedRounded = correctedExact
            .coerceAtLeast(1.0)
            .roundToInt()

        return CarbJetCorrectionResult(
            correctedMainJet = correctedRounded,
            correctedMainJetExact = correctedExact,
            correctionFactor = correctionFactor,
            referenceAirDensity = referenceDensity,
            targetAirDensity = targetDensity,
            referencePressureMbar = referencePressure,
            targetPressureMbar = targetPressure,
        )
    }
}
