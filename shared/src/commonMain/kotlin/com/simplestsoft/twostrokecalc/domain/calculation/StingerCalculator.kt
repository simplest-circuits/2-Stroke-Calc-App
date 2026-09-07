package com.simplestsoft.twostrokecalc.domain.calculation

import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sqrt

enum class StingerFlowRegime {
    SUBSONIC,
    CHOKED,
}

data class StingerInput(
    val displacementCc: Double,
    val rpm: Double,
    val exhaustGasTempCelsius: Double,
    val gaugePressureBar: Double,
    val ambientPressureBarAbs: Double,
    val specificHeatRatio: Double = StingerCalculator.DEFAULT_GAMMA,
    val gasConstant: Double = StingerCalculator.DEFAULT_GAS_CONSTANT,
    val ambientTempCelsius: Double = StingerCalculator.DEFAULT_AMBIENT_TEMP_CELSIUS,
)

data class StingerResult(
    val massFlowKgPerS: Double,
    val speedOfSoundMs: Double,
    val stagnationPressureBarAbs: Double,
    val pressureRatio: Double,
    val criticalPressureRatio: Double,
    val machNumber: Double,
    val exitVelocityMs: Double,
    val massFluxKgPerM2S: Double,
    val areaM2: Double,
    val areaMm2: Double,
    val innerDiameterMm: Double,
    val flowRegime: StingerFlowRegime,
)

/**
 * Theoretical stinger cross-section from compressible isentropic nozzle flow.
 *
 * Mass flow is the 2-stroke intake charge: one swept volume per revolution at ambient
 * density. Exhaust temperature must not be used for that density — it would treat the
 * cold displacement as already-hot gas and understate ṁ by roughly T_exh / T_amb.
 *
 * `ṁ = ρ_amb × V_h × n / 60` with `ρ_amb = p_amb / (R × T_amb)`.
 *
 * Exhaust temperature and mean back pressure set the nozzle state (speed of sound,
 * Mach, mass flux). Gauge pressure is the **mean** pipe-to-ambient drop, typically
 * 0.03–0.10 bar — not the reflected-wave peak.
 *
 * Resulting diameter is an ideal round inner diameter without friction, pulsation
 * or manufacturing allowance. Stinger length is intentionally not computed.
 */
object StingerCalculator {
    const val DEFAULT_GAMMA = 1.33
    const val DEFAULT_AMBIENT_PRESSURE_BAR_ABS = 1.01325
    const val DEFAULT_AMBIENT_TEMP_CELSIUS = 20.0
    const val DEFAULT_GAUGE_PRESSURE_BAR = 0.05
    const val DEFAULT_GAS_CONSTANT = 287.0

    private const val CC_TO_M3 = 1.0e-6
    private const val BAR_TO_PA = 100_000.0
    private const val M2_TO_MM2 = 1_000_000.0
    private const val M_TO_MM = 1_000.0

    /**
     * Mean exhaust mass flow for a 2-stroke engine (delivery ratio 1.0):
     * `ṁ = ρ_amb × V_h × n / 60`.
     */
    fun massFlowKgPerS(
        displacementCc: Double,
        rpm: Double,
        ambientPressureBarAbs: Double,
        ambientTempCelsius: Double = DEFAULT_AMBIENT_TEMP_CELSIUS,
        gasConstant: Double = DEFAULT_GAS_CONSTANT,
    ): Double? {
        if (
            displacementCc <= 0.0 ||
            rpm <= 0.0 ||
            ambientTempCelsius <= -273.0 ||
            ambientPressureBarAbs <= 0.0 ||
            gasConstant <= 0.0
        ) {
            return null
        }

        val tempAmbientK = ambientTempCelsius + 273.15
        if (tempAmbientK <= 0.0) return null

        val volumeFlowM3PerS = displacementCc * CC_TO_M3 * rpm / 60.0
        val ambientPressurePa = ambientPressureBarAbs * BAR_TO_PA
        val densityKgPerM3 = ambientPressurePa / (gasConstant * tempAmbientK)
        val massFlow = volumeFlowM3PerS * densityKgPerM3

        return massFlow.takeIf { it > 0.0 }
    }

    fun speedOfSoundMs(
        exhaustGasTempCelsius: Double,
        specificHeatRatio: Double = DEFAULT_GAMMA,
        gasConstant: Double = DEFAULT_GAS_CONSTANT,
    ): Double? {
        if (
            exhaustGasTempCelsius <= -273.0 ||
            specificHeatRatio <= 0.0 ||
            gasConstant <= 0.0
        ) {
            return null
        }
        return sqrt(specificHeatRatio * gasConstant * (exhaustGasTempCelsius + 273.15))
    }

