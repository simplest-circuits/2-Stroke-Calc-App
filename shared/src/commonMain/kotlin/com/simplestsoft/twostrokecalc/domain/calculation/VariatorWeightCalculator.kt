package com.simplestsoft.twostrokecalc.domain.calculation

import kotlin.math.round

enum VariatorWeightRollerType(val rpmPerGram: Double) {
    ROLLERS(550.0),
    SLIDERS(275.0),
}

object VariatorWeightCalculator {

    typealias RollerType = VariatorWeightRollerType

    data class Result(
        val physicsWeightGrams: Double,
        val empiricalWeightGrams: Double,
        val targetRpm: Double,
        val rpmDelta: Double,
    )

    /**
     * Physics mode: at the shift point F_c = m·ω²·r is constant, so m₁·RPM₁² = m₂·RPM₂².
     */
    fun weightForTargetRpmPhysics(
        currentGrams: Double,
        currentRpm: Double,
        targetRpm: Double,
    ): Double? {
        if (currentGrams <= 0.0 || currentRpm <= 0.0 || targetRpm <= 0.0) return null
        val ratio = currentRpm / targetRpm
        return currentGrams * ratio * ratio
    }

    /**
     * Empirical mode: approximately linear RPM change per gram (community rule of thumb).
     * Positive [rpmDelta] means higher target RPM → lighter rollers.
     */
    fun weightForRpmDeltaEmpirical(
        currentGrams: Double,
        rpmDelta: Double,
        rollerType: RollerType,
    ): Double? {
        if (currentGrams <= 0.0) return null
        val weightDelta = -rpmDelta / rollerType.rpmPerGram
        val newWeight = currentGrams + weightDelta
        return if (newWeight > 0.0) newWeight else null
    }

    fun targetRpmFromDelta(currentRpm: Double, rpmDelta: Double): Double? {
        if (currentRpm <= 0.0) return null
        val target = currentRpm + rpmDelta
        return if (target > 0.0) target else null
    }

    fun rpmDeltaFromTarget(currentRpm: Double, targetRpm: Double): Double? {
        if (currentRpm <= 0.0 || targetRpm <= 0.0) return null
        return targetRpm - currentRpm
    }

    fun rpmDeltaForWeightChangeEmpirical(
        weightDeltaGrams: Double,
        rollerType: RollerType,
    ): Double = -weightDeltaGrams * rollerType.rpmPerGram

    fun calculate(
        currentGrams: Double,
        currentRpm: Double,
        targetRpm: Double,
        rollerType: RollerType,
    ): Result? {
        val physicsWeight = weightForTargetRpmPhysics(currentGrams, currentRpm, targetRpm) ?: return null
        val rpmDelta = rpmDeltaFromTarget(currentRpm, targetRpm) ?: return null
        val empiricalWeight = weightForRpmDeltaEmpirical(currentGrams, rpmDelta, rollerType) ?: return null
        return Result(
            physicsWeightGrams = physicsWeight,
            empiricalWeightGrams = empiricalWeight,
            targetRpm = targetRpm,
            rpmDelta = rpmDelta,
        )
    }

    fun roundToTenth(grams: Double): Double = round(grams * 10.0) / 10.0
}
