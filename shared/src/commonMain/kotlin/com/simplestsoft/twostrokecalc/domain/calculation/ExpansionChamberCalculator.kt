package com.simplestsoft.twostrokecalc.domain.calculation

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sqrt

enum class ExpansionChamberPreset {
    ENDURO,
    CROSS,
    GRAND_PRIX,
    CUSTOM,
}

enum class ExpansionChamberDiffuserStages(val stageCount: Int) {
    ONE(1),
    TWO(2),
    THREE(3),
    ;

    companion object {
        fun fromStageCount(count: Int): ExpansionChamberDiffuserStages? =
            entries.firstOrNull { it.stageCount == count }
    }
}

data class ExpansionChamberCoefficients(
    val k0: Double,
    val k1: Double,
    val k2: Double,
    val hornCoefficient: Double,
) {
    companion object {
        fun forPreset(preset: ExpansionChamberPreset): ExpansionChamberCoefficients = when (preset) {
            ExpansionChamberPreset.ENDURO -> ExpansionChamberCoefficients(
                k0 = 0.70,
                k1 = 1.125,
                k2 = 2.25,
                hornCoefficient = 1.5,
            )
            ExpansionChamberPreset.CROSS -> ExpansionChamberCoefficients(
                k0 = 0.62,
                k1 = 1.10,
                k2 = 2.50,
                hornCoefficient = 1.8,
            )
            ExpansionChamberPreset.GRAND_PRIX -> ExpansionChamberCoefficients(
                k0 = 0.58,
                k1 = 1.05,
                k2 = 3.00,
                hornCoefficient = 1.0,
            )
            ExpansionChamberPreset.CUSTOM -> ExpansionChamberCoefficients(
                k0 = 0.70,
                k1 = 1.125,
                k2 = 2.25,
                hornCoefficient = 1.5,
            )
        }
    }
}

data class ExpansionChamberSegment(
    val id: ExpansionChamberSegmentId,
    val lengthMm: Double,
    val startDiameterMm: Double,
    val endDiameterMm: Double,
)

enum class ExpansionChamberSegmentId {
    HEADER,
    DIFFUSER_1,
    DIFFUSER_2,
    DIFFUSER_3,
    BELLY,
    BAFFLE,
    STINGER,
}

data class ExpansionChamberOptionalInputs(
    val bmepBar: Double? = null,
    val exhaustGasTempCelsius: Double? = null,
    val speedOfSoundMs: Double? = null,
    val specificHeatRatio: Double? = null,
    val gasConstant: Double? = null,
    val portAreaMm2: Double? = null,
    val equivalentPortDiameterMm: Double? = null,
)

data class ExpansionChamberInput(
    val exhaustPortWidthMm: Double?,
    val exhaustPortHeightMm: Double?,
    val exhaustPortDurationDeg: Double,
    val transferPortDurationDeg: Double,
    val rpm: Double,
    val powerPs: Double?,
    val displacementCc: Double?,
    val diffuserStages: ExpansionChamberDiffuserStages,
    val coefficients: ExpansionChamberCoefficients,
    val optionalInputs: ExpansionChamberOptionalInputs = ExpansionChamberOptionalInputs(),
)

data class ExpansionChamberResult(
    val portAreaMm2: Double,
    val equivalentPortDiameterMm: Double,
    val bmepBar: Double,
    val exhaustGasTempCelsius: Double,
    val speedOfSoundMs: Double,
    val specificHeatRatio: Double,
    val gasConstant: Double,
    val tunedLengthMm: Double,
    val diffuserStageCount: Int,
    val hornCoefficient: Double,
    val diametersMm: Map<String, Double>,
    val segments: List<ExpansionChamberSegment>,
    val totalLengthMm: Double,
    val usedManualBmep: Boolean,
    val usedManualExhaustTemp: Boolean,
    val usedManualSpeedOfSound: Boolean,
    val usedManualPortArea: Boolean,
    val usedManualEquivalentPortDiameter: Boolean,
) {
    val diffuserSegments: List<ExpansionChamberSegment>
        get() = segments.filter { it.id.name.startsWith("DIFFUSER") }
}

