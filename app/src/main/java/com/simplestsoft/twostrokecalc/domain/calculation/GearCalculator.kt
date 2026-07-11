package com.simplestsoft.twostrokecalc.domain.calculation

enum class GearDriveMode {
    /** Mehrgang-Schaltgetriebe mit Primär- und Sekundärübersetzung pro Gang. */
    MULTI_SPEED,
    /** Feste Gesamtübersetzung aus Primär- und Sekundärgetriebe (z. B. CVT-Radausgang). */
    FIXED_TWO_STAGE,
    /** Eine einzige Übersetzung auf die Ausgangswelle (z. B. Kettensäge). */
    SINGLE_STAGE,
}

enum class GearOutputType {
    /** Fahrgeschwindigkeit in km/h (benötigt Radumfang). */
    VEHICLE_SPEED,
    /** Drehzahl an der Ausgangswelle in U/min. */
    OUTPUT_RPM,
}

data class GearStageConfig(
    val secondaryPinion: Int,
    val secondaryGear: Int,
    val referenceRpm: Double,
)

data class GearStageResult(
    val stageNumber: Int,
    val secondaryPinion: Int,
    val secondaryGear: Int,
    val gearRatio: Double,
    val referenceRpm: Double,
    val referenceLabel: GearReferenceLabel,
    val speedsAtReferenceRpmKmh: List<Double>,
    val shiftSpeedKmh: Double,
    val speedJumpKmh: Double?,
    val rpmJump: Double?,
)

enum class GearReferenceLabel {
    LOW,
    HIGH,
    RESONANCE_ENTRY,
    RESONANCE_PEAK,
    ADDITIONAL,
}

data class GearCalculatorInput(
    val driveMode: GearDriveMode = GearDriveMode.MULTI_SPEED,
    val outputType: GearOutputType = GearOutputType.VEHICLE_SPEED,
    val wheelCircumferenceMm: Double,
    val rpmConstant: Double,
    val shiftRpm: Double,
    val primaryPinion: Int,
    val primaryGear: Int,
    val stages: List<GearStageConfig>,
    /** Vier Referenz-Drehzahlen für feste Übersetzungen (niedrig, hoch, Reso Einstieg, Reso Spitze). */
    val fixedReferenceRpms: List<Double> = emptyList(),
)

data class GearCalculatorResult(
    val driveMode: GearDriveMode,
    val outputType: GearOutputType,
    val stages: List<GearStageResult>,
    val maxSpeedAtLowReferenceKmh: Double,
    val maxSpeedAtHighReferenceKmh: Double,
    val fixedReferenceRpms: List<Double> = emptyList(),
)

object GearCalculator {
    /**
     * Calculates gear speeds from the Excel sheet "Getriebe":
     * - Gear ratio = (primaryGear / primaryPinion) × (secondaryGear / secondaryPinion)
     * - Speed [km/h] = (rpm × wheelCircumferenceMm) / (gearRatio × rpmConstant)
     * - Output RPM = engineRpm / gearRatio
     * - RPM jump = (speedJumpKmh × rpmConstant × gearRatio) / wheelCircumferenceMm
     */
    fun calculate(input: GearCalculatorInput): GearCalculatorResult? {
        if (input.rpmConstant <= 0.0) return null

        if (input.outputType == GearOutputType.VEHICLE_SPEED && input.wheelCircumferenceMm <= 0.0) {
            return null
        }

        return when (input.driveMode) {
            GearDriveMode.MULTI_SPEED -> calculateMultiSpeed(input)
            GearDriveMode.FIXED_TWO_STAGE -> calculateFixedRatio(input, twoStage = true)
            GearDriveMode.SINGLE_STAGE -> calculateFixedRatio(input, twoStage = false)
        }
    }

