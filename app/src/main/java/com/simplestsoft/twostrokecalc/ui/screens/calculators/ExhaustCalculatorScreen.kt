package com.simplestsoft.twostrokecalc.ui.screens.calculators

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.simplestsoft.twostrokecalc.R
import com.simplestsoft.twostrokecalc.domain.calculation.ExpansionChamberOptionalInputs
import com.simplestsoft.twostrokecalc.domain.calculation.ExpansionChamberCalculator
import com.simplestsoft.twostrokecalc.domain.calculation.ExpansionChamberCoefficients
import com.simplestsoft.twostrokecalc.domain.calculation.ExpansionChamberDiffuserStages
import com.simplestsoft.twostrokecalc.domain.calculation.ExpansionChamberInput
import com.simplestsoft.twostrokecalc.domain.calculation.ExpansionChamberPreset
import com.simplestsoft.twostrokecalc.domain.calculation.ExpansionChamberResult
import com.simplestsoft.twostrokecalc.ui.components.AppColors
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorCollapsibleSection
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorDecimalField
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorEmptyResultText
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorChoiceField
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorDropdownOption
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorHeader
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorInvalidResultText
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorResultCard
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorResultDivider
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorResultRow
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorScaffold
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorSection
import com.simplestsoft.twostrokecalc.ui.components.calculator.filterDecimalInput
import com.simplestsoft.twostrokecalc.ui.components.calculator.parseDecimal
import com.simplestsoft.twostrokecalc.ui.exhaust.ExhaustPipeDiagram
import com.simplestsoft.twostrokecalc.ui.exhaust.ExhaustPipeDisplayMode
import com.simplestsoft.twostrokecalc.ui.exhaust.exhaustSegmentLabel
import com.simplestsoft.twostrokecalc.ui.exhaust.formatExhaustDecimal
import com.simplestsoft.twostrokecalc.ui.util.DialogFullscreenEffect
import com.simplestsoft.twostrokecalc.ui.util.ImmersiveSystemBarsEffect
import java.util.Locale

