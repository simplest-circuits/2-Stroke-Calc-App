package com.simplestsoft.twostrokecalc.domain.calculation

import kotlin.math.roundToInt

object ElectrolyteCalculator {

    /** Reference batch: 500 ml 60 % acetic acid + 2.5 l water + 122 g salt (≈10 % acetic acid in mix). */
    const val BASE_ACETIC_ACID_ML = 500.0
    const val BASE_WATER_ML = 2500.0
    const val BASE_SALT_G = 122.0
    const val BASE_ACID_CONCENTRATION_PERCENT = 60.0
    const val BASE_TOTAL_LIQUID_ML = BASE_ACETIC_ACID_ML + BASE_WATER_ML
    const val BASE_TOTAL_LIQUID_LITERS = BASE_TOTAL_LIQUID_ML / 1000.0
    const val BASE_WATER_LITERS = BASE_WATER_ML / 1000.0
    /** Practical reference: 3 l batch, 1.6 A constant current → 4.3 V measured at the cell. */
    const val BASE_ELECTRIFICATION_CURRENT_AMPS = 1.6
    const val BASE_ELECTRIFICATION_VOLTAGE_VOLTS = 4.3

    /** Decomposition / contact potential (zinc dissolution in acetic acid). */
    const val BASE_CELL_VOLTAGE_VOLTS = 3.2

    private val BASE_OHMIC_RESISTANCE_OHMS: Double =
        (BASE_ELECTRIFICATION_VOLTAGE_VOLTS - BASE_CELL_VOLTAGE_VOLTS) / BASE_ELECTRIFICATION_CURRENT_AMPS

    const val MIN_ELECTRIFICATION_VOLTAGE_VOLTS = 2.5
    const val MAX_ELECTRIFICATION_VOLTAGE_VOLTS = 12.0

    /** Reference salt content per liter of finished liquid (122 g / 3 l). */
    const val BASE_SALT_G_PER_L = BASE_SALT_G / BASE_TOTAL_LIQUID_LITERS

    /** Reference salt content per liter of water (122 g / 2.5 l). */
    const val BASE_SALT_G_PER_WATER_L = BASE_SALT_G / BASE_WATER_LITERS

    /** Pure acetic acid share in the reference mix (500 ml × 60 % / 3 l ≈ 10 %). */
    const val BASE_FINAL_ACETIC_ACID_PERCENT =
        BASE_ACETIC_ACID_ML * BASE_ACID_CONCENTRATION_PERCENT / BASE_TOTAL_LIQUID_ML

    /** Minimum dissolved zinc for a usable zinc-acetate electrolyte (galvanic reference). */
    const val TARGET_DISSOLVED_ZINC_G_PER_L = 25.0

    /** Typical anodic zinc dissolution efficiency in weak acetic acid. */
    const val ZINC_DISSOLUTION_EFFICIENCY = 0.90

    const val MIN_TOTAL_LITERS = 0.5
    const val MAX_TOTAL_LITERS = 14.0
    const val TOTAL_VOLUME_STEP_LITERS = 0.1

    val totalVolumeSliderSteps: Int
        get() = ((MAX_TOTAL_LITERS - MIN_TOTAL_LITERS) / TOTAL_VOLUME_STEP_LITERS).roundToInt()

    const val SALT_STEP_G = 1.0
    val MIN_SALT_G: Double = BASE_SALT_G * MIN_TOTAL_LITERS / BASE_TOTAL_LIQUID_LITERS
    val MAX_SALT_G: Double = BASE_SALT_G * MAX_TOTAL_LITERS / BASE_TOTAL_LIQUID_LITERS

    val saltSliderSteps: Int
        get() = ((MAX_SALT_G - MIN_SALT_G) / SALT_STEP_G).roundToInt()

