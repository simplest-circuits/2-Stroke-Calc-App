package com.simplestsoft.twostrokecalc.ui.porttiming

import androidx.lifecycle.ViewModel
import com.simplestsoft.twostrokecalc.data.session.CalculatorSession
import com.simplestsoft.twostrokecalc.domain.porttiming.IntakeSystem
import com.simplestsoft.twostrokecalc.domain.porttiming.PortTimingCalculator
import com.simplestsoft.twostrokecalc.domain.porttiming.PortTimingInput
import com.simplestsoft.twostrokecalc.domain.porttiming.PortTimingResult
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class PortTimingUiState(
    val stroke: String = "57",
    val connectingRod: String = "110",
    val exhaustPort: String = "36.2",
    val transferPort: String = "47.5",
    val intakeOpen: String = "44.2",
    val intakeClose: String = "15.9",
    val pistonDeck: String = "-0.5",
    val intakeSystem: IntakeSystem = IntakeSystem.ROTARY_VALVE,
    val roundResults: Boolean = false,
    val result: PortTimingResult? = null,
    val lastInput: PortTimingInput? = null,
    val errorMessage: String? = null,
)

@HiltViewModel
class PortTimingViewModel @Inject constructor(
    private val calculatorSession: CalculatorSession,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PortTimingUiState())
    val uiState = _uiState.asStateFlow()

    init {
        recalculate()
    }

    fun onStrokeChange(value: String) {
        _uiState.update { it.copy(stroke = value) }
        recalculate()
    }

    fun onConnectingRodChange(value: String) {
        _uiState.update { it.copy(connectingRod = value) }
        recalculate()
    }

    fun onExhaustPortChange(value: String) {
        _uiState.update { it.copy(exhaustPort = value) }
        recalculate()
    }

    fun onTransferPortChange(value: String) {
        _uiState.update { it.copy(transferPort = value) }
        recalculate()
    }

    fun onIntakeOpenChange(value: String) {
        _uiState.update { it.copy(intakeOpen = value) }
        recalculate()
    }

    fun onIntakeCloseChange(value: String) {
        _uiState.update { it.copy(intakeClose = value) }
        recalculate()
    }

    fun onPistonDeckChange(value: String) {
        _uiState.update { it.copy(pistonDeck = value) }
        recalculate()
    }

    fun onIntakeSystemChange(system: IntakeSystem) {
        _uiState.update { it.copy(intakeSystem = system) }
        recalculate()
    }

    fun onRoundResultsChange(checked: Boolean) {
        _uiState.update { it.copy(roundResults = checked) }
        recalculate()
    }

    private fun recalculate() {
        val state = _uiState.value
        val stroke = parseNumber(state.stroke)
        val connectingRod = parseNumber(state.connectingRod)
        val exhaustPort = parseNumber(state.exhaustPort)
        val transferPort = parseNumber(state.transferPort)
        val intakeOpen = parseNumber(state.intakeOpen)
        val intakeClose = parseNumber(state.intakeClose)
        val pistonDeck = parseNumber(state.pistonDeck)

        if (stroke == null || connectingRod == null || exhaustPort == null ||
            transferPort == null || intakeOpen == null || intakeClose == null || pistonDeck == null
        ) {
            _uiState.update {
                it.copy(
                    result = null,
                    lastInput = null,
                    errorMessage = null,
                )
            }
            return
        }

        if (stroke <= 0.0 || connectingRod <= 0.0) {
            _uiState.update {
                it.copy(
                    result = null,
                    lastInput = null,
                    errorMessage = "invalid_input",
                )
            }
            return
        }

        val input = PortTimingInput(
            strokeMm = stroke,
            connectingRodMm = connectingRod,
            exhaustPortMm = exhaustPort,
            transferPortMm = transferPort,
            intakeOpenMm = intakeOpen,
            intakeCloseMm = intakeClose,
            pistonDeckMm = pistonDeck,
            intakeSystem = state.intakeSystem,
            roundResults = state.roundResults,
        )

        val result = PortTimingCalculator.calculate(input)
        if (result.exhaust == null || result.transfer == null) {
            _uiState.update {
                it.copy(
                    errorMessage = "out_of_range",
                    lastInput = input,
                    result = null,
                )
            }
            return
        }

        if (input.intakeSystem == IntakeSystem.ROTARY_VALVE && result.intake == null) {
            _uiState.update {
                it.copy(
                    errorMessage = "out_of_range",
                    lastInput = input,
                    result = null,
                )
            }
            return
        }

        calculatorSession.updateFromPortTiming(
            strokeMm = stroke,
            transferDurationDeg = result.transfer!!.duration,
            exhaustDurationDeg = result.exhaust!!.duration,
        )

        _uiState.update {
            it.copy(
                result = result,
                lastInput = input,
                errorMessage = null,
            )
        }
    }

    private fun parseNumber(raw: String): Double? {
        val normalized = raw.trim().replace(',', '.')
        if (normalized.isEmpty()) return null
        return normalized.toDoubleOrNull()
    }
}

fun formatPortTimingDegrees(value: Double, round: Boolean, locale: Locale = Locale.GERMAN): String {
    return if (round) {
        "${kotlin.math.round(value).toInt()}°"
    } else {
        String.format(locale, "%.2f°", value)
    }
}

fun formatPortTimingMillimeters(value: Double, locale: Locale = Locale.GERMAN): String {
    return String.format(locale, "%.2f mm", value)
}