@Composable
fun ExhaustCalculatorScreen() {
    var presetName by rememberSaveable { mutableStateOf(ExpansionChamberPreset.ENDURO.name) }
    var exhaustWidthText by rememberSaveable { mutableStateOf("40") }
    var exhaustHeightText by rememberSaveable { mutableStateOf("20") }
    var exhaustDurationText by rememberSaveable { mutableStateOf("180") }
    var transferDurationText by rememberSaveable { mutableStateOf("130") }
    var rpmText by rememberSaveable { mutableStateOf("9000") }
    var powerPsText by rememberSaveable { mutableStateOf("30") }
    var displacementText by rememberSaveable { mutableStateOf("125") }
    var hornCoeffText by rememberSaveable { mutableStateOf("1.5") }
    var k0Text by rememberSaveable { mutableStateOf("0.70") }
    var k1Text by rememberSaveable { mutableStateOf("1.125") }
    var k2Text by rememberSaveable { mutableStateOf("2.25") }
    var bmepOverrideText by rememberSaveable { mutableStateOf("") }
    var exhaustTempOverrideText by rememberSaveable { mutableStateOf("") }
    var speedOfSoundOverrideText by rememberSaveable { mutableStateOf("") }
    var gammaText by rememberSaveable { mutableStateOf("1.4") }
    var gasConstantText by rememberSaveable { mutableStateOf("287") }
    var portAreaOverrideText by rememberSaveable { mutableStateOf("") }
    var eqPortDiameterOverrideText by rememberSaveable { mutableStateOf("") }
    var diffuserStagesName by rememberSaveable { mutableStateOf(ExpansionChamberDiffuserStages.THREE.name) }
    var selectedSegmentIndex by rememberSaveable { mutableStateOf<Int?>(null) }

    val preset = ExpansionChamberPreset.entries.firstOrNull { it.name == presetName } ?: ExpansionChamberPreset.ENDURO
    val diffuserStages = ExpansionChamberDiffuserStages.entries.firstOrNull { it.name == diffuserStagesName }
        ?: ExpansionChamberDiffuserStages.THREE
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()

    fun applyPreset(selected: ExpansionChamberPreset) {
        presetName = selected.name
        if (selected != ExpansionChamberPreset.CUSTOM) {
            val coeffs = ExpansionChamberCoefficients.forPreset(selected)
            hornCoeffText = formatExhaustDecimal(coeffs.hornCoefficient, 2, locale)
            k0Text = formatExhaustDecimal(coeffs.k0, 3, locale)
            k1Text = formatExhaustDecimal(coeffs.k1, 3, locale)
            k2Text = formatExhaustDecimal(coeffs.k2, 2, locale)
        }
    }

    val exhaustWidth = parseDecimal(exhaustWidthText)
    val exhaustHeight = parseDecimal(exhaustHeightText)
    val exhaustDuration = parseDecimal(exhaustDurationText)
    val transferDuration = parseDecimal(transferDurationText)
    val rpm = parseDecimal(rpmText)
    val powerPs = parseDecimal(powerPsText)
    val displacement = parseDecimal(displacementText)
    val hornCoeff = parseDecimal(hornCoeffText)
    val k0 = parseDecimal(k0Text)
    val k1 = parseDecimal(k1Text)
    val k2 = parseDecimal(k2Text)
    val bmepOverride = parseDecimal(bmepOverrideText)
    val exhaustTempOverride = parseDecimal(exhaustTempOverrideText)
    val speedOfSoundOverride = parseDecimal(speedOfSoundOverrideText)
    val gamma = parseDecimal(gammaText)
    val gasConstant = parseDecimal(gasConstantText)
    val portAreaOverride = parseDecimal(portAreaOverrideText)
    val eqPortDiameterOverride = parseDecimal(eqPortDiameterOverrideText)

    val autoBmep = if (powerPs != null && displacement != null && rpm != null) {
        ExpansionChamberCalculator.calculateBmepBar(powerPs, displacement, rpm)
    } else {
        null
    }
    val autoExhaustTemp = autoBmep?.let(ExpansionChamberCalculator::exhaustTemperatureFromBmep)
        ?: bmepOverride?.let(ExpansionChamberCalculator::exhaustTemperatureFromBmep)
    val autoSpeedOfSound = (exhaustTempOverride ?: autoExhaustTemp)?.let { temp ->
        ExpansionChamberCalculator.calculateSpeedOfSoundMs(
            exhaustGasTempCelsius = temp,
            specificHeatRatio = gamma ?: 1.4,
            gasConstant = gasConstant ?: 287.0,
        )
    }
    val autoPortArea = if (exhaustWidth != null && exhaustHeight != null) {
        exhaustWidth * exhaustHeight
    } else {
        null
    }
    val autoEqPortDiameter = autoPortArea?.let { area ->
        ExpansionChamberCalculator.calculateEquivalentPortDiameterMm(area)
    } ?: eqPortDiameterOverride

    val hasInput = listOf(
        exhaustWidthText,
        exhaustHeightText,
        exhaustDurationText,
        transferDurationText,
        rpmText,
        powerPsText,
        displacementText,
        hornCoeffText,
        k0Text,
        k1Text,
        k2Text,
        bmepOverrideText,
        exhaustTempOverrideText,
        speedOfSoundOverrideText,
        gammaText,
        gasConstantText,
        portAreaOverrideText,
        eqPortDiameterOverrideText,
    ).any { it.isNotBlank() }

    val hasPortGeometry = (exhaustWidth != null && exhaustHeight != null && exhaustWidth > 0.0 && exhaustHeight > 0.0) ||
        (portAreaOverride != null && portAreaOverride > 0.0) ||
        (eqPortDiameterOverride != null && eqPortDiameterOverride > 0.0)

    val hasWaveInputs = bmepOverride != null && bmepOverride > 0.0 ||
        (powerPs != null && displacement != null && powerPs > 0.0 && displacement > 0.0)

    val result = if (
        hasPortGeometry &&
        exhaustDuration != null &&
        transferDuration != null &&
        rpm != null &&
        rpm > 0.0 &&
        exhaustDuration > 0.0 &&
        transferDuration > 0.0 &&
        hasWaveInputs &&
        hornCoeff != null &&
        k0 != null &&
        k1 != null &&
        k2 != null &&
        k0 > 0.0 &&
        k1 > 0.0 &&
        k2 > 0.0 &&
        (diffuserStages == ExpansionChamberDiffuserStages.ONE || hornCoeff > 0.0) &&
        (gamma == null || gamma > 0.0) &&
        (gasConstant == null || gasConstant > 0.0)
    ) {
        ExpansionChamberCalculator.calculate(
            ExpansionChamberInput(
                exhaustPortWidthMm = exhaustWidth?.takeIf { it > 0.0 },
                exhaustPortHeightMm = exhaustHeight?.takeIf { it > 0.0 },
                exhaustPortDurationDeg = exhaustDuration,
                transferPortDurationDeg = transferDuration,
                rpm = rpm,
                powerPs = powerPs?.takeIf { it > 0.0 },
                displacementCc = displacement?.takeIf { it > 0.0 },
                diffuserStages = diffuserStages,
                coefficients = ExpansionChamberCoefficients(
                    k0 = k0,
                    k1 = k1,
                    k2 = k2,
                    hornCoefficient = hornCoeff,
                ),
                optionalInputs = ExpansionChamberOptionalInputs(
                    bmepBar = bmepOverride?.takeIf { it > 0.0 },
                    exhaustGasTempCelsius = exhaustTempOverride?.takeIf { it > -273.0 },
                    speedOfSoundMs = speedOfSoundOverride?.takeIf { it > 0.0 },
                    specificHeatRatio = gamma?.takeIf { it > 0.0 },
                    gasConstant = gasConstant?.takeIf { it > 0.0 },
                    portAreaMm2 = portAreaOverride?.takeIf { it > 0.0 },
                    equivalentPortDiameterMm = eqPortDiameterOverride?.takeIf { it > 0.0 },
                ),
            ),
        )
    } else {
        null
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        ExhaustPipeLandscapeOverlay(
            result = result,
            locale = locale,
            selectedSegmentIndex = selectedSegmentIndex,
            onSegmentSelected = { selectedSegmentIndex = it },
        )
    }

    if (!isLandscape) {
        CalculatorScaffold {
            CalculatorHeader(
                title = stringResource(R.string.exhaust_calculator_title),
                subtitle = stringResource(R.string.exhaust_calculator_subtitle),
            )

            CalculatorSection(title = stringResource(R.string.exhaust_section_design)) {
                val diffuserStageOptions = ExpansionChamberDiffuserStages.entries.map { stages ->
                    CalculatorDropdownOption(
                        key = stages.name,
                        label = stringResource(
                            R.string.exhaust_diffuser_stages_option,
                            stages.stageCount,
                        ),
                    )
                }
                CalculatorChoiceField(
                    label = stringResource(R.string.exhaust_diffuser_stages_label),
                    options = diffuserStageOptions,
                    selectedKey = diffuserStages.name,
                    onOptionSelected = { key ->
                        diffuserStagesName = key
                        selectedSegmentIndex = null
                        applyPreset(ExpansionChamberPreset.CUSTOM)
                    },
                    supportingText = when (diffuserStages) {
                        ExpansionChamberDiffuserStages.ONE -> stringResource(R.string.exhaust_diffuser_stages_hint_one)
                        ExpansionChamberDiffuserStages.TWO -> stringResource(R.string.exhaust_diffuser_stages_hint_two)
                        ExpansionChamberDiffuserStages.THREE -> stringResource(R.string.exhaust_diffuser_stages_hint_three)
                    },
                )
            }

            CalculatorSection(title = stringResource(R.string.exhaust_section_preset)) {
                val presetOptions = ExpansionChamberPreset.entries.map { item ->
                    CalculatorDropdownOption(item.name, stringResource(item.labelRes()))
                }
                CalculatorChoiceField(
                    label = "",
                    options = presetOptions,
                    selectedKey = preset.name,
                    onOptionSelected = { key ->
                        ExpansionChamberPreset.entries.firstOrNull { it.name == key }?.let { applyPreset(it) }
                    },
                )
            }

            CalculatorSection(title = stringResource(R.string.exhaust_section_port)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CalculatorDecimalField(
                        value = exhaustWidthText,
                        onValueChange = {
                            exhaustWidthText = it.filterDecimalInput()
                            applyPreset(ExpansionChamberPreset.CUSTOM)
                        },
                        label = stringResource(R.string.exhaust_port_width_label),
                        suffix = stringResource(R.string.exhaust_unit_mm),
                        modifier = Modifier.weight(1f),
                    )
                    CalculatorDecimalField(
                        value = exhaustHeightText,
                        onValueChange = {
                            exhaustHeightText = it.filterDecimalInput()
                            applyPreset(ExpansionChamberPreset.CUSTOM)
                        },
                        label = stringResource(R.string.exhaust_port_height_label),
                        suffix = stringResource(R.string.exhaust_unit_mm),
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CalculatorDecimalField(
                        value = exhaustDurationText,
                        onValueChange = {
                            exhaustDurationText = it.filterDecimalInput()
                            applyPreset(ExpansionChamberPreset.CUSTOM)
                        },
                        label = stringResource(R.string.exhaust_port_duration_label),
                        suffix = stringResource(R.string.exhaust_unit_deg),
                        modifier = Modifier.weight(1f),
                    )
                    CalculatorDecimalField(
                        value = transferDurationText,
                        onValueChange = {
                            transferDurationText = it.filterDecimalInput()
                            applyPreset(ExpansionChamberPreset.CUSTOM)
                        },
                        label = stringResource(R.string.exhaust_transfer_duration_label),
                        suffix = stringResource(R.string.exhaust_unit_deg),
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            CalculatorSection(title = stringResource(R.string.exhaust_section_engine)) {
                CalculatorDecimalField(
                    value = rpmText,
                    onValueChange = {
                        rpmText = it.filterDecimalInput()
                        applyPreset(ExpansionChamberPreset.CUSTOM)
                    },
                    label = stringResource(R.string.exhaust_rpm_label),
                    suffix = stringResource(R.string.exhaust_unit_rpm),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CalculatorDecimalField(
                        value = powerPsText,
                        onValueChange = {
                            powerPsText = it.filterDecimalInput()
                            applyPreset(ExpansionChamberPreset.CUSTOM)
                        },
                        label = stringResource(R.string.exhaust_power_label),
                        suffix = stringResource(R.string.exhaust_unit_ps),
                        modifier = Modifier.weight(1f),
                    )
                    CalculatorDecimalField(
                        value = displacementText,
                        onValueChange = {
                            displacementText = it.filterDecimalInput()
                            applyPreset(ExpansionChamberPreset.CUSTOM)
                        },
                        label = stringResource(R.string.exhaust_displacement_label),
                        suffix = stringResource(R.string.exhaust_unit_cc),
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            CalculatorSection(title = stringResource(R.string.exhaust_section_coefficients)) {
                if (diffuserStages != ExpansionChamberDiffuserStages.ONE) {
                    CalculatorDecimalField(
                        value = hornCoeffText,
                        onValueChange = {
                            hornCoeffText = it.filterDecimalInput()
                            applyPreset(ExpansionChamberPreset.CUSTOM)
                        },
                        label = stringResource(R.string.exhaust_horn_coeff_label),
                        supportingText = stringResource(R.string.exhaust_horn_coeff_hint),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CalculatorDecimalField(
                        value = k1Text,
                        onValueChange = {
                            k1Text = it.filterDecimalInput()
                            applyPreset(ExpansionChamberPreset.CUSTOM)
                        },
                        label = stringResource(R.string.exhaust_k1_label),
                        modifier = Modifier.weight(1f),
                    )
                    CalculatorDecimalField(
                        value = k2Text,
                        onValueChange = {
                            k2Text = it.filterDecimalInput()
                            applyPreset(ExpansionChamberPreset.CUSTOM)
                        },
                        label = stringResource(R.string.exhaust_k2_label),
                        modifier = Modifier.weight(1f),
                    )
                    CalculatorDecimalField(
                        value = k0Text,
                        onValueChange = {
                            k0Text = it.filterDecimalInput()
                            applyPreset(ExpansionChamberPreset.CUSTOM)
                        },
                        label = stringResource(R.string.exhaust_k0_label),
                        modifier = Modifier.weight(1f),
                    )
                }
                Text(
                    text = stringResource(R.string.exhaust_formula_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.textSecondary(),
                )
            }

            CalculatorCollapsibleSection(
                title = stringResource(R.string.exhaust_section_advanced),
            ) {
                Text(
                    text = stringResource(R.string.exhaust_section_advanced_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.textSecondary(),
                )
                CalculatorDecimalField(
                    value = bmepOverrideText,
                    onValueChange = {
                        bmepOverrideText = it.filterDecimalInput()
                        applyPreset(ExpansionChamberPreset.CUSTOM)
                    },
                    label = stringResource(R.string.exhaust_optional_bmep_label),
                    suffix = stringResource(R.string.exhaust_unit_bar),
                    supportingText = if (bmepOverrideText.isBlank() && autoBmep != null) {
                        stringResource(
                            R.string.exhaust_optional_auto_value,
                            formatExhaustDecimal(autoBmep, 2, locale),
                        )
                    } else {
                        null
                    },
                )
                CalculatorDecimalField(
                    value = exhaustTempOverrideText,
                    onValueChange = {
                        exhaustTempOverrideText = it.filterDecimalInput()
                        applyPreset(ExpansionChamberPreset.CUSTOM)
                    },
                    label = stringResource(R.string.exhaust_optional_exhaust_temp_label),
                    suffix = stringResource(R.string.exhaust_unit_celsius),
                    supportingText = if (exhaustTempOverrideText.isBlank() && autoExhaustTemp != null) {
                        stringResource(
                            R.string.exhaust_optional_auto_value,
                            formatExhaustDecimal(autoExhaustTemp, 0, locale),
                        )
                    } else {
                        null
                    },
                )
                CalculatorDecimalField(
                    value = speedOfSoundOverrideText,
                    onValueChange = {
                        speedOfSoundOverrideText = it.filterDecimalInput()
                        applyPreset(ExpansionChamberPreset.CUSTOM)
                    },
                    label = stringResource(R.string.exhaust_optional_speed_of_sound_label),
                    suffix = stringResource(R.string.exhaust_unit_ms),
                    supportingText = if (speedOfSoundOverrideText.isBlank() && autoSpeedOfSound != null) {
                        stringResource(
                            R.string.exhaust_optional_auto_value,
                            formatExhaustDecimal(autoSpeedOfSound, 1, locale),
                        )
                    } else {
                        null
                    },
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CalculatorDecimalField(
                        value = gammaText,
                        onValueChange = {
                            gammaText = it.filterDecimalInput()
                            applyPreset(ExpansionChamberPreset.CUSTOM)
                        },
                        label = stringResource(R.string.exhaust_optional_gamma_label),
                        modifier = Modifier.weight(1f),
                    )
                    CalculatorDecimalField(
                        value = gasConstantText,
                        onValueChange = {
                            gasConstantText = it.filterDecimalInput()
                            applyPreset(ExpansionChamberPreset.CUSTOM)
                        },
                        label = stringResource(R.string.exhaust_optional_gas_constant_label),
                        modifier = Modifier.weight(1f),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CalculatorDecimalField(
                        value = portAreaOverrideText,
                        onValueChange = {
                            portAreaOverrideText = it.filterDecimalInput()
                            applyPreset(ExpansionChamberPreset.CUSTOM)
                        },
                        label = stringResource(R.string.exhaust_optional_port_area_label),
                        suffix = stringResource(R.string.exhaust_unit_mm2),
                        supportingText = if (portAreaOverrideText.isBlank() && autoPortArea != null) {
                            stringResource(
                                R.string.exhaust_optional_auto_value,
                                formatExhaustDecimal(autoPortArea, 0, locale),
                            )
                        } else {
                            null
                        },
                        modifier = Modifier.weight(1f),
                    )
                    CalculatorDecimalField(
                        value = eqPortDiameterOverrideText,
                        onValueChange = {
                            eqPortDiameterOverrideText = it.filterDecimalInput()
                            applyPreset(ExpansionChamberPreset.CUSTOM)
                        },
                        label = stringResource(R.string.exhaust_optional_eq_port_diameter_label),
                        suffix = stringResource(R.string.exhaust_unit_mm),
                        supportingText = if (eqPortDiameterOverrideText.isBlank() && autoEqPortDiameter != null) {
                            stringResource(
                                R.string.exhaust_optional_auto_value,
                                formatExhaustDecimal(autoEqPortDiameter, 2, locale),
                            )
                        } else {
                            null
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            ExhaustResultsCard(result = result, hasInput = hasInput, locale = locale)

            CalculatorSection(title = stringResource(R.string.exhaust_section_preview)) {
                if (result != null) {
                    ExhaustPipeDiagram(
                        result = result,
                        locale = locale,
                        displayMode = ExhaustPipeDisplayMode.EMBEDDED,
                    )
                    Text(
                        text = stringResource(R.string.exhaust_preview_rotate_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.textSecondary(),
                        modifier = Modifier.padding(top = 4.dp),
                    )
                } else {
                    CalculatorEmptyResultText(stringResource(R.string.exhaust_result_empty))
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ExhaustResultsCard(
    result: ExpansionChamberResult?,
    hasInput: Boolean,
    locale: Locale,
) {
    CalculatorResultCard(title = stringResource(R.string.exhaust_result_title)) {
        when {
            !hasInput -> CalculatorEmptyResultText(stringResource(R.string.exhaust_result_empty))
            result == null -> CalculatorInvalidResultText(stringResource(R.string.exhaust_result_invalid))
            else -> {
                val manualHint = stringResource(R.string.exhaust_optional_manual_hint)
                CalculatorResultRow(
                    label = stringResource(R.string.exhaust_result_port_area),
                    value = "${formatExhaustDecimal(result.portAreaMm2, 0, locale)} mm²",
                    hint = if (result.usedManualPortArea) manualHint else null,
                )
                CalculatorResultRow(
                    label = stringResource(R.string.exhaust_result_eq_port_diameter),
                    value = "${formatExhaustDecimal(result.equivalentPortDiameterMm, 2, locale)} mm",
                    hint = if (result.usedManualEquivalentPortDiameter) manualHint else null,
                )
                CalculatorResultRow(
                    label = stringResource(R.string.exhaust_result_diffuser_stages),
                    value = stringResource(
                        R.string.exhaust_diffuser_stages_option,
                        result.diffuserStageCount,
                    ),
                )
                if (result.diffuserStageCount > 1) {
                    CalculatorResultRow(
                        label = stringResource(R.string.exhaust_result_horn_coeff),
                        value = formatExhaustDecimal(result.hornCoefficient, 2, locale),
                    )
                }
                CalculatorResultRow(
                    label = stringResource(R.string.exhaust_result_tuned_length),
                    value = "${formatExhaustDecimal(result.tunedLengthMm, 0, locale)} mm",
                )
                CalculatorResultRow(
                    label = stringResource(R.string.exhaust_result_total_length),
                    value = "${formatExhaustDecimal(result.totalLengthMm, 0, locale)} mm",
                )
                CalculatorResultDivider()
                CalculatorResultRow(
                    label = stringResource(R.string.exhaust_result_bmep),
                    value = "${formatExhaustDecimal(result.bmepBar, 2, locale)} bar",
                    hint = if (result.usedManualBmep) manualHint else null,
                )
                CalculatorResultRow(
                    label = stringResource(R.string.exhaust_result_exhaust_temp),
                    value = "${formatExhaustDecimal(result.exhaustGasTempCelsius, 0, locale)} °C",
                    hint = if (result.usedManualExhaustTemp) manualHint else null,
                )
                CalculatorResultRow(
                    label = stringResource(R.string.exhaust_result_speed_of_sound),
                    value = "${formatExhaustDecimal(result.speedOfSoundMs, 1, locale)} m/s",
                    hint = if (result.usedManualSpeedOfSound) manualHint else null,
                )
                CalculatorResultRow(
                    label = stringResource(R.string.exhaust_result_gamma),
                    value = formatExhaustDecimal(result.specificHeatRatio, 2, locale),
                )
                CalculatorResultRow(
                    label = stringResource(R.string.exhaust_result_gas_constant),
                    value = "${formatExhaustDecimal(result.gasConstant, 0, locale)} J/(kg·K)",
                )
                CalculatorResultDivider()
                result.segments.forEachIndexed { index, segment ->
                    if (index > 0) {
                        CalculatorResultDivider()
                    }
                    CalculatorResultRow(
                        label = exhaustSegmentLabel(segment.id),
                        value = stringResource(
                            R.string.exhaust_segment_value,
                            formatExhaustDecimal(segment.lengthMm, 0, locale),
                            formatExhaustDecimal(segment.startDiameterMm, 1, locale),
                            formatExhaustDecimal(segment.endDiameterMm, 1, locale),
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun ExhaustPipeLandscapeOverlay(
    result: ExpansionChamberResult?,
    locale: Locale,
    selectedSegmentIndex: Int?,
    onSegmentSelected: (Int?) -> Unit,
) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
    ) {
        DialogFullscreenEffect()
        ImmersiveSystemBarsEffect(enabled = true)
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = AppColors.background(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(horizontal = 4.dp, vertical = 4.dp),
            ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.exhaust_fullscreen_title),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.textPrimary(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                    horizontalAlignment = Alignment.End,
                ) {
                    Text(
                        text = stringResource(R.string.exhaust_segment_tap_hint_short),
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.textSecondary(),
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = stringResource(R.string.exhaust_fullscreen_hint),
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.textSecondary(),
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (result != null) {
                ExhaustPipeDiagram(
                    result = result,
                    locale = locale,
                    displayMode = ExhaustPipeDisplayMode.FULLSCREEN,
                    selectedSegmentIndex = selectedSegmentIndex,
                    onSegmentSelected = onSegmentSelected,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                )
            } else {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.exhaust_fullscreen_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.textSecondary(),
                        textAlign = TextAlign.Center,
                    )
                }
            }
            }
        }
    }
}

private fun ExpansionChamberPreset.labelRes(): Int = when (this) {
    ExpansionChamberPreset.ENDURO -> R.string.exhaust_preset_enduro
    ExpansionChamberPreset.CROSS -> R.string.exhaust_preset_cross
    ExpansionChamberPreset.GRAND_PRIX -> R.string.exhaust_preset_gp
    ExpansionChamberPreset.CUSTOM -> R.string.exhaust_preset_custom
}