    private fun calculateMultiSpeed(input: GearCalculatorInput): GearCalculatorResult? {
        if (
            input.primaryPinion <= 0 ||
            input.primaryGear <= 0 ||
            input.stages.size !in MIN_STAGE_COUNT..MAX_STAGE_COUNT ||
            input.shiftRpm <= 0.0
        ) {
            return null
        }

        val primaryRatio = input.primaryGear.toDouble() / input.primaryPinion.toDouble()

        val gearRatios = input.stages.map { stage ->
            if (stage.secondaryPinion <= 0 || stage.secondaryGear <= 0 || stage.referenceRpm <= 0.0) {
                return null
            }
            primaryRatio * stage.secondaryGear.toDouble() / stage.secondaryPinion.toDouble()
        }

        val stageResults = input.stages.mapIndexed { index, stage ->
            val speedsAtReference = gearRatios.map { ratio ->
                outputValue(
                    rpm = stage.referenceRpm,
                    gearRatio = ratio,
                    outputType = input.outputType,
                    wheelCircumferenceMm = input.wheelCircumferenceMm,
                    rpmConstant = input.rpmConstant,
                )
            }
            val shiftSpeed = outputValue(
                rpm = input.shiftRpm,
                gearRatio = gearRatios[index],
                outputType = input.outputType,
                wheelCircumferenceMm = input.wheelCircumferenceMm,
                rpmConstant = input.rpmConstant,
            )

            GearStageResult(
                stageNumber = index + 1,
                secondaryPinion = stage.secondaryPinion,
                secondaryGear = stage.secondaryGear,
                gearRatio = gearRatios[index],
                referenceRpm = stage.referenceRpm,
                referenceLabel = referenceLabelForIndex(index),
                speedsAtReferenceRpmKmh = speedsAtReference,
                shiftSpeedKmh = shiftSpeed,
                speedJumpKmh = null,
                rpmJump = null,
            )
        }

        val withJumps = stageResults.mapIndexed { index, stage ->
            if (index == 0) {
                stage
            } else {
                val previousShiftSpeed = stageResults[index - 1].shiftSpeedKmh
                val speedJump = stage.shiftSpeedKmh - previousShiftSpeed
                val rpmJump = if (input.outputType == GearOutputType.VEHICLE_SPEED) {
                    (speedJump * input.rpmConstant * stage.gearRatio) / input.wheelCircumferenceMm
                } else {
                    speedJump * stage.gearRatio
                }
                stage.copy(
                    speedJumpKmh = speedJump,
                    rpmJump = rpmJump,
                )
            }
        }

        return GearCalculatorResult(
            driveMode = input.driveMode,
            outputType = input.outputType,
            stages = withJumps,
            maxSpeedAtLowReferenceKmh = withJumps.maxOfOrNull {
                it.speedsAtReferenceRpmKmh.lastOrNull() ?: 0.0
            } ?: 0.0,
            maxSpeedAtHighReferenceKmh = withJumps.flatMap { stage ->
                stage.speedsAtReferenceRpmKmh.mapIndexedNotNull { speedIndex, speed ->
                    if (speedIndex == stage.stageNumber - 1) speed else null
                }
            }.maxOrNull() ?: 0.0,
        )
    }

    private fun calculateFixedRatio(
        input: GearCalculatorInput,
        twoStage: Boolean,
    ): GearCalculatorResult? {
        if (input.stages.size != 1) return null

        val stage = input.stages.first()
        if (stage.secondaryPinion <= 0 || stage.secondaryGear <= 0) return null

        val referenceRpms = resolvedFixedReferenceRpms(input.fixedReferenceRpms)
        if (referenceRpms.any { it <= 0.0 }) return null

        val ratio = if (twoStage) {
            if (input.primaryPinion <= 0 || input.primaryGear <= 0) return null
            gearRatio(
                primaryPinion = input.primaryPinion,
                primaryGear = input.primaryGear,
                secondaryPinion = stage.secondaryPinion,
                secondaryGear = stage.secondaryGear,
            ) ?: return null
        } else {
            stage.secondaryGear.toDouble() / stage.secondaryPinion.toDouble()
        }

        val speedsAtReference = referenceRpms.map { rpm ->
            outputValue(
                rpm = rpm,
                gearRatio = ratio,
                outputType = input.outputType,
                wheelCircumferenceMm = input.wheelCircumferenceMm,
                rpmConstant = input.rpmConstant,
            )
        }

        val stageResult = GearStageResult(
            stageNumber = 1,
            secondaryPinion = stage.secondaryPinion,
            secondaryGear = stage.secondaryGear,
            gearRatio = ratio,
            referenceRpm = referenceRpms.first(),
            referenceLabel = GearReferenceLabel.LOW,
            speedsAtReferenceRpmKmh = speedsAtReference,
            shiftSpeedKmh = 0.0,
            speedJumpKmh = null,
            rpmJump = null,
        )

        return GearCalculatorResult(
            driveMode = input.driveMode,
            outputType = input.outputType,
            stages = listOf(stageResult),
            maxSpeedAtLowReferenceKmh = speedsAtReference.first(),
            maxSpeedAtHighReferenceKmh = speedsAtReference[1],
            fixedReferenceRpms = referenceRpms,
        )
    }

