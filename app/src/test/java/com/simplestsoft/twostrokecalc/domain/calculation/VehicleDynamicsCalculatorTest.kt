package com.simplestsoft.twostrokecalc.domain.calculation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VehicleDynamicsCalculatorTest {

    private fun vespaInput(
        massKg: Double = 180.0,
        powerPs: Double = 12.0,
        engineRpm: Double = 8000.0,
        analysisSpeedKmh: Double = 0.0,
    ) = VehicleDynamicsInput(
        massKg = massKg,
        engineMode = VehicleDynamicsEngineMode.POWER,
        powerPs = powerPs,
        torqueNm = null,
        engineRpm = engineRpm,
        maxEngineRpm = 9500.0,
        primaryPinion = 23,
        primaryGear = 64,
        stages = listOf(
            VehicleDynamicsGearInput(12, 57),
            VehicleDynamicsGearInput(13, 42),
            VehicleDynamicsGearInput(17, 38),
            VehicleDynamicsGearInput(21, 35),
        ),
        wheelCircumferenceMm = 1307.0,
        shiftRpm = 8000.0,
        drivetrainEfficiency = 0.90,
        dragCoefficient = 0.65,
        frontalAreaM2 = 0.50,
        rollingResistance = 0.015,
        airDensityKgM3 = 1.204,
        massFactor = 1.10,
        gradientPercent = 0.0,
        tractionLimitG = null,
        analysisSpeedKmh = analysisSpeedKmh,
        targetSpeedKmh = 100.0,
        selectedGearIndex = 0,
    )

    @Test
    fun driveForce_fromTorque() {
        val force = VehicleDynamicsCalculator.driveForceN(
            torqueNm = 10.0,
            gearRatio = 15.0,
            drivetrainEfficiency = 0.9,
            wheelRadiusM = 0.208,
        )
        assertEquals(649.04, force, 0.1)
    }

    @Test
    fun dragForce_increasesWithSpeedSquared() {
        val at10 = VehicleDynamicsCalculator.dragForceN(10.0, 0.65, 0.5, 1.204)
        val at20 = VehicleDynamicsCalculator.dragForceN(20.0, 0.65, 0.5, 1.204)
        assertEquals(4.0, at20 / at10, 0.001)
    }

    @Test
    fun engineTorque_fromPower() {
        val torque = VehicleDynamicsCalculator.engineTorqueNm(
            mode = VehicleDynamicsEngineMode.POWER,
            powerPs = 12.0,
            torqueNm = null,
            rpm = 8000.0,
        )
        assertNotNull(torque)
        assertTrue(torque!! > 0.0)
    }

    @Test
    fun calculate_gearSnapshots_orderedByRatio() {
        val result = VehicleDynamicsCalculator.calculate(vespaInput())!!
        assertEquals(4, result.gearSnapshots.size)
        assertTrue(result.gearSnapshots.first().gearRatio > result.gearSnapshots.last().gearRatio)
        assertTrue(result.gearSnapshots.first().speedAtEngineRpmKmh < result.gearSnapshots.last().speedAtEngineRpmKmh)
    }

    @Test
    fun calculate_firstGearStrongerAccelerationAtStandstill() {
        val result = VehicleDynamicsCalculator.calculate(vespaInput(analysisSpeedKmh = 0.0))!!
        val first = result.gearSnapshots.first()
        val last = result.gearSnapshots.last()
        assertTrue(first.accelerationG > last.accelerationG)
        assertTrue(first.forces.driveForceN > last.forces.driveForceN)
    }

    @Test
    fun calculate_topSpeed_inHighestGear() {
        val result = VehicleDynamicsCalculator.calculate(vespaInput())!!
        assertNotNull(result.topSpeedKmh)
        assertTrue(result.topSpeedKmh!! > 50.0)
        assertNotNull(result.topSpeedGear)
        assertNotNull(result.topSpeedLimit)
    }

    @Test
    fun calculate_sprintToTarget() {
        val result = VehicleDynamicsCalculator.calculate(vespaInput().copy(targetSpeedKmh = 60.0))!!
        val sprint = result.sprintToTarget
        assertNotNull(sprint)
        assertTrue(sprint!!.timeSeconds > 0.0)
        assertTrue(sprint.finalSpeedKmh > 0.0)
    }

    @Test
    fun calculate_requiredGearRatio() {
        val result = VehicleDynamicsCalculator.calculate(vespaInput())!!
        assertNotNull(result.requiredGearRatioForTarget)
        assertTrue(result.requiredGearRatioForTarget!! > 0.0)
    }

    @Test
    fun calculate_accelerationCurves_perGear() {
        val result = VehicleDynamicsCalculator.calculate(vespaInput())!!
        assertEquals(4, result.accelerationCurves.size)
        result.accelerationCurves.values.forEach { points ->
            assertTrue(points.isNotEmpty())
            assertTrue(points.first().accelerationG >= points.last().accelerationG)
        }
    }

    @Test
    fun calculate_quarterMileEstimate() {
        val result = VehicleDynamicsCalculator.calculate(vespaInput())!!
        assertNotNull(result.quarterMileEstimateSeconds)
        assertNotNull(result.quarterMileTrapSpeedKmh)
        assertTrue(result.quarterMileEstimateSeconds!! > 10.0)
    }

    @Test
    fun calculate_tractionLimit_capsAcceleration() {
        val unlimited = VehicleDynamicsCalculator.calculate(
            vespaInput(analysisSpeedKmh = 0.0).copy(tractionLimitG = null),
        )!!
        val limited = VehicleDynamicsCalculator.calculate(
            vespaInput(analysisSpeedKmh = 0.0).copy(tractionLimitG = 0.05),
        )!!
        assertTrue(limited.gearSnapshots.first().tractionLimited)
        assertTrue(
            limited.gearSnapshots.first().accelerationG <=
                unlimited.gearSnapshots.first().accelerationG + 0.001,
        )
    }

    @Test
    fun calculate_invalidInput() {
        assertNull(VehicleDynamicsCalculator.calculate(vespaInput(massKg = 0.0)))
        assertNull(
            VehicleDynamicsCalculator.calculate(
                vespaInput().copy(
                    engineMode = VehicleDynamicsEngineMode.TORQUE,
                    powerPs = null,
                    torqueNm = null,
                ),
            ),
        )
    }

    @Test
    fun requiredGearRatio_matchesGearCalculator() {
        val ratio = VehicleDynamicsCalculator.requiredGearRatio(
            targetSpeedKmh = 100.0,
            engineRpm = 8000.0,
            wheelCircumferenceMm = 1307.0,
            rpmConstant = GearCalculator.DEFAULT_RPM_CONSTANT,
        )!!
        val speed = GearCalculator.speedKmh(
            rpm = 8000.0,
            wheelCircumferenceMm = 1307.0,
            gearRatio = ratio,
            rpmConstant = GearCalculator.DEFAULT_RPM_CONSTANT,
        )
        assertEquals(100.0, speed, 0.01)
    }
}
