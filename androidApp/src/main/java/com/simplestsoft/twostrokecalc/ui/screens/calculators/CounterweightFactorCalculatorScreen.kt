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
import com.simplestsoft.twostrokecalc.domain.calculation.CounterweightFactorCalculator
import com.simplestsoft.twostrokecalc.ui.components.AppColors
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorDecimalField
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorEmptyResultText
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorHeader
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorInvalidResultText
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorPrimaryResultText
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorResultCard
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorScaffold
import com.simplestsoft.twostrokecalc.ui.components.calculator.filterDecimalInput
import com.simplestsoft.twostrokecalc.ui.components.calculator.parseDecimal
import java.util.Locale

@Composable
fun CounterweightFactorCalculatorScreen() {
    var pistonWeightText by rememberSaveable { mutableStateOf("303") }
    var connectingRodHalfText by rememberSaveable { mutableStateOf("60") }
    var bigEndWeightText by rememberSaveable { mutableStateOf("71") }

    val pistonWeight = parseDecimal(pistonWeightText)
    val connectingRodHalf = parseDecimal(connectingRodHalfText)
    val bigEndWeight = parseDecimal(bigEndWeightText)

    val factorPercent = if (
        pistonWeight != null &&
        connectingRodHalf != null &&
        bigEndWeight != null
    ) {
        CounterweightFactorCalculator.factorPercent(
            pistonWeightGrams = pistonWeight,
            connectingRodHalfGrams = connectingRodHalf,
            bigEndWeightGrams = bigEndWeight,
        )
    } else {
        null
    }

    val hasInput = pistonWeight != null && connectingRodHalf != null && bigEndWeight != null

    CalculatorScaffold {
        CalculatorHeader(
            title = stringResource(R.string.counterweight_calculator_title),
            subtitle = stringResource(R.string.counterweight_calculator_subtitle),
        )

        CalculatorDecimalField(
            value = pistonWeightText,
            onValueChange = { pistonWeightText = it.filterDecimalInput() },
            label = stringResource(R.string.counterweight_piston_label),
            suffix = stringResource(R.string.counterweight_unit_grams),
        )

        CalculatorDecimalField(
            value = connectingRodHalfText,
            onValueChange = { connectingRodHalfText = it.filterDecimalInput() },
            label = stringResource(R.string.counterweight_connecting_rod_half_label),
            suffix = stringResource(R.string.counterweight_unit_grams),
            supportingText = stringResource(R.string.counterweight_connecting_rod_half_hint),
        )

        CalculatorDecimalField(
            value = bigEndWeightText,
            onValueChange = { bigEndWeightText = it.filterDecimalInput() },
            label = stringResource(R.string.counterweight_big_end_label),
            suffix = stringResource(R.string.counterweight_unit_grams),
            supportingText = stringResource(R.string.counterweight_big_end_hint),
        )

        Text(
            text = stringResource(R.string.counterweight_formula_hint),
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.textSecondary(),
        )

        CalculatorResultCard(title = stringResource(R.string.counterweight_result_title)) {
            when {
                !hasInput -> CalculatorEmptyResultText(stringResource(R.string.counterweight_result_empty))
                factorPercent == null -> CalculatorInvalidResultText(stringResource(R.string.counterweight_result_invalid))
                else -> CalculatorPrimaryResultText(
                    text = stringResource(
                        R.string.counterweight_result_factor,
                        formatPercent(factorPercent),
                    ),
                )
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

private fun formatPercent(value: Double): String =
    String.format(Locale.getDefault(), "%.2f", value)