    fun calculate(input: StingerInput): StingerResult? {
        val gamma = input.specificHeatRatio
        if (
            input.displacementCc <= 0.0 ||
            input.rpm <= 0.0 ||
            input.exhaustGasTempCelsius <= -273.0 ||
            input.ambientTempCelsius <= -273.0 ||
            input.gaugePressureBar <= 0.0 ||
            input.ambientPressureBarAbs <= 0.0 ||
            gamma <= 1.0 ||
            input.gasConstant <= 0.0
        ) {
            return null
        }

        val p0Bar = input.ambientPressureBarAbs + input.gaugePressureBar
        if (p0Bar <= input.ambientPressureBarAbs) {
            return null
        }

        val massFlow = massFlowKgPerS(
            displacementCc = input.displacementCc,
            rpm = input.rpm,
            ambientPressureBarAbs = input.ambientPressureBarAbs,
            ambientTempCelsius = input.ambientTempCelsius,
            gasConstant = input.gasConstant,
        ) ?: return null

        val speedOfSound = speedOfSoundMs(
            exhaustGasTempCelsius = input.exhaustGasTempCelsius,
            specificHeatRatio = gamma,
            gasConstant = input.gasConstant,
        ) ?: return null

        val pressureRatio = input.ambientPressureBarAbs / p0Bar
        val criticalPressureRatio = (2.0 / (gamma + 1.0)).pow(gamma / (gamma - 1.0))
        val choked = pressureRatio <= criticalPressureRatio

        val mach = if (choked) {
            1.0
        } else {
            machFromPressureRatio(pressureRatio, gamma) ?: return null
        }

        val p0Pa = p0Bar * BAR_TO_PA
        val massFlux = massFluxKgPerM2S(
            stagnationPressurePa = p0Pa,
            speedOfSoundMs = speedOfSound,
            mach = mach,
            gamma = gamma,
        ) ?: return null

        if (massFlux <= 0.0) {
            return null
        }

        val areaM2 = massFlow / massFlux
        if (areaM2 <= 0.0) {
            return null
        }

        val diameterM = sqrt(4.0 * areaM2 / PI)
        val exitVelocity = exitVelocityMs(
            speedOfSoundMs = speedOfSound,
            mach = mach,
            gamma = gamma,
        ) ?: return null

        return StingerResult(
            massFlowKgPerS = massFlow,
            speedOfSoundMs = speedOfSound,
            stagnationPressureBarAbs = p0Bar,
            pressureRatio = pressureRatio,
            criticalPressureRatio = criticalPressureRatio,
            machNumber = mach,
            exitVelocityMs = exitVelocity,
            massFluxKgPerM2S = massFlux,
            areaM2 = areaM2,
            areaMm2 = areaM2 * M2_TO_MM2,
            innerDiameterMm = diameterM * M_TO_MM,
            flowRegime = if (choked) StingerFlowRegime.CHOKED else StingerFlowRegime.SUBSONIC,
        )
    }

    fun criticalPressureRatio(gamma: Double = DEFAULT_GAMMA): Double? {
        if (gamma <= 1.0) return null
        return (2.0 / (gamma + 1.0)).pow(gamma / (gamma - 1.0))
    }

    fun machFromPressureRatio(pressureRatio: Double, gamma: Double = DEFAULT_GAMMA): Double? {
        if (gamma <= 1.0 || pressureRatio <= 0.0 || pressureRatio > 1.0) {
            return null
        }
        val term = pressureRatio.pow(-(gamma - 1.0) / gamma) - 1.0
        if (term < 0.0) {
            return null
        }
        return sqrt(2.0 / (gamma - 1.0) * term)
    }

    private fun massFluxKgPerM2S(
        stagnationPressurePa: Double,
        speedOfSoundMs: Double,
        mach: Double,
        gamma: Double,
    ): Double? {
        if (stagnationPressurePa <= 0.0 || speedOfSoundMs <= 0.0 || mach < 0.0 || gamma <= 1.0) {
            return null
        }
        val factor = 1.0 + (gamma - 1.0) / 2.0 * mach * mach
        val exponent = -(gamma + 1.0) / (2.0 * (gamma - 1.0))
        return stagnationPressurePa * (gamma / speedOfSoundMs) * mach * factor.pow(exponent)
    }

    private fun exitVelocityMs(
        speedOfSoundMs: Double,
        mach: Double,
        gamma: Double,
    ): Double? {
        if (speedOfSoundMs <= 0.0 || mach < 0.0 || gamma <= 1.0) {
            return null
        }
        val localSpeedOfSound = speedOfSoundMs / sqrt(1.0 + (gamma - 1.0) / 2.0 * mach * mach)
        return mach * localSpeedOfSound
    }
}
