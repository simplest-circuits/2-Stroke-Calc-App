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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.simplestsoft.twostrokecalc.R
import com.simplestsoft.twostrokecalc.domain.calculation.DcCableCrossSectionCalculator
import com.simplestsoft.twostrokecalc.ui.components.AppColors
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorBidirectionalField
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorCollapsibleSection
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorDecimalField
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorEmptyResultText
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorChoiceField
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorDropdownOption
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorHeader
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorPrimaryResultText
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorResultCard
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorResultDivider
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorResultRow
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorScaffold
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorSecondaryResultText
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorSection
import com.simplestsoft.twostrokecalc.ui.components.calculator.filterDecimalInput
import com.simplestsoft.twostrokecalc.ui.components.calculator.parsePositiveDecimal
import java.util.Locale

private enum class DcCableLoadInputMode {
    CURRENT,
    POWER,
}

@Composable
fun DcCableCrossSectionCalculatorScreen() {
    var voltagePresetName by rememberSaveable { mutableStateOf(DcCableVoltagePreset.TWELVE.name) }
    var inputModeName by rememberSaveable { mutableStateOf(DcCableLoadInputMode.CURRENT.name) }
    var loadInputText by rememberSaveable { mutableStateOf("10") }
    var lengthText by rememberSaveable { mutableStateOf("3") }
    var dropPresetName by rememberSaveable { mutableStateOf(DcCableDropPreset.THREE.name) }

    val voltagePreset = DcCableVoltagePreset.entries.firstOrNull { it.name == voltagePresetName }
        ?: DcCableVoltagePreset.TWELVE
    val voltage = voltagePreset.value.toDouble()

    val dropPreset = DcCableDropPreset.entries.firstOrNull { it.name == dropPresetName }
        ?: DcCableDropPreset.THREE
    val dropPercent = dropPreset.value.toDouble()

    val inputMode = DcCableLoadInputMode.entries.firstOrNull { it.name == inputModeName }
        ?: DcCableLoadInputMode.CURRENT
    val useCurrent = inputMode == DcCableLoadInputMode.CURRENT

    val loadInput = parsePositiveDecimal(loadInputText)
    val lengthM = parsePositiveDecimal(lengthText)

    val currentAmps = when (inputMode) {
        DcCableLoadInputMode.CURRENT -> loadInput
        DcCableLoadInputMode.POWER -> {
            if (loadInput != null) {
                DcCableCrossSectionCalculator.currentFromPower(loadInput, voltage)
            } else {
                null
            }
        }
    }

    val result = if (currentAmps != null && lengthM != null) {
        DcCableCrossSectionCalculator.calculate(
            voltage = voltage,
            currentAmps = currentAmps,
            oneWayLengthM = lengthM,
            maxDropPercent = dropPercent,
        )
    } else {
        null
    }

    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()

    val onInputModeChange: (Boolean) -> Unit = { useCurrentMode ->
        val newMode = if (useCurrentMode) DcCableLoadInputMode.CURRENT else DcCableLoadInputMode.POWER
        val currentMode = DcCableLoadInputMode.entries.firstOrNull { it.name == inputModeName }
            ?: DcCableLoadInputMode.CURRENT
        if (newMode != currentMode) {
            val currentLoadInput = parsePositiveDecimal(loadInputText)
            val convertedText = currentLoadInput?.let { value ->
                when {
                    currentMode == DcCableLoadInputMode.CURRENT && newMode == DcCableLoadInputMode.POWER ->
                        formatDcCableAmount(value * voltage, locale, 1)
                    currentMode == DcCableLoadInputMode.POWER && newMode == DcCableLoadInputMode.CURRENT ->
                        DcCableCrossSectionCalculator.currentFromPower(value, voltage)
                            ?.let { formatDcCableAmount(it, locale, 2) }
                    else -> null
                }
            }
            inputModeName = newMode.name
            loadInputText = convertedText ?: loadInputText
        }
    }

    CalculatorScaffold {
        CalculatorHeader(
            title = stringResource(R.string.dc_cable_calculator_title),
            subtitle = stringResource(R.string.dc_cable_calculator_subtitle),
        )

        CalculatorSection(title = stringResource(R.string.dc_cable_voltage_label)) {
            val voltageOptions = DcCableVoltagePreset.entries.map { preset ->
                CalculatorDropdownOption(preset.name, stringResource(preset.labelRes))
            }
            CalculatorChoiceField(
                label = "",
                options = voltageOptions,
                selectedKey = voltagePreset.name,
                onOptionSelected = { voltagePresetName = it },
                supportingText = stringResource(R.string.dc_cable_voltage_hint),
            )
        }

        CalculatorSection(title = stringResource(R.string.dc_cable_section_load)) {
            CalculatorBidirectionalField(
                optionALabel = stringResource(R.string.dc_cable_input_mode_current),
                optionBLabel = stringResource(R.string.dc_cable_input_mode_power),
                useOptionA = useCurrent,
                onUseOptionAChange = onInputModeChange,
                value = loadInputText,
                onValueChange = { loadInputText = it.filterDecimalInput() },
                fieldLabelA = stringResource(R.string.dc_cable_current_label),
                fieldLabelB = stringResource(R.string.dc_cable_power_label),
                suffixA = stringResource(R.string.dc_cable_unit_amps),
                suffixB = stringResource(R.string.dc_cable_unit_watts),
            )
        }

        CalculatorSection(title = stringResource(R.string.dc_cable_section_cable)) {
            CalculatorDecimalField(
                value = lengthText,
                onValueChange = { lengthText = it.filterDecimalInput() },
                label = stringResource(R.string.dc_cable_length_label),
                suffix = stringResource(R.string.dc_cable_unit_meters),
                supportingText = stringResource(R.string.dc_cable_length_hint),
            )
        }

        CalculatorSection(title = stringResource(R.string.dc_cable_drop_label)) {
            val dropOptions = DcCableDropPreset.entries.map { preset ->
                CalculatorDropdownOption(preset.name, stringResource(preset.labelRes))
            }
            CalculatorChoiceField(
                label = "",
                options = dropOptions,
                selectedKey = dropPreset.name,
                onOptionSelected = { dropPresetName = it },
                supportingText = stringResource(R.string.dc_cable_drop_hint),
            )
        }

        CalculatorCollapsibleSection(title = stringResource(R.string.dc_cable_section_help)) {
            Text(
                text = stringResource(R.string.dc_cable_formula_hint),
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.textSecondary(),
            )
        }

        CalculatorResultCard(title = stringResource(R.string.dc_cable_result_title)) {
            if (result == null) {
                CalculatorEmptyResultText(stringResource(R.string.dc_cable_result_empty))
                return@CalculatorResultCard
            }

            CalculatorPrimaryResultText(
                text = stringResource(
                    R.string.dc_cable_result_recommended,
                    formatCrossSection(result.recommendedCrossSectionMm2, locale),
                ),
            )

            CalculatorResultDivider()

            CalculatorResultRow(
                label = stringResource(R.string.dc_cable_result_minimum_label),
                value = stringResource(
                    R.string.dc_cable_result_minimum_value,
                    formatCrossSection(result.minimumCrossSectionMm2, locale),
                ),
            )

            if (inputMode == DcCableLoadInputMode.POWER) {
                CalculatorResultRow(
                    label = stringResource(R.string.dc_cable_result_current_label),
                    value = stringResource(
                        R.string.dc_cable_result_current_value,
                        formatDcCableAmount(result.currentAmps, locale, 2),
                    ),
                )
            }

            CalculatorResultRow(
                label = stringResource(R.string.dc_cable_result_voltage_drop_label),
                value = stringResource(
                    R.string.dc_cable_result_voltage_drop_value,
                    formatDcCableAmount(result.voltageDropVolts, locale, 2),
                    formatDcCableAmount(result.voltageDropPercent, locale, 2),
                ),
            )

            CalculatorSecondaryResultText(
                text = stringResource(
                    R.string.dc_cable_result_drop_limit,
                    formatDcCableAmount(result.maxAllowedDropVolts, locale, 2),
                    formatDcCableAmount(result.maxAllowedDropPercent, locale, 1),
                ),
            )
        }

        Spacer(Modifier.height(8.dp))
    }
}

private enum class DcCableVoltagePreset(val value: String, val labelRes: Int) {
    SIX("6", R.string.dc_cable_preset_6v),
    TWELVE("12", R.string.dc_cable_preset_12v),
    TWENTY_FOUR("24", R.string.dc_cable_preset_24v),
}

private enum class DcCableDropPreset(val value: String, val labelRes: Int) {
    THREE("3", R.string.dc_cable_preset_drop_3),
    FIVE("5", R.string.dc_cable_preset_drop_5),
    TEN("10", R.string.dc_cable_preset_drop_10),
}

private fun formatCrossSection(mm2: Double, locale: Locale): String {
    return if (mm2 < 10.0) {
        String.format(locale, "%.2f", mm2)
    } else {
        String.format(locale, "%.1f", mm2)
    }
}

private fun formatDcCableAmount(value: Double, locale: Locale, decimals: Int): String {
    val pattern = "%.${decimals}f"
    return String.format(locale, pattern, value)
}
