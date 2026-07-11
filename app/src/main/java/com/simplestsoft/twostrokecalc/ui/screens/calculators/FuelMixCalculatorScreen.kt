package com.simplestsoft.twostrokecalc.ui.screens.calculators

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.simplestsoft.twostrokecalc.R
import com.simplestsoft.twostrokecalc.domain.calculation.FuelMixCalculator
import com.simplestsoft.twostrokecalc.domain.model.FuelMixPreset
import com.simplestsoft.twostrokecalc.ui.components.AppColors
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorDecimalField
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorChoiceField
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorDropdownOption
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorEmptyResultText
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorHeader
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorInputModeSwitch
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorPrimaryResultText
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorResultCard
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorScaffold
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorSliderField
import com.simplestsoft.twostrokecalc.ui.components.calculator.filterDecimalInput
import com.simplestsoft.twostrokecalc.ui.components.calculator.parseDecimal
import com.simplestsoft.twostrokecalc.ui.components.FormFieldDefaults
import java.util.Locale
import kotlin.math.roundToInt

private const val CUSTOM_PRESET_KEY = "custom"
private const val ML_PER_FLUID_OUNCE = 29.5735
private const val LITERS_PER_US_GALLON = 3.78541

@Composable
fun FuelMixCalculatorScreen() {
    var selectedPresetKey by rememberSaveable { mutableStateOf(FuelMixPreset.STANDARD.name) }
    var customRatioText by rememberSaveable { mutableStateOf("") }
    var inputMode by rememberSaveable { mutableStateOf(FuelInputMode.FUEL.name) }
    var inputAmount by rememberSaveable {
        mutableFloatStateOf(FuelMixCalculator.DEFAULT_FUEL_LITERS.toFloat())
    }

    val mode = FuelInputMode.entries.firstOrNull { it.name == inputMode } ?: FuelInputMode.FUEL
    val useFuel = mode == FuelInputMode.FUEL

    val ratioPartsFuel = resolveRatioPartsFuel(selectedPresetKey, customRatioText)

    val resultOilMl = when (mode) {
        FuelInputMode.FUEL -> {
            val ratio = ratioPartsFuel
            if (ratio != null) {
                FuelMixCalculator.oilMillilitersFromFuelLiters(
                    FuelMixCalculator.snapFuelLiters(inputAmount.toDouble()),
                    ratio,
                )
            } else {
                null
            }
        }
        FuelInputMode.OIL -> FuelMixCalculator.snapOilMl(inputAmount.toDouble())
    }

    val resultFuelL = when (mode) {
        FuelInputMode.OIL -> {
            val ratio = ratioPartsFuel
            if (ratio != null) {
                FuelMixCalculator.fuelLitersFromOilMilliliters(
                    FuelMixCalculator.snapOilMl(inputAmount.toDouble()),
                    ratio,
                )
            } else {
                null
            }
        }
        FuelInputMode.FUEL -> FuelMixCalculator.snapFuelLiters(inputAmount.toDouble())
    }

    val onInputModeChange: (Boolean) -> Unit = { useFuelMode ->
        val newMode = if (useFuelMode) FuelInputMode.FUEL else FuelInputMode.OIL
        val currentMode = FuelInputMode.entries.firstOrNull { it.name == inputMode } ?: FuelInputMode.FUEL
        if (newMode != currentMode) {
            val ratio = resolveRatioPartsFuel(selectedPresetKey, customRatioText)
            if (ratio != null) {
                inputAmount = when {
                    currentMode == FuelInputMode.FUEL && newMode == FuelInputMode.OIL ->
                        FuelMixCalculator.oilMillilitersFromFuelLiters(
                            FuelMixCalculator.snapFuelLiters(inputAmount.toDouble()),
                            ratio,
                        )
                            ?.let { FuelMixCalculator.snapOilMl(it).toFloat() }
                            ?: inputAmount

                    currentMode == FuelInputMode.OIL && newMode == FuelInputMode.FUEL ->
                        FuelMixCalculator.fuelLitersFromOilMilliliters(
                            FuelMixCalculator.snapOilMl(inputAmount.toDouble()),
                            ratio,
                        )
                            ?.let { FuelMixCalculator.snapFuelLiters(it).toFloat() }
                            ?: inputAmount

                    else -> inputAmount
                }
            }
            inputMode = newMode.name
        }
    }

    CalculatorScaffold {
        CalculatorHeader(
            title = stringResource(R.string.fuel_mix_calculator_title),
            subtitle = stringResource(R.string.fuel_mix_calculator_subtitle),
        )

        val presetOptions = buildList {
            FuelMixPreset.entries.forEach { preset ->
                add(CalculatorDropdownOption(preset.name, stringResource(preset.labelRes())))
            }
            add(
                CalculatorDropdownOption(
                    CUSTOM_PRESET_KEY,
                    stringResource(R.string.fuel_mix_preset_custom),
                ),
            )
        }

        val selectedPreset = FuelMixPreset.entries.firstOrNull { it.name == selectedPresetKey }
        val presetSupportingText = when {
            selectedPresetKey == CUSTOM_PRESET_KEY -> stringResource(R.string.fuel_mix_custom_ratio_hint)
            selectedPreset != null -> stringResource(selectedPreset.descriptionRes())
            else -> null
        }

        CalculatorChoiceField(
            label = stringResource(R.string.fuel_mix_presets_label),
            options = presetOptions,
            selectedKey = selectedPresetKey,
            onOptionSelected = { selectedPresetKey = it },
            supportingText = presetSupportingText,
        )

        if (selectedPresetKey == CUSTOM_PRESET_KEY) {
            CalculatorDecimalField(
                value = customRatioText,
                onValueChange = { customRatioText = it.filterDecimalInput() },
                label = stringResource(R.string.fuel_mix_custom_ratio_label),
            )
        } else if (selectedPreset != null) {
            Text(
                text = stringResource(R.string.fuel_mix_ratio_display, selectedPreset.ratio),
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.textSecondary(),
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(FormFieldDefaults.fieldSpacing)) {
            CalculatorInputModeSwitch(
                optionALabel = stringResource(R.string.fuel_mix_input_mode_fuel),
                optionBLabel = stringResource(R.string.fuel_mix_input_mode_oil),
                useOptionA = useFuel,
                onUseOptionAChange = onInputModeChange,
            )

            if (useFuel) {
                CalculatorSliderField(
                    label = stringResource(R.string.fuel_mix_fuel_amount_label),
                    value = inputAmount,
                    onValueChange = { inputAmount = it },
                    valueRange = FuelMixCalculator.MIN_FUEL_LITERS.toFloat()..
                        FuelMixCalculator.MAX_FUEL_LITERS.toFloat(),
                    steps = FuelMixCalculator.fuelSliderSteps,
                    valueDisplay = stringResource(
                        R.string.fuel_mix_fuel_slider_value,
                        formatFuelLiters(inputAmount.toDouble()),
                    ),
                )
            } else {
                CalculatorSliderField(
                    label = stringResource(R.string.fuel_mix_oil_amount_label),
                    value = inputAmount,
                    onValueChange = { inputAmount = it },
                    valueRange = FuelMixCalculator.MIN_OIL_ML.toFloat()..
                        FuelMixCalculator.MAX_OIL_ML.toFloat(),
                    steps = FuelMixCalculator.oilSliderSteps,
                    valueDisplay = stringResource(
                        R.string.fuel_mix_oil_slider_value,
                        formatOilMilliliters(inputAmount.toDouble()),
                    ),
                )
            }
        }

        CalculatorResultCard(title = "") {
            if (ratioPartsFuel == null) {
                CalculatorEmptyResultText(stringResource(R.string.fuel_mix_result_invalid_ratio))
                return@CalculatorResultCard
            }

            Text(
                text = stringResource(R.string.fuel_mix_result_header, ratioPartsFuel),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.textPrimary(),
            )

            when (mode) {
                FuelInputMode.FUEL -> {
                    resultOilMl?.let { oil ->
                        CalculatorPrimaryResultText(
                            text = stringResource(
                                R.string.fuel_mix_result_oil_amount,
                                formatAmount(oil),
                            ),
                        )
                        Text(
                            text = stringResource(
                                R.string.fuel_mix_result_oil_fluid_ounces,
                                formatAmount(oil / ML_PER_FLUID_OUNCE),
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppColors.textSecondary(),
                        )
                    }
                }
                FuelInputMode.OIL -> {
                    resultFuelL?.let { fuel ->
                        CalculatorPrimaryResultText(
                            text = stringResource(
                                R.string.fuel_mix_result_fuel_amount,
                                formatAmount(fuel),
                            ),
                        )
                        Text(
                            text = stringResource(
                                R.string.fuel_mix_result_fuel_us_gallons,
                                formatAmount(fuel / LITERS_PER_US_GALLON),
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppColors.textSecondary(),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

private enum class FuelInputMode {
    FUEL,
    OIL,
}

private fun FuelMixPreset.labelRes(): Int = when (this) {
    FuelMixPreset.STANDARD -> R.string.fuel_mix_preset_standard
    FuelMixPreset.OLDER -> R.string.fuel_mix_preset_older
    FuelMixPreset.CLASSIC -> R.string.fuel_mix_preset_classic
    FuelMixPreset.RACING -> R.string.fuel_mix_preset_racing
    FuelMixPreset.SYNTHETIC -> R.string.fuel_mix_preset_synthetic
}

private fun FuelMixPreset.descriptionRes(): Int = when (this) {
    FuelMixPreset.STANDARD -> R.string.fuel_mix_preset_standard_desc
    FuelMixPreset.OLDER -> R.string.fuel_mix_preset_older_desc
    FuelMixPreset.CLASSIC -> R.string.fuel_mix_preset_classic_desc
    FuelMixPreset.RACING -> R.string.fuel_mix_preset_racing_desc
    FuelMixPreset.SYNTHETIC -> R.string.fuel_mix_preset_synthetic_desc
}

private fun resolveRatioPartsFuel(selectedPresetKey: String, customRatioText: String): Int? {
    if (selectedPresetKey == CUSTOM_PRESET_KEY) {
        val parsed = parseDecimal(customRatioText)?.roundToInt()
        return parsed?.takeIf { it > 0 }
    }
    return FuelMixPreset.entries.firstOrNull { it.name == selectedPresetKey }?.ratio
}

private fun formatFuelLiters(value: Double): String =
    String.format(Locale.getDefault(), "%.1f", FuelMixCalculator.snapFuelLiters(value))

private fun formatOilMilliliters(value: Double): String {
    val snapped = FuelMixCalculator.snapOilMl(value)
    return if (snapped >= 100.0) {
        String.format(Locale.getDefault(), "%.0f", snapped)
    } else {
        String.format(Locale.getDefault(), "%.1f", snapped)
    }
}

private fun formatAmount(value: Double): String =
    String.format(Locale.getDefault(), "%.2f", value)
