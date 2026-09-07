package com.simplestsoft.twostrokecalc.domain.tools.porttiming

import com.simplestsoft.twostrokecalc.domain.porttiming.IntakeSystem
import com.simplestsoft.twostrokecalc.domain.porttiming.PortTimingCalculator
import com.simplestsoft.twostrokecalc.domain.porttiming.PortTimingInput
import com.simplestsoft.twostrokecalc.domain.porttiming.PortTimingResult
import kotlinx.serialization.Serializable

enum class PortTimingAssistStep {
    IDLE,
    MARK_TDC,
    MEASURE_EXHAUST,
    MEASURE_TRANSFER,
    MEASURE_INTAKE,
    REVIEW,
}

@Serializable
data class PortTimingMeasurementDraft(
    val strokeMm: Double = 0.0,
    val connectingRodMm: Double = 0.0,
    val pistonDeckMm: Double = 0.0,
    val exhaustPortMm: Double = 0.0,
    val transferPortMm: Double = 0.0,
    val intakeOpenMm: Double = 0.0,
    val intakeCloseMm: Double = 0.0,
    /** Default reed: most scooter engines have no geometric intake duration. */
    val intakeSystem: String = IntakeSystem.REED_VALVE.name,
    val step: String = PortTimingAssistStep.IDLE.name,
)

data class PortTimingMeasurementSession(
    val draft: PortTimingMeasurementDraft = PortTimingMeasurementDraft(),
) {
    val step: PortTimingAssistStep =
        PortTimingAssistStep.entries.firstOrNull { it.name == draft.step }
            ?: PortTimingAssistStep.IDLE

    val intakeSystem: IntakeSystem =
        IntakeSystem.entries.firstOrNull { it.name == draft.intakeSystem }
            ?: IntakeSystem.REED_VALVE

    val measuresIntake: Boolean = intakeSystem == IntakeSystem.ROTARY_VALVE

    fun withStep(step: PortTimingAssistStep): PortTimingMeasurementSession =
        copy(draft = draft.copy(step = step.name))

    fun withIntakeSystem(system: IntakeSystem): PortTimingMeasurementSession {
        val updated = copy(draft = draft.copy(intakeSystem = system.name))
        // If switching to reed while on the intake step, skip ahead to review.
        return if (!updated.measuresIntake && updated.step == PortTimingAssistStep.MEASURE_INTAKE) {
            updated.withStep(PortTimingAssistStep.REVIEW)
        } else {
            updated
        }
    }

    fun nextStep(): PortTimingMeasurementSession {
        val next = when (step) {
            PortTimingAssistStep.IDLE -> PortTimingAssistStep.MARK_TDC
            PortTimingAssistStep.MARK_TDC -> PortTimingAssistStep.MEASURE_EXHAUST
            PortTimingAssistStep.MEASURE_EXHAUST -> PortTimingAssistStep.MEASURE_TRANSFER
            PortTimingAssistStep.MEASURE_TRANSFER ->
                if (measuresIntake) PortTimingAssistStep.MEASURE_INTAKE else PortTimingAssistStep.REVIEW
            PortTimingAssistStep.MEASURE_INTAKE -> PortTimingAssistStep.REVIEW
            PortTimingAssistStep.REVIEW -> PortTimingAssistStep.REVIEW
        }
        return withStep(next)
    }

    fun previousStep(): PortTimingMeasurementSession {
        val prev = when (step) {
            PortTimingAssistStep.IDLE -> PortTimingAssistStep.IDLE
            PortTimingAssistStep.MARK_TDC -> PortTimingAssistStep.IDLE
            PortTimingAssistStep.MEASURE_EXHAUST -> PortTimingAssistStep.MARK_TDC
            PortTimingAssistStep.MEASURE_TRANSFER -> PortTimingAssistStep.MEASURE_EXHAUST
            PortTimingAssistStep.MEASURE_INTAKE -> PortTimingAssistStep.MEASURE_TRANSFER
            PortTimingAssistStep.REVIEW ->
                if (measuresIntake) PortTimingAssistStep.MEASURE_INTAKE else PortTimingAssistStep.MEASURE_TRANSFER
        }
        return withStep(prev)
    }

    fun toInput(): PortTimingInput? {
        if (draft.strokeMm <= 0.0 || draft.connectingRodMm <= 0.0) return null
        return PortTimingInput(
            strokeMm = draft.strokeMm,
            connectingRodMm = draft.connectingRodMm,
            pistonDeckMm = draft.pistonDeckMm,
            exhaustPortMm = draft.exhaustPortMm,
            transferPortMm = draft.transferPortMm,
            intakeOpenMm = draft.intakeOpenMm,
            intakeCloseMm = draft.intakeCloseMm,
            intakeSystem = intakeSystem,
        )
    }

    fun calculate(): PortTimingResult? {
        val input = toInput() ?: return null
        return PortTimingCalculator.calculate(input)
    }
}