    fun referenceLabelForIndex(index: Int): GearReferenceLabel = when (index) {
        0 -> GearReferenceLabel.LOW
        1 -> GearReferenceLabel.HIGH
        2 -> GearReferenceLabel.RESONANCE_ENTRY
        3 -> GearReferenceLabel.RESONANCE_PEAK
        else -> GearReferenceLabel.ADDITIONAL
    }

    fun defaultReferenceRpm(index: Int): Double = when (index) {
        0 -> DEFAULT_REFERENCE_LOW_RPM
        1 -> DEFAULT_REFERENCE_HIGH_RPM
        2 -> DEFAULT_RESONANCE_ENTRY_RPM
        3 -> DEFAULT_RESONANCE_PEAK_RPM
        else -> DEFAULT_RESONANCE_PEAK_RPM
    }

    private fun resolvedFixedReferenceRpms(provided: List<Double>): List<Double> {
        val defaults = listOf(
            DEFAULT_REFERENCE_LOW_RPM,
            DEFAULT_REFERENCE_HIGH_RPM,
            DEFAULT_RESONANCE_ENTRY_RPM,
            DEFAULT_RESONANCE_PEAK_RPM,
        )
        return if (provided.size == defaults.size) provided else defaults
    }

    fun gearRatio(
        primaryPinion: Int,
        primaryGear: Int,
        secondaryPinion: Int,
        secondaryGear: Int,
    ): Double? {
        if (primaryPinion <= 0 || primaryGear <= 0 || secondaryPinion <= 0 || secondaryGear <= 0) {
            return null
        }
        return primaryGear.toDouble() / primaryPinion * secondaryGear.toDouble() / secondaryPinion
    }

    fun singleStageRatio(pinion: Int, gear: Int): Double? {
        if (pinion <= 0 || gear <= 0) return null
        return gear.toDouble() / pinion.toDouble()
    }

    fun speedKmh(
        rpm: Double,
        wheelCircumferenceMm: Double,
        gearRatio: Double,
        rpmConstant: Double = DEFAULT_RPM_CONSTANT,
    ): Double = (rpm * wheelCircumferenceMm) / (gearRatio * rpmConstant)

    fun outputRpm(engineRpm: Double, gearRatio: Double): Double = engineRpm / gearRatio

    fun outputValue(
        rpm: Double,
        gearRatio: Double,
        outputType: GearOutputType,
        wheelCircumferenceMm: Double,
        rpmConstant: Double = DEFAULT_RPM_CONSTANT,
    ): Double = when (outputType) {
        GearOutputType.VEHICLE_SPEED -> speedKmh(rpm, wheelCircumferenceMm, gearRatio, rpmConstant)
        GearOutputType.OUTPUT_RPM -> outputRpm(rpm, gearRatio)
    }

    /** Inverse of [outputValue]: engine RPM for a given output at a fixed gear ratio. */
    fun engineRpmAtOutputValue(
        outputValue: Double,
        gearRatio: Double,
        outputType: GearOutputType,
        wheelCircumferenceMm: Double,
        rpmConstant: Double = DEFAULT_RPM_CONSTANT,
    ): Double = when (outputType) {
        GearOutputType.VEHICLE_SPEED ->
            (outputValue * gearRatio * rpmConstant) / wheelCircumferenceMm
        GearOutputType.OUTPUT_RPM -> outputValue * gearRatio
    }

    data class GearChartSegment(
        val startRpm: Double,
        val startOutput: Double,
        val endRpm: Double,
        val endOutput: Double,
        val coreStartRpm: Double,
        val coreStartOutput: Double,
        val coreEndRpm: Double,
        val coreEndOutput: Double,
    )

