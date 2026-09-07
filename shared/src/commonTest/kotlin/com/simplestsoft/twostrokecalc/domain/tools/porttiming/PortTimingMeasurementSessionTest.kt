package com.simplestsoft.twostrokecalc.domain.tools.porttiming

import com.simplestsoft.twostrokecalc.domain.porttiming.IntakeSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PortTimingMeasurementSessionTest {

    @Test
    fun reedSkipsIntakeStep() {
        var session = PortTimingMeasurementSession(
            PortTimingMeasurementDraft(
                strokeMm = 54.0,
                connectingRodMm = 105.0,
                intakeSystem = IntakeSystem.REED_VALVE.name,
                step = PortTimingAssistStep.MEASURE_TRANSFER.name,
            ),
        )
        assertFalse(session.measuresIntake)
        session = session.nextStep()
        assertEquals(PortTimingAssistStep.REVIEW, session.step)
        session = session.previousStep()
        assertEquals(PortTimingAssistStep.MEASURE_TRANSFER, session.step)
    }

    @Test
    fun rotaryIncludesIntakeStep() {
        var session = PortTimingMeasurementSession(
            PortTimingMeasurementDraft(
                strokeMm = 54.0,
                connectingRodMm = 105.0,
                intakeSystem = IntakeSystem.ROTARY_VALVE.name,
                step = PortTimingAssistStep.MEASURE_TRANSFER.name,
            ),
        )
        assertTrue(session.measuresIntake)
        session = session.nextStep()
        assertEquals(PortTimingAssistStep.MEASURE_INTAKE, session.step)
        session = session.nextStep()
        assertEquals(PortTimingAssistStep.REVIEW, session.step)
        session = session.previousStep()
        assertEquals(PortTimingAssistStep.MEASURE_INTAKE, session.step)
    }

    @Test
    fun reedResultHasNoIntakeTiming() {
        val session = PortTimingMeasurementSession(
            PortTimingMeasurementDraft(
                strokeMm = 57.0,
                connectingRodMm = 110.0,
                exhaustPortMm = 36.2,
                transferPortMm = 47.5,
                intakeSystem = IntakeSystem.REED_VALVE.name,
            ),
        )
        val result = session.calculate()
        assertTrue(result != null)
        assertNull(result!!.intake)
        assertTrue(result.exhaust != null)
        assertTrue(result.transfer != null)
    }
}
