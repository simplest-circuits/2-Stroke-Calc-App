package com.simplestsoft.twostrokecalc.domain.calculation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GearCalculatorTest {

    private val referenceInput = GearCalculatorInput(
        driveMode = GearDriveMode.MULTI_SPEED,
        outputType = GearOutputType.VEHICLE_SPEED,
        wheelCircumferenceMm = 1307.0,
        rpmConstant = 16000.0,
        shiftRpm = 8000.0,
        primaryPinion = 23,
        primaryGear = 64,
        stages = listOf(
            GearStageConfig(12, 57, 3000.0),
            GearStageConfig(13, 42, 9500.0),
            GearStageConfig(17, 38, 6000.0),
            GearStageConfig(21, 35, 8500.0),
        ),
    )

    @Test
    fun speedKmh_excelGearOneAt3000Rpm() {
        val ratio = GearCalculator.gearRatio(23, 64, 12, 57)!!
        val speed = GearCalculator.speedKmh(
            rpm = 3000.0,
            wheelCircumferenceMm = 1307.0,
            gearRatio = ratio,
        )
        assertEquals(18.54, speed, 0.01)
    }

    @Test
    fun calculate_excelReferenceSection() {
        val result = GearCalculator.calculate(referenceInput)!!

        assertEquals(18.54, result.stages[0].speedsAtReferenceRpmKmh[0], 0.01)
        assertEquals(27.26, result.stages[0].speedsAtReferenceRpmKmh[1], 0.01)
        assertEquals(39.40, result.stages[0].speedsAtReferenceRpmKmh[2], 0.01)
        assertEquals(52.84, result.stages[0].speedsAtReferenceRpmKmh[3], 0.01)
        assertEquals(49.44, result.stages[0].shiftSpeedKmh, 0.01)

        assertEquals(58.71, result.stages[1].speedsAtReferenceRpmKmh[0], 0.01)
        assertEquals(72.69, result.stages[1].shiftSpeedKmh, 0.01)
        assertEquals(23.25, result.stages[1].speedJumpKmh!!, 0.01)
        assertEquals(2558.70, result.stages[1].rpmJump!!, 1.0)

        assertEquals(105.07, result.stages[2].shiftSpeedKmh, 0.01)
        assertEquals(32.37, result.stages[2].speedJumpKmh!!, 0.01)
        assertEquals(2464.99, result.stages[2].rpmJump!!, 1.0)

        assertEquals(140.91, result.stages[3].shiftSpeedKmh, 0.01)
        assertEquals(35.85, result.stages[3].speedJumpKmh!!, 0.01)
        assertEquals(2035.09, result.stages[3].rpmJump!!, 1.0)
    }

    @Test
    fun calculate_threeStages() {
        val result = GearCalculator.calculate(
            referenceInput.copy(
                stages = referenceInput.stages.take(3),
            ),
        )

        assertNotNull(result)
        assertEquals(3, result!!.stages.size)
        assertEquals(GearReferenceLabel.LOW, result.stages[0].referenceLabel)
        assertEquals(GearReferenceLabel.HIGH, result.stages[1].referenceLabel)
        assertEquals(GearReferenceLabel.RESONANCE_ENTRY, result.stages[2].referenceLabel)
    }

    @Test
    fun calculate_sixStages() {
        val stages = referenceInput.stages + listOf(
            GearStageConfig(24, 32, 8500.0),
            GearStageConfig(27, 30, 8500.0),
        )
        val result = GearCalculator.calculate(referenceInput.copy(stages = stages))

        assertNotNull(result)
        assertEquals(6, result!!.stages.size)
        assertEquals(GearReferenceLabel.ADDITIONAL, result.stages[4].referenceLabel)
    }

    @Test
    fun calculate_invalidInput() {
        assertNull(
            GearCalculator.calculate(
                referenceInput.copy(primaryPinion = 0),
            ),
        )
        assertNull(
            GearCalculator.calculate(
                referenceInput.copy(stages = referenceInput.stages.take(1)),
            ),
        )
        assertNull(
            GearCalculator.calculate(
                referenceInput.copy(
                    stages = referenceInput.stages + referenceInput.stages,
                ),
            ),
        )
    }

    @Test
    fun calculate_fixedTwoStage_totalRatio() {
        val result = GearCalculator.calculate(
            referenceInput.copy(
                driveMode = GearDriveMode.FIXED_TWO_STAGE,
                shiftRpm = 0.0,
                stages = listOf(GearStageConfig(12, 57, 3000.0)),
                fixedReferenceRpms = listOf(3000.0, 9500.0, 6000.0, 8500.0),
            ),
        )!!

        assertEquals(1, result.stages.size)
        assertEquals(13.22, result.stages[0].gearRatio, 0.01)
        assertEquals(18.54, result.stages[0].speedsAtReferenceRpmKmh[0], 0.01)
        assertEquals(58.71, result.stages[0].speedsAtReferenceRpmKmh[1], 0.01)
    }

    @Test
    fun engineRpmAtOutputValue_invertsSpeedKmh() {
        val ratio = GearCalculator.gearRatio(23, 64, 12, 57)!!
        val rpm = 8000.0
        val speed = GearCalculator.speedKmh(rpm, 1307.0, ratio)
        val recoveredRpm = GearCalculator.engineRpmAtOutputValue(
            outputValue = speed,
            gearRatio = ratio,
            outputType = GearOutputType.VEHICLE_SPEED,
            wheelCircumferenceMm = 1307.0,
        )
        assertEquals(rpm, recoveredRpm, 0.01)
    }

    @Test
    fun buildMultiSpeedChartSegments_chainAtShiftPoints() {
        val result = GearCalculator.calculate(referenceInput)!!
        val segments = GearCalculator.buildMultiSpeedChartSegments(
            stages = result.stages,
            shiftRpm = referenceInput.shiftRpm,
            chartMaxRpm = 9500.0,
            outputType = GearOutputType.VEHICLE_SPEED,
            wheelCircumferenceMm = referenceInput.wheelCircumferenceMm,
            rpmConstant = referenceInput.rpmConstant,
        )

        assertEquals(4, segments.size)
        assertEquals(0.0, segments[0].coreStartRpm, 0.01)
        assertEquals(0.0, segments[0].coreStartOutput, 0.01)
        assertEquals(8000.0, segments[0].coreEndRpm, 0.01)
        assertEquals(49.44, segments[0].coreEndOutput, 0.01)
        assertEquals(54.44, segments[0].endOutput, 0.01)

        assertEquals(49.44, segments[1].coreStartOutput, 0.01)
        assertEquals(8000.0, segments[1].coreEndRpm, 0.01)
        assertEquals(72.69, segments[1].coreEndOutput, 0.01)
        assertEquals(44.44, segments[1].startOutput, 0.01)
        assertEquals(77.69, segments[1].endOutput, 0.01)
        assertTrue(segments[1].coreStartRpm < referenceInput.shiftRpm)

        assertEquals(72.69, segments[2].coreStartOutput, 0.01)
        assertEquals(105.07, segments[2].coreEndOutput, 0.01)

        assertEquals(105.07, segments[3].coreStartOutput, 0.01)
        assertEquals(9500.0, segments[3].coreEndRpm, 0.01)
        assertTrue(segments[3].coreEndOutput > segments[3].coreStartOutput)
    }

    @Test
    fun resolveResonanceRpmRange_multiSpeedFromStageLabels() {
        val result = GearCalculator.calculate(referenceInput)!!
        val range = GearCalculator.resolveResonanceRpmRange(result)
        assertNotNull(range)
        assertEquals(6000.0, range!!.first, 0.01)
        assertEquals(8500.0, range.second, 0.01)
    }

    @Test
    fun resolveResonanceRpmRange_multiSpeedUsesFallbackWhenStagesMissingResoLabels() {
        val result = GearCalculator.calculate(
            referenceInput.copy(stages = referenceInput.stages.take(2)),
        )!!
        val range = GearCalculator.resolveResonanceRpmRange(
            result = result,
            fallbackEntryRpm = 6200.0,
            fallbackPeakRpm = 8400.0,
        )
        assertNotNull(range)
        assertEquals(6200.0, range!!.first, 0.01)
        assertEquals(8400.0, range.second, 0.01)
    }

    @Test
    fun resolveResonanceRpmRange_fixedRatioFromReferenceRpms() {
        val result = GearCalculator.calculate(
            referenceInput.copy(
                driveMode = GearDriveMode.FIXED_TWO_STAGE,
                shiftRpm = 0.0,
                stages = listOf(GearStageConfig(12, 57, 3000.0)),
                fixedReferenceRpms = listOf(3000.0, 9500.0, 6100.0, 8300.0),
            ),
        )!!
        val range = GearCalculator.resolveResonanceRpmRange(result)
        assertNotNull(range)
        assertEquals(6100.0, range!!.first, 0.01)
        assertEquals(8300.0, range.second, 0.01)
    }

    @Test
    fun calculate_singleStage_outputRpm() {
        val result = GearCalculator.calculate(
            GearCalculatorInput(
                driveMode = GearDriveMode.SINGLE_STAGE,
                outputType = GearOutputType.OUTPUT_RPM,
                wheelCircumferenceMm = 0.0,
                rpmConstant = 16000.0,
                shiftRpm = 0.0,
                primaryPinion = 1,
                primaryGear = 1,
                stages = listOf(GearStageConfig(7, 33, 3000.0)),
                fixedReferenceRpms = listOf(3000.0, 12000.0, 6000.0, 10000.0),
            ),
        )!!

        assertEquals(1, result.stages.size)
        assertEquals(4.71, result.stages[0].gearRatio, 0.01)
        assertEquals(636.4, result.stages[0].speedsAtReferenceRpmKmh[0], 0.1)
        assertEquals(2545.5, result.stages[0].speedsAtReferenceRpmKmh[1], 1.0)
    }
}