object ExpansionChamberCalculator {
    private const val PS_TO_KW = 1.36
    private const val BMEP_FACTOR = 600_000.0
    private const val DEFAULT_GAMMA = 1.4
    private const val DEFAULT_GAS_CONSTANT = 287.0

    private data class ResolvedWaveProperties(
        val bmepBar: Double,
        val exhaustGasTempCelsius: Double,
        val speedOfSoundMs: Double,
        val specificHeatRatio: Double,
        val gasConstant: Double,
        val usedManualBmep: Boolean,
        val usedManualExhaustTemp: Boolean,
        val usedManualSpeedOfSound: Boolean,
    )

    fun calculate(input: ExpansionChamberInput): ExpansionChamberResult? {
        val optional = input.optionalInputs
        val gamma = optional.specificHeatRatio?.takeIf { it > 0.0 } ?: DEFAULT_GAMMA
        val gasConstant = optional.gasConstant?.takeIf { it > 0.0 } ?: DEFAULT_GAS_CONSTANT

        if (
            input.exhaustPortDurationDeg <= 0.0 ||
            input.transferPortDurationDeg <= 0.0 ||
            input.rpm <= 0.0 ||
            input.coefficients.k0 <= 0.0 ||
            input.coefficients.k1 <= 0.0 ||
            input.coefficients.k2 <= 0.0 ||
            (input.diffuserStages != ExpansionChamberDiffuserStages.ONE &&
                input.coefficients.hornCoefficient <= 0.0) ||
            optional.bmepBar?.let { it <= 0.0 } == true ||
            optional.exhaustGasTempCelsius?.let { it <= -273.0 } == true ||
            optional.speedOfSoundMs?.let { it <= 0.0 } == true ||
            optional.portAreaMm2?.let { it <= 0.0 } == true ||
            optional.equivalentPortDiameterMm?.let { it <= 0.0 } == true
        ) {
            return null
        }

        val portGeometry = resolvePortGeometry(input) ?: return null
        val waveProperties = resolveWaveProperties(input, gamma, gasConstant) ?: return null

        val tunedLength = 83.3 * waveProperties.speedOfSoundMs * input.exhaustPortDurationDeg / input.rpm

        val (segments, diameters) = buildSegments(
            stageCount = input.diffuserStages.stageCount,
            equivalentPortDiameter = portGeometry.equivalentPortDiameterMm,
            tunedLength = tunedLength,
            k0 = input.coefficients.k0,
            k1 = input.coefficients.k1,
            k2 = input.coefficients.k2,
            hornCoefficient = input.coefficients.hornCoefficient,
        )

        return ExpansionChamberResult(
            portAreaMm2 = portGeometry.portAreaMm2,
            equivalentPortDiameterMm = portGeometry.equivalentPortDiameterMm,
            bmepBar = waveProperties.bmepBar,
            exhaustGasTempCelsius = waveProperties.exhaustGasTempCelsius,
            speedOfSoundMs = waveProperties.speedOfSoundMs,
            specificHeatRatio = waveProperties.specificHeatRatio,
            gasConstant = waveProperties.gasConstant,
            tunedLengthMm = tunedLength,
            diffuserStageCount = input.diffuserStages.stageCount,
            hornCoefficient = input.coefficients.hornCoefficient,
            diametersMm = diameters,
            segments = segments,
            totalLengthMm = segments.sumOf { it.lengthMm },
            usedManualBmep = waveProperties.usedManualBmep,
            usedManualExhaustTemp = waveProperties.usedManualExhaustTemp,
            usedManualSpeedOfSound = waveProperties.usedManualSpeedOfSound,
            usedManualPortArea = portGeometry.usedManualPortArea,
            usedManualEquivalentPortDiameter = portGeometry.usedManualEquivalentPortDiameter,
        )
    }

    private data class ResolvedPortGeometry(
        val portAreaMm2: Double,
        val equivalentPortDiameterMm: Double,
        val usedManualPortArea: Boolean,
        val usedManualEquivalentPortDiameter: Boolean,
    )