    const val MIN_ACID_CONCENTRATION_PERCENT = 30.0
    const val MAX_ACID_CONCENTRATION_PERCENT = 80.0
    const val MIN_ELECTRIFICATION_CURRENT_AMPS = 0.5
    const val MAX_ELECTRIFICATION_CURRENT_AMPS = 5.0

    private const val ZINC_MOLAR_MASS_G_PER_MOL = 65.38
    private const val ZINC_ELECTRONS_PER_ATOM = 2
    private const val FARADAY_CONSTANT_C_PER_MOL = 96485.0

    enum class SliderDriver {
        SALT,
        TOTAL,
        ACID_CONCENTRATION,
    }

    data class SliderState(
        val totalLiters: Double,
        val waterLiters: Double,
        val aceticAcidMl: Double,
        val saltG: Double,
        val acidConcentrationPercent: Double,
        val electrificationCurrentAmps: Double,
    )

    data class Result(
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
        val zincDissolution: ZincDissolutionResult?,
    )

    data class ZincDissolutionResult(
        val targetDissolvedZincG: Double,
        val targetConcentrationGPerL: Double,
        val currentAmps: Double,
        val voltageVolts: Double,
        val powerWatts: Double,
        val dissolutionRateGPerHour: Double,
        val electrificationHours: Double,
    )

    fun referenceSliderState(): SliderState = SliderState(
        totalLiters = BASE_TOTAL_LIQUID_LITERS,
        waterLiters = BASE_WATER_LITERS,
        aceticAcidMl = BASE_ACETIC_ACID_ML,
        saltG = BASE_SALT_G,
        acidConcentrationPercent = BASE_ACID_CONCENTRATION_PERCENT,
        electrificationCurrentAmps = BASE_ELECTRIFICATION_CURRENT_AMPS,
    )

    fun snapTotalLiters(totalLiters: Double): Double {
        val stepIndex = ((totalLiters - MIN_TOTAL_LITERS) / TOTAL_VOLUME_STEP_LITERS)
            .roundToInt()
        return (MIN_TOTAL_LITERS + stepIndex * TOTAL_VOLUME_STEP_LITERS)
            .coerceIn(MIN_TOTAL_LITERS, MAX_TOTAL_LITERS)
    }

    fun snapSaltG(saltG: Double): Double {
        val minG = MIN_SALT_G.roundToInt().toDouble()
        val maxG = MAX_SALT_G.roundToInt().toDouble()
        val stepIndex = ((saltG - minG) / SALT_STEP_G).roundToInt()
        return (minG + stepIndex * SALT_STEP_G).coerceIn(minG, maxG)
    }

    fun saltForTotalLiters(totalLiters: Double): Double =
        BASE_SALT_G * totalLiters / BASE_TOTAL_LIQUID_LITERS

    fun totalLitersForSalt(saltG: Double): Double =
        saltG * BASE_TOTAL_LIQUID_LITERS / BASE_SALT_G

