package com.simplestsoft.twostrokecalc.ui.screens.calculators

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.simplestsoft.twostrokecalc.R
import com.simplestsoft.twostrokecalc.domain.calculation.SquishBandCalculationMode
import com.simplestsoft.twostrokecalc.domain.calculation.SquishBandCalculator
import com.simplestsoft.twostrokecalc.domain.calculation.SquishBandResult
import com.simplestsoft.twostrokecalc.domain.model.CalculatorId
import com.simplestsoft.twostrokecalc.ui.components.AppColors
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorDecimalField
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorEmptyResultText
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorChoiceField
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorDropdownOption
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorHeader
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorInvalidResultText
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorPrimaryResultText
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorResultCard
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorResultDivider
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorResultRow
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorScaffold
import com.simplestsoft.twostrokecalc.ui.components.calculator.filterDecimalInput
import com.simplestsoft.twostrokecalc.ui.components.calculator.parseDecimal
import java.util.Locale

@Composable
fun SquishBandCalculatorScreen(
    onNavigateToCalculator: (CalculatorId) -> Unit = {},
) {
    var mode by rememberSaveable { mutableStateOf(SquishBandCalculationMode.PERCENT.name) }
    var boreText by rememberSaveable { mutableStateOf("72") }
    var squishAreaPercentText by rememberSaveable { mutableStateOf("50") }
    var squishBandWidthText by rememberSaveable { mutableStateOf("11") }

    val selectedMode = SquishBandCalculationMode.entries.firstOrNull { it.name == mode }
        ?: SquishBandCalculationMode.PERCENT

    val bore = parseDecimal(boreText)
    val squishAreaPercent = parseDecimal(squishAreaPercentText)
    val squishBandWidth = parseDecimal(squishBandWidthText)

    val hasInput = when (selectedMode) {
        SquishBandCalculationMode.PERCENT -> bore != null && squishAreaPercent != null
        SquishBandCalculationMode.WIDTH -> bore != null && squishBandWidth != null
    }

    val result = when (selectedMode) {
        SquishBandCalculationMode.PERCENT -> if (hasInput) {
            SquishBandCalculator.calculateFromPercent(
                boreMm = bore!!,
                squishAreaPercent = squishAreaPercent!!,
            )
        } else {
            null
        }
        SquishBandCalculationMode.WIDTH -> if (hasInput) {
            SquishBandCalculator.calculateFromWidth(
                boreMm = bore!!,
                squishBandWidthMm = squishBandWidth!!,
            )
        } else {
            null
        }
    }

    CalculatorScaffold {
        CalculatorHeader(
            title = stringResource(R.string.squish_band_calculator_title),
            subtitle = when (selectedMode) {
                SquishBandCalculationMode.PERCENT ->
                    stringResource(R.string.squish_band_subtitle_percent)
                SquishBandCalculationMode.WIDTH ->
                    stringResource(R.string.squish_band_subtitle_width)
            },
        )

        val modeOptions = SquishBandCalculationMode.entries.map { entry ->
            CalculatorDropdownOption(entry.name, stringResource(entry.labelRes()))
        }
        CalculatorChoiceField(
            label = "",
            options = modeOptions,
            selectedKey = selectedMode.name,
            onOptionSelected = { mode = it },
        )

        CalculatorDecimalField(
            value = boreText,
            onValueChange = { boreText = it.filterDecimalInput() },
            label = stringResource(R.string.squish_band_bore_label),
            suffix = stringResource(R.string.squish_band_unit_mm),
        )

        when (selectedMode) {
            SquishBandCalculationMode.PERCENT -> {
                CalculatorDecimalField(
                    value = squishAreaPercentText,
                    onValueChange = { squishAreaPercentText = it.filterDecimalInput() },
                    label = stringResource(R.string.squish_band_percent_label),
                    suffix = stringResource(R.string.squish_band_unit_percent),
                    supportingText = stringResource(R.string.squish_band_percent_hint),
                )
                Text(
                    text = stringResource(R.string.squish_band_formula_hint_percent),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.textSecondary(),
                )
            }
            SquishBandCalculationMode.WIDTH -> {
                CalculatorDecimalField(
                    value = squishBandWidthText,
                    onValueChange = { squishBandWidthText = it.filterDecimalInput() },
                    label = stringResource(R.string.squish_band_width_label),
                    suffix = stringResource(R.string.squish_band_unit_mm),
                    supportingText = stringResource(R.string.squish_band_width_hint),
                )
                Text(
                    text = stringResource(R.string.squish_band_formula_hint_width),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.textSecondary(),
                )
            }
        }

        SquishBandResultCard(
            result = result,
            hasInput = hasInput,
            mode = selectedMode,
        )

        Text(
            text = stringResource(R.string.calculator_link_hint_compression_squish),
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.textSecondary(),
        )
        TextButton(onClick = { onNavigateToCalculator(CalculatorId.COMPRESSION) }) {
            Text(stringResource(R.string.calculator_link_compression))
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun SquishBandResultCard(
    result: SquishBandResult?,
    hasInput: Boolean,
    mode: SquishBandCalculationMode,
) {
    CalculatorResultCard(title = stringResource(R.string.squish_band_result_title)) {
        when {
            !hasInput -> CalculatorEmptyResultText(
                when (mode) {
                    SquishBandCalculationMode.PERCENT ->
                        stringResource(R.string.squish_band_result_empty_percent)
                    SquishBandCalculationMode.WIDTH ->
                        stringResource(R.string.squish_band_result_empty_width)
                },
            )
            result == null -> CalculatorInvalidResultText(
                when (mode) {
                    SquishBandCalculationMode.PERCENT ->
                        stringResource(R.string.squish_band_result_invalid_percent)
                    SquishBandCalculationMode.WIDTH ->
                        stringResource(R.string.squish_band_result_invalid_width)
                },
            )
            else -> when (mode) {
                SquishBandCalculationMode.PERCENT -> PercentModeResults(result = result)
                SquishBandCalculationMode.WIDTH -> WidthModeResults(result = result)
            }
        }
    }
}

@Composable
private fun PercentModeResults(result: SquishBandResult) {
    CalculatorPrimaryResultText(
        text = stringResource(
            R.string.squish_band_result_dome,
            formatDecimal(result.domeDiameterMm, 2),
        ),
    )
    CalculatorResultDivider()
    CalculatorResultRow(
        label = stringResource(R.string.squish_band_result_width),
        value = stringResource(
            R.string.squish_band_result_value_mm,
            formatDecimal(result.squishBandWidthMm, 2),
        ),
    )
}

@Composable
private fun WidthModeResults(result: SquishBandResult) {
    CalculatorPrimaryResultText(
        text = stringResource(
            R.string.squish_band_result_area,
            formatDecimal(result.squishAreaPercent, 1),
        ),
    )
    CalculatorResultDivider()
    CalculatorResultRow(
        label = stringResource(R.string.squish_band_result_dome_label),
        value = stringResource(
            R.string.squish_band_result_value_mm,
            formatDecimal(result.domeDiameterMm, 2),
        ),
    )
}

private fun SquishBandCalculationMode.labelRes(): Int = when (this) {
    SquishBandCalculationMode.PERCENT -> R.string.squish_band_mode_percent
    SquishBandCalculationMode.WIDTH -> R.string.squish_band_mode_width
}

private fun formatDecimal(value: Double, decimals: Int): String =
    String.format(Locale.getDefault(), "%.${decimals}f", value)
