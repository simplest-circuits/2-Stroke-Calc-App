package com.simplestsoft.twostrokecalc.ui.screens.calculators

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.simplestsoft.twostrokecalc.R
import com.simplestsoft.twostrokecalc.domain.calculation.CarbJetCorrectionCalculator
import com.simplestsoft.twostrokecalc.domain.calculation.CarbJetCorrectionResult
import com.simplestsoft.twostrokecalc.ui.components.AppColors
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorDecimalField
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorEmptyResultText
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorHeader
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorInvalidResultText
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorPrimaryResultText
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorResultCard
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorResultDivider
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorResultRow
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorScaffold
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorSection
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorSecondaryResultText
import com.simplestsoft.twostrokecalc.ui.components.calculator.filterDecimalInput
import com.simplestsoft.twostrokecalc.ui.components.calculator.parseDecimal
import java.util.Locale

@Composable
fun CarbJetCorrectionCalculatorScreen() {
    var baseJetText by rememberSaveable { mutableStateOf("165") }
    var referenceAltitudeText by rememberSaveable { mutableStateOf("0") }
    var referenceTemperatureText by rememberSaveable { mutableStateOf("15") }
    var targetAltitudeText by rememberSaveable { mutableStateOf("500") }
    var targetTemperatureText by rememberSaveable { mutableStateOf("20") }

    val baseJet = parsePositiveInteger(baseJetText)
    val referenceAltitude = parseDecimal(referenceAltitudeText)
    val referenceTemperature = parseDecimal(referenceTemperatureText)
    val targetAltitude = parseDecimal(targetAltitudeText)
    val targetTemperature = parseDecimal(targetTemperatureText)

    val hasInput = baseJet != null &&
        referenceAltitude != null &&
        referenceTemperature != null &&
        targetAltitude != null &&
        targetTemperature != null

    val result = if (hasInput) {
        CarbJetCorrectionCalculator.calculate(
            baseMainJet = baseJet!!,
            referenceAltitudeM = referenceAltitude!!,
            referenceTemperatureC = referenceTemperature!!,
            targetAltitudeM = targetAltitude!!,
            targetTemperatureC = targetTemperature!!,
        )
    } else {
        null
    }

    CalculatorScaffold {
        CalculatorHeader(
            title = stringResource(R.string.carb_jet_calculator_title),
            subtitle = stringResource(R.string.carb_jet_calculator_subtitle),
        )

        CalculatorSection(title = stringResource(R.string.carb_jet_section_reference)) {
            CalculatorDecimalField(
                value = baseJetText,
                onValueChange = { baseJetText = it.filterIntegerInput() },
                label = stringResource(R.string.carb_jet_base_main_jet_label),
                supportingText = stringResource(R.string.carb_jet_base_main_jet_hint),
            )
            CalculatorDecimalField(
                value = referenceAltitudeText,
                onValueChange = { referenceAltitudeText = it.filterDecimalInput() },
                label = stringResource(R.string.carb_jet_reference_altitude_label),
                suffix = stringResource(R.string.carb_jet_unit_m),
                supportingText = stringResource(R.string.carb_jet_reference_altitude_hint),
            )
            CalculatorDecimalField(
                value = referenceTemperatureText,
                onValueChange = { referenceTemperatureText = it.filterDecimalInput() },
                label = stringResource(R.string.carb_jet_reference_temperature_label),
                suffix = stringResource(R.string.carb_jet_unit_celsius),
            )
        }

        CalculatorSection(title = stringResource(R.string.carb_jet_section_target)) {
            CalculatorDecimalField(
                value = targetAltitudeText,
                onValueChange = { targetAltitudeText = it.filterDecimalInput() },
                label = stringResource(R.string.carb_jet_target_altitude_label),
                suffix = stringResource(R.string.carb_jet_unit_m),
            )
            CalculatorDecimalField(
                value = targetTemperatureText,
                onValueChange = { targetTemperatureText = it.filterDecimalInput() },
                label = stringResource(R.string.carb_jet_target_temperature_label),
                suffix = stringResource(R.string.carb_jet_unit_celsius),
            )
        }

        Text(
            text = stringResource(R.string.carb_jet_formula_hint),
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.textSecondary(),
        )

        CarbJetCorrectionResultCard(
            result = result,
            hasInput = hasInput,
        )

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun CarbJetCorrectionResultCard(
    result: CarbJetCorrectionResult?,
    hasInput: Boolean,
) {
    CalculatorResultCard(title = stringResource(R.string.carb_jet_result_title)) {
        when {
            !hasInput -> CalculatorEmptyResultText(stringResource(R.string.carb_jet_result_empty))
            result == null -> CalculatorInvalidResultText(stringResource(R.string.carb_jet_result_invalid))
            else -> {
                CalculatorPrimaryResultText(
                    text = stringResource(
                        R.string.carb_jet_result_main_jet,
                        result.correctedMainJet,
                    ),
                )
                CalculatorSecondaryResultText(
                    text = stringResource(
                        R.string.carb_jet_result_main_jet_exact,
                        formatDecimal(result.correctedMainJetExact, 1),
                    ),
                )
                Text(
                    text = stringResource(
                        R.string.carb_jet_result_factor,
                        formatDecimal(result.correctionFactor * 100.0, 1),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.textSecondary(),
                )
                Text(
                    text = stringResource(R.string.carb_jet_result_rounding_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.textSecondary(),
                )
                CalculatorResultDivider()
                CalculatorResultRow(
                    label = stringResource(R.string.carb_jet_result_reference_pressure),
                    value = stringResource(
                        R.string.carb_jet_result_pressure_value,
                        formatDecimal(result.referencePressureMbar, 0),
                    ),
                )
                CalculatorResultRow(
                    label = stringResource(R.string.carb_jet_result_target_pressure),
                    value = stringResource(
                        R.string.carb_jet_result_pressure_value,
                        formatDecimal(result.targetPressureMbar, 0),
                    ),
                )
            }
        }
    }
}

private fun parsePositiveInteger(text: String): Int? {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return null
    val value = trimmed.toIntOrNull() ?: return null
    return value.takeIf { it > 0 }
}

private fun String.filterIntegerInput(): String =
    filter { it.isDigit() }.take(4)

private fun formatDecimal(value: Double, decimals: Int): String =
    String.format(Locale.getDefault(), "%.${decimals}f", value)