    fun syncSliders(
        driver: SliderDriver,
        totalLiters: Double,
        waterLiters: Double,
        aceticAcidMl: Double,
        saltG: Double,
        acidConcentrationPercent: Double,
        electrificationCurrentAmps: Double,
    ): SliderState? {
        val scaled = when (driver) {
            SliderDriver.SALT -> SliderState(
                totalLiters = totalLiters,
                waterLiters = waterLiters,
                aceticAcidMl = aceticAcidMl,
                saltG = saltG,
                acidConcentrationPercent = acidConcentrationPercent,
                electrificationCurrentAmps = electrificationCurrentAmps,
            )
            SliderDriver.TOTAL -> {
                val snappedTotal = snapTotalLiters(totalLiters)
                if (snappedTotal <= 0.0) return null
                val scale = snappedTotal / BASE_TOTAL_LIQUID_LITERS
                SliderState(
                    totalLiters = snappedTotal,
                    waterLiters = BASE_WATER_LITERS * scale,
                    aceticAcidMl = BASE_ACETIC_ACID_ML * scale,
                    saltG = saltG,
                    acidConcentrationPercent = acidConcentrationPercent,
                    electrificationCurrentAmps = electrificationCurrentAmps,
                )
            }
            SliderDriver.ACID_CONCENTRATION -> SliderState(
                totalLiters = totalLiters,
                waterLiters = waterLiters,
                aceticAcidMl = aceticAcidMl,
                saltG = saltG,
                acidConcentrationPercent = acidConcentrationPercent,
                electrificationCurrentAmps = electrificationCurrentAmps,
            )
        }

        val withAcidSplit = applyAcidConcentration(
            state = scaled,
            acidConcentrationPercent = acidConcentrationPercent,
            keepTotal = true,
        ) ?: return null

        return withAcidSplit.copy(
            saltG = snapSaltG(withAcidSplit.saltG),
            acidConcentrationPercent = acidConcentrationPercent.coerceIn(
                MIN_ACID_CONCENTRATION_PERCENT,
                MAX_ACID_CONCENTRATION_PERCENT,
            ),
            electrificationCurrentAmps = electrificationCurrentAmps.coerceIn(
                MIN_ELECTRIFICATION_CURRENT_AMPS,
                MAX_ELECTRIFICATION_CURRENT_AMPS,
            ),
        )
    }

    fun scaleFromTotal(
        totalLiters: Double,
        electrificationCurrentAmps: Double,
        saltG: Double = saltForTotalLiters(totalLiters),
    ): SliderState? {
        if (totalLiters <= 0.0) return null
        val scale = totalLiters / BASE_TOTAL_LIQUID_LITERS
        return SliderState(
            totalLiters = totalLiters,
            waterLiters = BASE_WATER_LITERS * scale,
            aceticAcidMl = BASE_ACETIC_ACID_ML * scale,
            saltG = saltG,
            acidConcentrationPercent = BASE_ACID_CONCENTRATION_PERCENT,
            electrificationCurrentAmps = electrificationCurrentAmps,
        )
    }

    fun scaleFromSalt(
        saltG: Double,
        electrificationCurrentAmps: Double,
    ): SliderState? {
        val snappedSalt = snapSaltG(saltG)
        return scaleFromTotal(
            totalLiters = snapTotalLiters(totalLitersForSalt(snappedSalt)),
            electrificationCurrentAmps = electrificationCurrentAmps,
            saltG = snappedSalt,
        )
    }

    fun applyAcidConcentration(
        state: SliderState,
        acidConcentrationPercent: Double,
        keepTotal: Boolean,
    ): SliderState? {
        if (acidConcentrationPercent <= 0.0) return null

        val totalMl = state.totalLiters * 1000.0
        val scale = state.totalLiters / BASE_TOTAL_LIQUID_LITERS
        val pureAceticAcidMl =
            BASE_ACETIC_ACID_ML * BASE_ACID_CONCENTRATION_PERCENT / 100.0 * scale
        val aceticAcidMl = pureAceticAcidMl / (acidConcentrationPercent / 100.0)
        if (aceticAcidMl > totalMl) return null

        val waterMl = totalMl - aceticAcidMl
        return state.copy(
            totalLiters = if (keepTotal) state.totalLiters else (waterMl + aceticAcidMl) / 1000.0,
            waterLiters = waterMl / 1000.0,
            aceticAcidMl = aceticAcidMl,
        )
    }

    /**
     * Estimated PSU voltage for galvanizing: cell potential + ohmic drop in the bath.
     * Calibrated to 4.3 V @ 1.6 A @ 3 l (practical measurement).
     */
    fun voltageForLoad(currentAmps: Double, totalLiters: Double): Double? {
        if (currentAmps <= 0.0 || totalLiters <= 0.0) return null
        val volumeFactor = kotlin.math.sqrt(BASE_TOTAL_LIQUID_LITERS / totalLiters)
        val ohmicDrop = BASE_OHMIC_RESISTANCE_OHMS * currentAmps * volumeFactor
        val raw = BASE_CELL_VOLTAGE_VOLTS + ohmicDrop
        return raw.coerceIn(MIN_ELECTRIFICATION_VOLTAGE_VOLTS, MAX_ELECTRIFICATION_VOLTAGE_VOLTS)
    }