    fun buildMultiSpeedChartSegments(
        stages: List<GearStageResult>,
        shiftRpm: Double,
        chartMaxRpm: Double,
        outputType: GearOutputType,
        wheelCircumferenceMm: Double,
        rpmConstant: Double = DEFAULT_RPM_CONSTANT,
        outputBuffer: Double = chartSegmentOutputBuffer(outputType),
    ): List<GearChartSegment> = stages.mapIndexed { index, stage ->
        val isFirst = index == 0
        val isLast = index == stages.lastIndex
        val previousShiftOutput = stages.getOrNull(index - 1)?.shiftSpeedKmh

        val coreStartRpm = if (isFirst) {
            0.0
        } else {
            engineRpmAtOutputValue(
                outputValue = previousShiftOutput!!,
                gearRatio = stage.gearRatio,
                outputType = outputType,
                wheelCircumferenceMm = wheelCircumferenceMm,
                rpmConstant = rpmConstant,
            )
        }
        val coreStartOutput = if (isFirst) 0.0 else previousShiftOutput!!

        val coreEndRpm = if (isLast) chartMaxRpm else shiftRpm
        val coreEndOutput = if (isLast) {
            outputValue(
                rpm = chartMaxRpm,
                gearRatio = stage.gearRatio,
                outputType = outputType,
                wheelCircumferenceMm = wheelCircumferenceMm,
                rpmConstant = rpmConstant,
            )
        } else {
            stage.shiftSpeedKmh
        }

        val bufferedStartOutput = if (isFirst) {
            0.0
        } else {
            coreStartOutput - outputBuffer
        }
        val bufferedEndOutput = coreEndOutput + outputBuffer

        val startRpm = engineRpmAtOutputValue(
            outputValue = bufferedStartOutput,
            gearRatio = stage.gearRatio,
            outputType = outputType,
            wheelCircumferenceMm = wheelCircumferenceMm,
            rpmConstant = rpmConstant,
        )
        val endRpm = engineRpmAtOutputValue(
            outputValue = bufferedEndOutput,
            gearRatio = stage.gearRatio,
            outputType = outputType,
            wheelCircumferenceMm = wheelCircumferenceMm,
            rpmConstant = rpmConstant,
        )

        GearChartSegment(
            startRpm = startRpm,
            startOutput = bufferedStartOutput,
            endRpm = endRpm,
            endOutput = bufferedEndOutput,
            coreStartRpm = coreStartRpm,
            coreStartOutput = coreStartOutput,
            coreEndRpm = coreEndRpm,
            coreEndOutput = coreEndOutput,
        )
    }

    fun chartSegmentOutputBuffer(outputType: GearOutputType): Double = when (outputType) {
        GearOutputType.VEHICLE_SPEED -> CHART_SEGMENT_SPEED_BUFFER_KMH
        GearOutputType.OUTPUT_RPM -> CHART_SEGMENT_OUTPUT_RPM_BUFFER
    }

    /** Engine-RPM band between resonance entry and peak for chart highlighting. */
    fun resolveResonanceRpmRange(
        result: GearCalculatorResult,
        fallbackEntryRpm: Double? = null,
        fallbackPeakRpm: Double? = null,
    ): Pair<Double, Double>? {
        val entryRpm: Double?
        val peakRpm: Double?
        when (result.driveMode) {
            GearDriveMode.MULTI_SPEED -> {
                entryRpm = result.stages
                    .firstOrNull { it.referenceLabel == GearReferenceLabel.RESONANCE_ENTRY }
                    ?.referenceRpm
                    ?: fallbackEntryRpm
                peakRpm = result.stages
                    .firstOrNull { it.referenceLabel == GearReferenceLabel.RESONANCE_PEAK }
                    ?.referenceRpm
                    ?: fallbackPeakRpm
            }
            GearDriveMode.FIXED_TWO_STAGE,
            GearDriveMode.SINGLE_STAGE,
            -> {
                entryRpm = result.fixedReferenceRpms.getOrNull(2) ?: fallbackEntryRpm
                peakRpm = result.fixedReferenceRpms.getOrNull(3) ?: fallbackPeakRpm
            }
        }
        if (entryRpm == null || peakRpm == null || entryRpm <= 0.0 || peakRpm <= 0.0) {
            return null
        }
        val startRpm = minOf(entryRpm, peakRpm)
        val endRpm = maxOf(entryRpm, peakRpm)
        if (endRpm <= startRpm) return null
        return startRpm to endRpm
    }

    const val MIN_STAGE_COUNT = 2
    const val MAX_STAGE_COUNT = 6
    /** @deprecated Use [MIN_STAGE_COUNT] / [MAX_STAGE_COUNT] */
    const val STAGE_COUNT = 4
    const val MULTI_SPEED_STAGE_COUNT = STAGE_COUNT
    const val DEFAULT_WHEEL_CIRCUMFERENCE_MM = 1307.0
    const val DEFAULT_RPM_CONSTANT = 16000.0
    const val DEFAULT_REFERENCE_LOW_RPM = 3000.0
    const val DEFAULT_REFERENCE_HIGH_RPM = 9500.0
    const val DEFAULT_RESONANCE_ENTRY_RPM = 6000.0
    const val DEFAULT_RESONANCE_PEAK_RPM = 8500.0
    const val DEFAULT_SHIFT_RPM = 8000.0
    const val CHART_SEGMENT_SPEED_BUFFER_KMH = 5.0
    const val CHART_SEGMENT_OUTPUT_RPM_BUFFER = 300.0
}
