package com.simplestsoft.twostrokecalc.domain.calculation

/** Top-level result types for Swift interop (nested `Result` classes are not exported). */

data class DcCableCrossSectionResult(
    val currentAmps: Double,
    val minimumCrossSectionMm2: Double,
    val recommendedCrossSectionMm2: Double,
    val voltageDropVolts: Double,
    val voltageDropPercent: Double,
    val maxAllowedDropVolts: Double,
    val maxAllowedDropPercent: Double,
)

data class ElectrolyteCalculatorResult(
    val aceticAcidMl: Double,
    val waterMl: Double,
    val waterLiters: Double,
    val saltG: Double,
    val saltGPerL: Double,
    val saltGPerWaterL: Double,
    val totalLiquidMl: Double,
    val totalLiquidLiters: Double,
    val scaleFactor: Double,
    val acidConcentrationPercent: Double,
    val aceticAcidVolumePercent: Double,
    val waterVolumePercent: Double,
    val finalAceticAcidPercent: Double,
    val zincDissolution: ElectrolyteZincDissolutionResult?,
)

data class ElectrolyteZincDissolutionResult(
    val targetDissolvedZincG: Double,
    val targetConcentrationGPerL: Double,
    val currentAmps: Double,
    val voltageVolts: Double,
    val powerWatts: Double,
    val dissolutionRateGPerHour: Double,
    val electrificationHours: Double,
)

data class CleaningAgentCalculatorResult(
    val totalLiters: Double,
    val concentrationPercent: Double,
    val cleaningAgentMl: Double,
    val waterLiters: Double,
)

data class VariatorWeightCalculatorResult(
    val physicsWeightGrams: Double,
    val empiricalWeightGrams: Double,
    val targetRpm: Double,
    val rpmDelta: Double,
)
