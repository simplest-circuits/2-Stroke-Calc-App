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
import com.simplestsoft.twostrokecalc.domain.calculation.VariatorWeightCalculator
import com.simplestsoft.twostrokecalc.domain.calculation.VariatorWeightCalculatorResult
import com.simplestsoft.twostrokecalc.domain.calculation.VariatorWeightRollerType
import com.simplestsoft.twostrokecalc.ui.components.AppColors
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorDecimalField
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorEmptyResultText
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorChoiceField
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorDropdownOption
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorHeader
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorInputModeSwitch
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorInvalidResultText
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorPrimaryResultText
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorResultCard
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorResultDivider
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorResultRow
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorScaffold
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorSecondaryResultText
import com.simplestsoft.twostrokecalc.ui.components.calculator.filterDecimalInput
import com.simplestsoft.twostrokecalc.ui.components.calculator.parseDecimal
import java.util.Locale
import kotlin.math.abs

private enum class TargetInputMode {
    TARGET_RPM,
    RPM_DELTA,
}

@Composable
fun VariatorWeightCalculatorScreen() {
    var currentWeightText by rememberSaveable { mutableStateOf("8,0") }
    var currentRpmText by rememberSaveable { mutableStateOf("7000") }
    var targetRpmText by rememberSaveable { mutableStateOf("7500") }
    var rpmDeltaText by rememberSaveable { mutableStateOf("500") }
    var useTargetRpm by rememberSaveable { mutableStateOf(true) }
    var rollerTypeName by rememberSaveable {
        mutableStateOf(VariatorWeightRollerType.ROLLERS.name)
    }

    val rollerType = VariatorWeightRollerType.entries.firstOrNull { it.name == rollerTypeName }
        ?: VariatorWeightRollerType.ROLLERS

    val currentWeight = parseDecimal(currentWeightText)
    val currentRpm = parseDecimal(currentRpmText)
    val targetRpmInput = parseDecimal(targetRpmText)
    val rpmDeltaInput = parseDecimal(rpmDeltaText)

    val targetRpm = when {
        !useTargetRpm && currentRpm != null && rpmDeltaInput != null ->
            VariatorWeightCalculator.targetRpmFromDelta(currentRpm, rpmDeltaInput)
        useTargetRpm -> targetRpmInput
        else -> null
    }

    val hasInput = currentWeight != null &&
        currentRpm != null &&
        targetRpm != null &&
        (useTargetRpm && targetRpmInput != null || !useTargetRpm && rpmDeltaInput != null)

    val result = if (hasInput) {
        VariatorWeightCalculator.calculate(
            currentGrams = currentWeight!!,
            currentRpm = currentRpm!!,
            targetRpm = targetRpm!!,
            rollerType = rollerType,
        )
    } else {
        null
    }

    CalculatorScaffold {
        CalculatorHeader(
            title = stringResource(R.string.variator_weight_calculator_title),
            subtitle = stringResource(R.string.variator_weight_calculator_subtitle),
        )

        CalculatorDecimalField(
            value = currentWeightText,
            onValueChange = { currentWeightText = it.filterDecimalInput() },
            label = stringResource(R.string.variator_weight_current_weight_label),
            suffix = stringResource(R.string.variator_weight_unit_grams),
            supportingText = stringResource(R.string.variator_weight_current_weight_hint),
        )

        CalculatorDecimalField(
            value = currentRpmText,
            onValueChange = { currentRpmText = it.filterDecimalInput() },
            label = stringResource(R.string.variator_weight_current_rpm_label),
            suffix = stringResource(R.string.variator_weight_unit_rpm),
            supportingText = stringResource(R.string.variator_weight_current_rpm_hint),
        )

        Text(
            text = stringResource(R.string.variator_weight_target_section_label),
            style = MaterialTheme.typography.titleSmall,
            color = AppColors.textPrimary(),
        )

        CalculatorInputModeSwitch(
            optionALabel = stringResource(R.string.variator_weight_target_mode_rpm),
            optionBLabel = stringResource(R.string.variator_weight_target_mode_delta),
            useOptionA = useTargetRpm,
            onUseOptionAChange = { useTargetRpm = it },
        )

        if (useTargetRpm) {
            CalculatorDecimalField(
                value = targetRpmText,
                onValueChange = { targetRpmText = it.filterDecimalInput() },
                label = stringResource(R.string.variator_weight_target_rpm_label),
                suffix = stringResource(R.string.variator_weight_unit_rpm),
            )
        } else {
            CalculatorDecimalField(
                value = rpmDeltaText,
                onValueChange = { rpmDeltaText = it.filterDecimalInput() },
                label = stringResource(R.string.variator_weight_rpm_delta_label),
                suffix = stringResource(R.string.variator_weight_unit_rpm),
                supportingText = stringResource(R.string.variator_weight_rpm_delta_hint),
            )
        }

        val rollerTypeOptions = VariatorWeightRollerType.entries.map { type ->
            CalculatorDropdownOption(type.name, stringResource(type.labelRes()))
        }
        CalculatorChoiceField(
            label = stringResource(R.string.variator_weight_roller_type_label),
            options = rollerTypeOptions,
            selectedKey = rollerType.name,
            onOptionSelected = { rollerTypeName = it },
            supportingText = stringResource(R.string.variator_weight_rule_hint),
        )

        VariatorWeightResultCard(
            result = result,
            currentWeight = currentWeight,
            hasInput = hasInput,
        )

        Text(
            text = stringResource(R.string.variator_weight_disclaimer),
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.textSecondary(),
        )

        Text(
            text = stringResource(R.string.variator_weight_tuning_tip),
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.textSecondary(),
        )

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun VariatorWeightResultCard(
    result: VariatorWeightCalculatorResult?,
    currentWeight: Double?,
    hasInput: Boolean,
) {
    CalculatorResultCard(title = stringResource(R.string.variator_weight_result_title)) {
        when {
            !hasInput -> CalculatorEmptyResultText(stringResource(R.string.variator_weight_result_empty))
            result == null -> CalculatorInvalidResultText(stringResource(R.string.variator_weight_result_invalid))
            else -> {
                val physicsRounded = VariatorWeightCalculator.roundToTenth(result.physicsWeightGrams)
                val empiricalRounded = VariatorWeightCalculator.roundToTenth(result.empiricalWeightGrams)

                CalculatorPrimaryResultText(
                    text = stringResource(
                        R.string.variator_weight_result_physics,
                        formatGrams(physicsRounded),
                    ),
                )
                if (currentWeight != null) {
                    CalculatorSecondaryResultText(
                        text = formatWeightChange(
                            currentWeight = currentWeight,
                            newWeight = physicsRounded,
                        ),
                    )
                }

                CalculatorResultDivider()

                CalculatorResultRow(
                    label = stringResource(R.string.variator_weight_result_empirical_label),
                    value = stringResource(
                        R.string.variator_weight_result_empirical_value,
                        formatGrams(empiricalRounded),
                    ),
                )
                if (currentWeight != null) {
                    CalculatorResultRow(
                        label = stringResource(R.string.variator_weight_result_change_label),
                        value = formatWeightChange(
                            currentWeight = currentWeight,
                            newWeight = empiricalRounded,
                        ),
                    )
                }

                CalculatorResultDivider()

                CalculatorResultRow(
                    label = stringResource(R.string.variator_weight_result_target_rpm_label),
                    value = formatRpm(result.targetRpm),
                )
                CalculatorResultRow(
                    label = stringResource(R.string.variator_weight_result_rpm_delta_label),
                    value = formatSignedRpm(result.rpmDelta),
                )
            }
        }
    }
}

private fun VariatorWeightRollerType.labelRes(): Int = when (this) {
    VariatorWeightRollerType.ROLLERS -> R.string.variator_weight_roller_type_rollers
    VariatorWeightRollerType.SLIDERS -> R.string.variator_weight_roller_type_sliders
}

@Composable
private fun formatWeightChange(currentWeight: Double, newWeight: Double): String {
    val delta = VariatorWeightCalculator.roundToTenth(newWeight - currentWeight)
    return when {
        delta < 0.0 -> stringResource(
            R.string.variator_weight_result_lighter,
            formatGrams(abs(delta)),
        )
        delta > 0.0 -> stringResource(
            R.string.variator_weight_result_heavier,
            formatGrams(delta),
        )
        else -> stringResource(R.string.variator_weight_result_unchanged)
    }
}

private fun formatGrams(value: Double): String =
    String.format(Locale.getDefault(), "%.1f", value)

private fun formatRpm(value: Double): String =
    String.format(Locale.getDefault(), "%.0f", value)

private fun formatSignedRpm(value: Double): String {
    val formatted = String.format(Locale.getDefault(), "%.0f", abs(value))
    return when {
        value > 0.0 -> "+$formatted"
        value < 0.0 -> "-$formatted"
        else -> "0"
    }
}
