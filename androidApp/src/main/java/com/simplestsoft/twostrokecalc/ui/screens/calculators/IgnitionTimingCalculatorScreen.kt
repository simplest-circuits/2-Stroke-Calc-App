package com.simplestsoft.twostrokecalc.ui.screens.calculators

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.simplestsoft.twostrokecalc.R
import com.simplestsoft.twostrokecalc.domain.ignitiontiming.IgnitionTimingCalculator
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorBidirectionalField
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorDecimalField
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorHeader
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorPrimaryResultText
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorResultCard
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorScaffold
import com.simplestsoft.twostrokecalc.ui.components.calculator.filterDecimalInput
import com.simplestsoft.twostrokecalc.ui.components.calculator.parseDecimal
import com.simplestsoft.twostrokecalc.ui.porttiming.formatPortTimingDegrees
import com.simplestsoft.twostrokecalc.ui.porttiming.formatPortTimingMillimeters
import java.util.Locale

@Composable
fun IgnitionTimingCalculatorScreen() {
    var strokeText by rememberSaveable { mutableStateOf("57") }
    var connectingRodText by rememberSaveable { mutableStateOf("110") }
    var inputMode by rememberSaveable { mutableStateOf(IgnitionInputMode.DEGREES.name) }
    var inputText by rememberSaveable { mutableStateOf("") }

    val mode = IgnitionInputMode.entries.firstOrNull { it.name == inputMode }
        ?: IgnitionInputMode.DEGREES
    val useDegrees = mode == IgnitionInputMode.DEGREES

    val stroke = parseDecimal(strokeText)
    val connectingRod = parseDecimal(connectingRodText)
    val inputValue = parseDecimal(inputText)

    val geometryValid = stroke != null && connectingRod != null && stroke > 0.0 && connectingRod > 0.0

    val resultDegrees = when (mode) {
        IgnitionInputMode.DEGREES -> inputValue
        IgnitionInputMode.MILLIMETERS -> {
            if (geometryValid && inputValue != null) {
                IgnitionTimingCalculator.mmBeforeTdcToDegrees(stroke!!, connectingRod!!, inputValue)
            } else {
                null
            }
        }
    }

    val resultMillimeters = when (mode) {
        IgnitionInputMode.MILLIMETERS -> inputValue
        IgnitionInputMode.DEGREES -> {
            if (geometryValid && inputValue != null) {
                IgnitionTimingCalculator.degreesBeforeTdcToMm(stroke!!, connectingRod!!, inputValue)
            } else {
                null
            }
        }
    }

    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()

    val onInputModeChange: (Boolean) -> Unit = { useDegreesMode ->
        val newMode = if (useDegreesMode) IgnitionInputMode.DEGREES else IgnitionInputMode.MILLIMETERS
        val currentMode = IgnitionInputMode.entries.firstOrNull { it.name == inputMode }
            ?: IgnitionInputMode.DEGREES
        if (newMode != currentMode) {
            val currentStroke = parseDecimal(strokeText)
            val currentConnectingRod = parseDecimal(connectingRodText)
            val currentInputValue = parseDecimal(inputText)
            val currentGeometryValid = currentStroke != null &&
                currentConnectingRod != null &&
                currentStroke > 0.0 &&
                currentConnectingRod > 0.0

            val convertedText = currentInputValue?.let { value ->
                if (!currentGeometryValid) return@let null
                when {
                    currentMode == IgnitionInputMode.DEGREES && newMode == IgnitionInputMode.MILLIMETERS ->
                        IgnitionTimingCalculator.degreesBeforeTdcToMm(currentStroke!!, currentConnectingRod!!, value)
                            ?.let { String.format(locale, "%.2f", it) }
                    currentMode == IgnitionInputMode.MILLIMETERS && newMode == IgnitionInputMode.DEGREES ->
                        IgnitionTimingCalculator.mmBeforeTdcToDegrees(currentStroke!!, currentConnectingRod!!, value)
                            ?.let { String.format(locale, "%.2f", it) }
                    else -> null
                }
            }

            inputMode = newMode.name
            inputText = convertedText ?: inputText
        }
    }

    CalculatorScaffold {
        CalculatorHeader(
            title = stringResource(R.string.ignition_calculator_title),
            subtitle = stringResource(R.string.ignition_calculator_subtitle),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CalculatorDecimalField(
                value = strokeText,
                onValueChange = { strokeText = it.filterDecimalInput() },
                label = stringResource(R.string.pt_stroke),
                suffix = stringResource(R.string.pt_mm),
                modifier = Modifier.weight(1f),
            )
            CalculatorDecimalField(
                value = connectingRodText,
                onValueChange = { connectingRodText = it.filterDecimalInput() },
                label = stringResource(R.string.pt_connecting_rod),
                suffix = stringResource(R.string.pt_mm),
                modifier = Modifier.weight(1f),
            )
        }

        CalculatorBidirectionalField(
            optionALabel = stringResource(R.string.ignition_input_mode_degrees),
            optionBLabel = stringResource(R.string.ignition_input_mode_millimeters),
            useOptionA = useDegrees,
            onUseOptionAChange = onInputModeChange,
            value = inputText,
            onValueChange = { inputText = it.filterDecimalInput() },
            fieldLabelA = stringResource(R.string.ignition_degrees_label),
            fieldLabelB = stringResource(R.string.ignition_millimeters_label),
            suffixA = stringResource(R.string.ignition_unit_degrees),
            suffixB = stringResource(R.string.pt_mm),
        )

        CalculatorResultCard(title = stringResource(R.string.ignition_result_title)) {
            if (geometryValid && inputValue != null) {
                when (mode) {
                    IgnitionInputMode.DEGREES -> {
                        resultMillimeters?.let { millimeters ->
                            CalculatorPrimaryResultText(
                                text = stringResource(
                                    R.string.ignition_result_millimeters,
                                    formatPortTimingMillimeters(millimeters, locale = locale),
                                ),
                            )
                        }
                    }
                    IgnitionInputMode.MILLIMETERS -> {
                        resultDegrees?.let { degrees ->
                            CalculatorPrimaryResultText(
                                text = stringResource(
                                    R.string.ignition_result_degrees,
                                    formatPortTimingDegrees(degrees, round = true, locale = locale),
                                ),
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

private enum class IgnitionInputMode {
    DEGREES,
    MILLIMETERS,
}
