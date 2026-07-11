package com.simplestsoft.twostrokecalc.domain.calculation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI

class FlywheelInertiaCalculatorTest {

    @Test
    fun inertiaFromGeometry_solidSteelDisc() {
        // 300 mm diameter, 100 mm length, steel
        val result = FlywheelInertiaCalculator.inertiaFromGeometry(
            FlywheelGeometryInput(
                cylinderType = FlywheelCylinderType.SOLID,
                outerDiameterMm = 300.0,
                lengthMm = 100.0,
                material = FlywheelMaterial.STEEL,
            ),
        )!!

        val density = 7850.0
        val r = 0.15
        val length = 0.1
        val mass = density * PI * r * r * length
        val expectedInertia = 0.5 * mass * r * r

        assertEquals(mass, result.massKg!!, 0.01)
        assertEquals(expectedInertia, result.inertiaKgm2, 0.0001)
    }

    @Test
    fun inertiaFromGeometry_hollowPipe() {
        val result = FlywheelInertiaCalculator.inertiaFromGeometry(
            FlywheelGeometryInput(
                cylinderType = FlywheelCylinderType.HOLLOW,
                outerDiameterMm = 300.0,
                innerDiameterMm = 280.0,
                lengthMm = 500.0,
                material = FlywheelMaterial.STEEL,
            ),
        )!!

        val rOut = 0.15
        val rIn = 0.14
        val length = 0.5
        val density = 7850.0
        val mass = density * PI * (rOut * rOut - rIn * rIn) * length
        val expectedInertia = 0.5 * mass * (rOut * rOut + rIn * rIn)

        assertEquals(expectedInertia, result.inertiaKgm2, 0.0001)
    }

    @Test
    fun inertiaFromMass_solidCylinder() {
        val result = FlywheelInertiaCalculator.inertiaFromMass(
            FlywheelMassInput(
                cylinderType = FlywheelCylinderType.SOLID,
                massKg = 38.0,
                outerDiameterMm = 380.0,
            ),
        )!!

        val r = 0.19
        val expected = 0.5 * 38.0 * r * r
        assertEquals(expected, result.inertiaKgm2, 0.0001)
    }

    @Test
    fun equivalentMass_fromInertiaAndRadius() {
        val inertia = 10.0
        val radiusMm = 150.0 // 300 mm diameter roller
        val em = FlywheelInertiaCalculator.equivalentMassKg(inertia, radiusMm)!!
        assertEquals(inertia / 0.15 / 0.15, em, 0.01)
    }

    @Test
    fun designEngineDyno_typicalSmallEngine() {
        val result = FlywheelInertiaCalculator.designEngineDyno(
            EngineDynoDesignInput(
                peakPowerPs = 50.0,
                peakEngineRpm = 10000.0,
                runTimeSeconds = 10.0,
                engineToFlywheelRatio = 1.0,
            ),
        )!!

        val omega = 10000.0 * 2.0 * PI / 60.0
        val powerWatt = 50.0 * FlywheelInertiaCalculator.PS_TO_WATT
        val expected = powerWatt * 10.0 / (omega * omega)
        assertEquals(expected, result.requiredFlywheelInertiaKgm2, 0.001)
    }

    @Test
    fun designEngineDyno_withGearRatio() {
        val direct = FlywheelInertiaCalculator.designEngineDyno(
            EngineDynoDesignInput(
                peakPowerPs = 30.0,
                peakEngineRpm = 12000.0,
                runTimeSeconds = 8.0,
                engineToFlywheelRatio = 1.0,
            ),
        )!!
        val geared = FlywheelInertiaCalculator.designEngineDyno(
            EngineDynoDesignInput(
                peakPowerPs = 30.0,
                peakEngineRpm = 12000.0,
                runTimeSeconds = 8.0,
                engineToFlywheelRatio = 2.0,
            ),
        )!!
        assertEquals(direct.requiredFlywheelInertiaKgm2 * 4.0, geared.requiredFlywheelInertiaKgm2, 0.0001)
    }

    @Test
    fun reduceInertiaToSpeed_doublesRpm_quartersInertia() {
        val result = FlywheelInertiaCalculator.reduceInertiaToSpeed(
            SpeedReductionInput(
                inertiaKgm2 = 4.0,
                sourceRpm = 1000.0,
                targetRpm = 2000.0,
            ),
        )!!
        assertEquals(1.0, result.reducedInertiaKgm2, 0.0001)
    }

    @Test
    fun inertiaFromGeometry_rejectsInvalidHollow() {
        assertNull(
            FlywheelInertiaCalculator.inertiaFromGeometry(
                FlywheelGeometryInput(
                    cylinderType = FlywheelCylinderType.HOLLOW,
                    outerDiameterMm = 200.0,
                    innerDiameterMm = 200.0,
                    lengthMm = 100.0,
                    material = FlywheelMaterial.STEEL,
                ),
            ),
        )
    }

    @Test
    fun geometry_includesAdditionalInertia() {
        val base = FlywheelInertiaCalculator.inertiaFromGeometry(
            FlywheelGeometryInput(
                cylinderType = FlywheelCylinderType.SOLID,
                outerDiameterMm = 200.0,
                lengthMm = 50.0,
                material = FlywheelMaterial.STEEL,
            ),
        )!!
        val withExtra = FlywheelInertiaCalculator.inertiaFromGeometry(
            FlywheelGeometryInput(
                cylinderType = FlywheelCylinderType.SOLID,
                outerDiameterMm = 200.0,
                lengthMm = 50.0,
                material = FlywheelMaterial.STEEL,
                additionalInertiaKgm2 = 0.5,
            ),
        )!!
        assertEquals(base.inertiaKgm2 + 0.5, withExtra.inertiaKgm2, 0.0001)
    }

    @Test
    fun simulateDynoRun_constantTorque_reachesPeakRpm() {
        val result = FlywheelInertiaCalculator.simulateDynoRun(
            DynoRunSimulationInput(
                totalInertiaKgm2 = 0.5,
                peakPowerPs = 50.0,
                peakEngineRpm = 10000.0,
                startRpm = 3000.0,
                torqueModel = FlywheelTorqueModel.CONSTANT_TORQUE,
            ),
        )!!

        assertEquals(10000.0, result.samples.last().engineRpm, 1.0)
        assertTrue(result.runTimeToPeakSeconds > 0.0)
        assertEquals(result.peakTorqueNm, result.samples.first().torqueNm, 0.001)
    }

    @Test
    fun simulateDynoRun_constantPower_reachesPeakRpm() {
        val result = FlywheelInertiaCalculator.simulateDynoRun(
            DynoRunSimulationInput(
                totalInertiaKgm2 = 0.5,
                peakPowerPs = 50.0,
                peakEngineRpm = 10000.0,
                startRpm = 3000.0,
                torqueModel = FlywheelTorqueModel.CONSTANT_POWER,
            ),
        )!!

        assertEquals(10000.0, result.samples.last().engineRpm, 1.0)
        assertTrue(result.runTimeToPeakSeconds > 0.0)
        assertEquals(50.0, result.samples.first().powerPs, 0.1)
    }

    @Test
    fun simulateDynoRun_rejectsStartAbovePeak() {
        assertNull(
            FlywheelInertiaCalculator.simulateDynoRun(
                DynoRunSimulationInput(
                    totalInertiaKgm2 = 0.5,
                    peakPowerPs = 50.0,
                    peakEngineRpm = 5000.0,
                    startRpm = 6000.0,
                ),
            ),
        )
    }
}
