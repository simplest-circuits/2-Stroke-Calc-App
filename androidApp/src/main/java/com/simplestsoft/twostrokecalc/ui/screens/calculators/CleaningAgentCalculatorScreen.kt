package com.simplestsoft.twostrokecalc.ui.screens.calculators

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.simplestsoft.twostrokecalc.R
import com.simplestsoft.twostrokecalc.domain.calculation.CleaningAgentCalculator
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorResultCard
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorResultRow
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorSection
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorSliderField
import java.util.Locale

@Composable
fun CleaningAgentCalculatorContent() {
    var totalLiters by rememberSaveable {
        mutableFloatStateOf(CleaningAgentCalculator.DEFAULT_TOTAL_LITERS.toFloat())
    }
    var concentrationPercent by rememberSaveable {
        mutableFloatStateOf(CleaningAgentCalculator.DEFAULT_CONCENTRATION_PERCENT.toFloat())
    }

    val result = CleaningAgentCalculator.calculate(
        CleaningAgentCalculator.Input(
            totalLiters = totalLiters.toDouble(),
            concentrationPercent = concentrationPercent.toDouble(),
        ),
    )

    CalculatorSection(title = "") {
        CalculatorSliderField(
            label = stringResource(R.string.cleaning_agent_total_volume_label),
            value = totalLiters,
            onValueChange = { totalLiters = it },
            valueRange = CleaningAgentCalculator.MIN_TOTAL_LITERS.toFloat()..
                CleaningAgentCalculator.MAX_TOTAL_LITERS.toFloat(),
            steps = CleaningAgentCalculator.totalVolumeSliderSteps,
            valueDisplay = stringResource(
                R.string.cleaning_agent_total_volume_value,
                formatLiters(totalLiters.toDouble()),
            ),
        )

        CalculatorSliderField(
            label = stringResource(R.string.cleaning_agent_concentration_label),
            value = concentrationPercent,
            onValueChange = { concentrationPercent = it },
            valueRange = CleaningAgentCalculator.MIN_CONCENTRATION_PERCENT.toFloat()..
                CleaningAgentCalculator.MAX_CONCENTRATION_PERCENT.toFloat(),
            steps = CleaningAgentCalculator.concentrationSliderSteps,
            valueDisplay = stringResource(
                R.string.cleaning_agent_concentration_value,
                formatPercent(concentrationPercent.toDouble()),
            ),
        )
    }

    CalculatorResultCard(title = stringResource(R.string.cleaning_agent_result_title)) {
        if (result == null) return@CalculatorResultCard

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CalculatorResultRow(
                label = stringResource(R.string.cleaning_agent_result_agent_label),
                value = stringResource(
                    R.string.cleaning_agent_result_agent_value,
                    formatMilliliters(result.cleaningAgentMl),
                    formatPercent(result.concentrationPercent),
                ),
            )

            CalculatorResultRow(
                label = stringResource(R.string.cleaning_agent_result_water_label),
                value = stringResource(
                    R.string.cleaning_agent_result_water_value,
                    formatLiters(result.waterLiters),
                ),
            )
        }
    }

    Spacer(Modifier.height(8.dp))
}

private fun formatLiters(value: Double): String =
    String.format(Locale.getDefault(), "%.1f", value)

private fun formatMilliliters(value: Double): String =
    if (value >= 100.0) {
        String.format(Locale.getDefault(), "%.0f", value)
    } else {
        String.format(Locale.getDefault(), "%.1f", value)
    }

private fun formatPercent(value: Double): String =
    String.format(Locale.getDefault(), "%.1f", value)