    fun calculate(state: SliderState): Result? {
        if (
            state.totalLiters <= 0.0 ||
            state.saltG <= 0.0 ||
            state.acidConcentrationPercent <= 0.0 ||
            state.electrificationCurrentAmps <= 0.0
        ) {
            return null
        }

        val targetTotalMl = state.totalLiters * 1000.0
        val waterMl = state.waterLiters * 1000.0
        val aceticAcidMl = state.aceticAcidMl
        if (aceticAcidMl > targetTotalMl) return null

        val scale = state.totalLiters / BASE_TOTAL_LIQUID_LITERS
        val pureAceticAcidMl =
            BASE_ACETIC_ACID_ML * BASE_ACID_CONCENTRATION_PERCENT / 100.0 * scale
        val saltG = state.saltG
        val saltGPerL = saltG / state.totalLiters
        val saltGPerWaterL = if (waterMl > 0.0) saltG / (waterMl / 1000.0) else return null
        val finalAceticAcidPercent = pureAceticAcidMl / targetTotalMl * 100.0

        val zincDissolution = calculateZincDissolution(
            totalLiquidLiters = state.totalLiters,
            currentAmps = state.electrificationCurrentAmps,
        )

        return Result(
            aceticAcidMl = aceticAcidMl,
            waterMl = waterMl,
            waterLiters = state.waterLiters,
            saltG = saltG,
            saltGPerL = saltGPerL,
            saltGPerWaterL = saltGPerWaterL,
            totalLiquidMl = targetTotalMl,
            totalLiquidLiters = state.totalLiters,
            scaleFactor = scale,
            acidConcentrationPercent = state.acidConcentrationPercent,
            aceticAcidVolumePercent = aceticAcidMl / targetTotalMl * 100.0,
            waterVolumePercent = waterMl / targetTotalMl * 100.0,
            finalAceticAcidPercent = finalAceticAcidPercent,
            zincDissolution = zincDissolution,
        )
    }

    fun dissolvedZincGramsPerAmpereHour(
        efficiency: Double = ZINC_DISSOLUTION_EFFICIENCY,
    ): Double =
        efficiency * ZINC_MOLAR_MASS_G_PER_MOL * 3600.0 /
            (ZINC_ELECTRONS_PER_ATOM * FARADAY_CONSTANT_C_PER_MOL)

    fun calculateZincDissolution(
        totalLiquidLiters: Double,
        currentAmps: Double,
        targetDissolvedZincGPerL: Double = TARGET_DISSOLVED_ZINC_G_PER_L,
    ): ZincDissolutionResult? {
        if (totalLiquidLiters <= 0.0 || currentAmps <= 0.0 || targetDissolvedZincGPerL <= 0.0) {
            return null
        }

        val targetDissolvedZincG = totalLiquidLiters * targetDissolvedZincGPerL
        val dissolutionRateGPerHour = dissolvedZincGramsPerAmpereHour() * currentAmps
        val electrificationHours = targetDissolvedZincG / dissolutionRateGPerHour
        val voltageVolts = voltageForLoad(currentAmps, totalLiquidLiters) ?: return null
        val powerWatts = voltageVolts * currentAmps

        return ZincDissolutionResult(
            targetDissolvedZincG = targetDissolvedZincG,
            targetConcentrationGPerL = targetDissolvedZincGPerL,
            currentAmps = currentAmps,
            voltageVolts = voltageVolts,
            powerWatts = powerWatts,
            dissolutionRateGPerHour = dissolutionRateGPerHour,
            electrificationHours = electrificationHours,
        )
    }
}