    private fun resolvePortGeometry(input: ExpansionChamberInput): ResolvedPortGeometry? {
        val optional = input.optionalInputs
        val width = input.exhaustPortWidthMm
        val height = input.exhaustPortHeightMm
        val manualArea = optional.portAreaMm2?.takeIf { it > 0.0 }
        val manualDiameter = optional.equivalentPortDiameterMm?.takeIf { it > 0.0 }

        val portArea = when {
            manualArea != null -> manualArea
            width != null && height != null && width > 0.0 && height > 0.0 -> width * height
            manualDiameter != null -> PI * manualDiameter * manualDiameter / 4.0
            else -> null
        } ?: return null

        val equivalentPortDiameter = manualDiameter ?: sqrt(4.0 * portArea / PI)

        if (equivalentPortDiameter <= 0.0) {
            return null
        }

        return ResolvedPortGeometry(
            portAreaMm2 = portArea,
            equivalentPortDiameterMm = equivalentPortDiameter,
            usedManualPortArea = manualArea != null,
            usedManualEquivalentPortDiameter = manualDiameter != null,
        )
    }

    private fun resolveWaveProperties(
        input: ExpansionChamberInput,
        gamma: Double,
        gasConstant: Double,
    ): ResolvedWaveProperties? {
        val optional = input.optionalInputs

        val bmep = optional.bmepBar ?: run {
            val powerPs = input.powerPs ?: return null
            val displacementCc = input.displacementCc ?: return null
            if (powerPs <= 0.0 || displacementCc <= 0.0) {
                return null
            }
            val powerKw = powerPs / PS_TO_KW
            BMEP_FACTOR * powerKw / (displacementCc * input.rpm)
        }

        val exhaustTemp = optional.exhaustGasTempCelsius ?: exhaustTemperatureFromBmep(bmep)

        val speedOfSound = optional.speedOfSoundMs
            ?: sqrt(gamma * gasConstant * (exhaustTemp + 273.0))

        return ResolvedWaveProperties(
            bmepBar = bmep,
            exhaustGasTempCelsius = exhaustTemp,
            speedOfSoundMs = speedOfSound,
            specificHeatRatio = gamma,
            gasConstant = gasConstant,
            usedManualBmep = optional.bmepBar != null,
            usedManualExhaustTemp = optional.exhaustGasTempCelsius != null,
            usedManualSpeedOfSound = optional.speedOfSoundMs != null,
        )
    }

    fun calculateEquivalentPortDiameterMm(portAreaMm2: Double): Double? {
        if (portAreaMm2 <= 0.0) {
            return null
        }
        return sqrt(4.0 * portAreaMm2 / PI)
    }

    fun calculateBmepBar(powerPs: Double, displacementCc: Double, rpm: Double): Double? {
        if (powerPs <= 0.0 || displacementCc <= 0.0 || rpm <= 0.0) {
            return null
        }
        val powerKw = powerPs / PS_TO_KW
        return BMEP_FACTOR * powerKw / (displacementCc * rpm)
    }

    fun calculateSpeedOfSoundMs(
        exhaustGasTempCelsius: Double,
        specificHeatRatio: Double = DEFAULT_GAMMA,
        gasConstant: Double = DEFAULT_GAS_CONSTANT,
    ): Double? {
        if (exhaustGasTempCelsius <= -273.0 || specificHeatRatio <= 0.0 || gasConstant <= 0.0) {
            return null
        }
        return sqrt(specificHeatRatio * gasConstant * (exhaustGasTempCelsius + 273.0))
    }

