package com.simplestsoft.twostrokecalc.ui.screens.calculators

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.simplestsoft.twostrokecalc.R
import com.simplestsoft.twostrokecalc.domain.calculation.MeanPressureCalculator
import com.simplestsoft.twostrokecalc.domain.calculation.MeanPressureResult
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorDecimalField
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorEmptyResultText
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
fun MeanPressureCalculatorScreen() {
    var powerPsText by rememberSaveable { mutableStateOf("100") }
    var rpmText by rememberSaveable { mutableStateOf("10500") }
    var boreText by rememberSaveable { mutableStateOf("90") }
    var strokeText by rememberSaveable { mutableStateOf("64") }

    val powerPs = parseDecimal(powerPsText)
    val rpm = parseDecimal(rpmText)
    val bore = parseDecimal(boreText)
    val stroke = parseDecimal(strokeText)

    val hasInput = powerPs != null && rpm != null && bore != null && stroke != null

    val result = if (hasInput) {
        MeanPressureCalculator.calculate(
            powerPs = powerPs!!,
            rpm = rpm!!,
            boreMm = bore!!,
            strokeMm = stroke!!,
        )
    } else {
        null
    }

    CalculatorScaffold {
        CalculatorHeader(
            title = stringResource(R.string.mean_pressure_calculator_title),
            subtitle = stringResource(R.string.mean_pressure_calculator_subtitle),
        )

        CalculatorDecimalField(
            value = powerPsText,
            onValueChange = { powerPsText = it.filterDecimalInput() },
            label = stringResource(R.string.mean_pressure_power_label),
            suffix = stringResource(R.string.mean_pressure_unit_ps),
        )

        CalculatorDecimalField(
            value = rpmText,
            onValueChange = { rpmText = it.filterDecimalInput() },
            label = stringResource(R.string.mean_pressure_rpm_label),
            suffix = stringResource(R.string.mean_pressure_unit_rpm),
        )

        CalculatorDecimalField(
            value = boreText,
            onValueChange = { boreText = it.filterDecimalInput() },
            label = stringResource(R.string.mean_pressure_bore_label),
            suffix = stringResource(R.string.mean_pressure_unit_mm),
        )

        CalculatorDecimalField(
            value = strokeText,
            onValueChange = { strokeText = it.filterDecimalInput() },
            label = stringResource(R.string.mean_pressure_stroke_label),
            suffix = stringResource(R.string.mean_pressure_unit_mm),
        )

        MeanPressureResultCard(result = result, hasInput = hasInput)

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun MeanPressureResultCard(
    result: MeanPressureResult?,
    hasInput: Boolean,
) {
    CalculatorResultCard(title = stringResource(R.string.mean_pressure_result_title)) {
        when {
            !hasInput -> CalculatorEmptyResultText(stringResource(R.string.mean_pressure_result_empty))
            result == null -> CalculatorInvalidResultText(stringResource(R.string.mean_pressure_result_invalid))
            else -> {
                CalculatorPrimaryResultText(
                    text = stringResource(
                        R.string.mean_pressure_result_bar,
                        formatDecimal(result.meanPressureBar, 2),
                    ),
                )
                CalculatorResultDivider()
                CalculatorResultRow(
                    label = stringResource(R.string.mean_pressure_result_power_watts),
                    value = stringResource(
                        R.string.mean_pressure_result_value_watts,
                        formatDecimal(result.powerWatts, 0),
                    ),
                )
                CalculatorResultRow(
                    label = stringResource(R.string.mean_pressure_result_torque),
                    value = stringResource(
                        R.string.mean_pressure_result_value_nm,
                        formatDecimal(result.torqueNm, 2),
                    ),
                )
                CalculatorResultRow(
                    label = stringResource(R.string.mean_pressure_result_displacement),
                    value = stringResource(
                        R.string.mean_pressure_result_value_ccm,
                        formatDecimal(result.displacementCcm, 2),
                    ),
                )
                CalculatorResultRow(
                    label = stringResource(R.string.mean_pressure_result_specific_power),
                    value = stringResource(
                        R.string.mean_pressure_result_value_ps_per_l,
                        formatDecimal(result.specificPowerPsPerLiter, 1),
                    ),
                )
            }
        }
    }
}

private fun formatDecimal(value: Double, decimals: Int): String =
    String.format(Locale.getDefault(), "%.${decimals}f", value)