    private fun buildSegments(
        stageCount: Int,
        equivalentPortDiameter: Double,
        tunedLength: Double,
        k0: Double,
        k1: Double,
        k2: Double,
        hornCoefficient: Double,
    ): Pair<List<ExpansionChamberSegment>, Map<String, Double>> {
        val d1 = k1 * equivalentPortDiameter
        val dMax = k2 * equivalentPortDiameter
        val dStinger = k0 * equivalentPortDiameter

        val lp01 = 0.10 * tunedLength
        val lp45 = 0.11 * tunedLength
        val lp56 = 0.24 * tunedLength
        val lp67 = lp56

        val diffuserLengths = diffuserLengthFractions(stageCount).map { it * tunedLength }
        val diffuserEndDiameters = diffuserEndDiameters(
            stageCount = stageCount,
            d1 = d1,
            dMax = dMax,
            diffuserLengths = diffuserLengths,
            hornCoefficient = hornCoefficient,
        )

        val diffuserSegments = diffuserLengths.mapIndexed { index, length ->
            val startDiameter = if (index == 0) d1 else diffuserEndDiameters[index - 1]
            val endDiameter = diffuserEndDiameters[index]
            ExpansionChamberSegment(
                id = diffuserSegmentId(index),
                lengthMm = length,
                startDiameterMm = startDiameter,
                endDiameterMm = endDiameter,
            )
        }

        val segments = buildList {
            add(ExpansionChamberSegment(ExpansionChamberSegmentId.HEADER, lp01, d1, d1))
            addAll(diffuserSegments)
            add(ExpansionChamberSegment(ExpansionChamberSegmentId.BELLY, lp45, dMax, dMax))
            add(ExpansionChamberSegment(ExpansionChamberSegmentId.BAFFLE, lp56, dMax, dStinger))
            add(ExpansionChamberSegment(ExpansionChamberSegmentId.STINGER, lp67, dStinger, dStinger))
        }

        val diameters = buildDiametersMap(
            stageCount = stageCount,
            d1 = d1,
            diffuserEndDiameters = diffuserEndDiameters,
            dMax = dMax,
            dStinger = dStinger,
        )

        return segments to diameters
    }

    private fun diffuserLengthFractions(stageCount: Int): List<Double> = when (stageCount) {
        1 -> listOf(0.55)
        2 -> listOf(0.41, 0.14)
        3 -> listOf(0.275, 0.183, 0.092)
        else -> error("Unsupported diffuser segment count: $stageCount")
    }

    private fun diffuserEndDiameters(
        stageCount: Int,
        d1: Double,
        dMax: Double,
        diffuserLengths: List<Double>,
        hornCoefficient: Double,
    ): List<Double> = when (stageCount) {
        1 -> listOf(dMax)
        2, 3 -> {
            val diffuserSum = diffuserLengths.sum()
            val logDiameterRatio = ln(dMax / d1)
            diffuserLengths.indices.map { index ->
                val cumulativeLength = diffuserLengths.take(index + 1).sum()
                d1 * exp((cumulativeLength / diffuserSum).pow(hornCoefficient) * logDiameterRatio)
            }
        }
        else -> error("Unsupported diffuser segment count: $stageCount")
    }

    private fun diffuserSegmentId(index: Int): ExpansionChamberSegmentId = when (index) {
        0 -> ExpansionChamberSegmentId.DIFFUSER_1
        1 -> ExpansionChamberSegmentId.DIFFUSER_2
        else -> ExpansionChamberSegmentId.DIFFUSER_3
    }

    private fun buildDiametersMap(
        stageCount: Int,
        d1: Double,
        diffuserEndDiameters: List<Double>,
        dMax: Double,
        dStinger: Double,
    ): Map<String, Double> = buildMap {
        put("D1", d1)
        diffuserEndDiameters.dropLast(1).forEachIndexed { index, diameter ->
            put("D${index + 2}", diameter)
        }
        when (stageCount) {
            1 -> {
                put("D2", dMax)
                put("D3", dStinger)
            }
            2 -> {
                put("D3", dMax)
                put("D4", dStinger)
            }
            3 -> {
                put("D4", dMax)
                put("D5", dStinger)
            }
        }
    }

    fun exhaustTemperatureFromBmep(bmepBar: Double): Double = when {
        bmepBar > 11.0 -> 650.0
        bmepBar > 9.0 -> 600.0
        bmepBar > 8.0 -> 500.0
        else -> 350.0
    }
}
