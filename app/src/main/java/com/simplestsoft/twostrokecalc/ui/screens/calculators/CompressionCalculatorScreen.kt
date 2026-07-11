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
import com.simplestsoft.twostrokecalc.domain.calculation.CompressionCalculationMode
import com.simplestsoft.twostrokecalc.domain.calculation.CompressionCalculator
import com.simplestsoft.twostrokecalc.domain.calculation.CompressionChangeResult
import com.simplestsoft.twostrokecalc.domain.calculation.CompressionResult
import com.simplestsoft.twostrokecalc.domain.calculation.CompressionTargetResult
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
fun CompressionCalculatorScreen(
    onNavigateToCalculator: (CalculatorId) -> Unit = {},
) {
    var mode by rememberSaveable { mutableStateOf(CompressionCalculationMode.FORWARD.name) }
    var boreText by rememberSaveable { mutableStateOf("72") }
    var domeDiameterText by rememberSaveable { mutableStateOf("50") }
    var strokeText by rememberSaveable { mutableStateOf("62") }
    var domeVolumeText by rememberSaveable { mutableStateOf("18") }
    var squishBandText by rememberSaveable { mutableStateOf("1.2") }
    var targetCompressionText by rememberSaveable { mutableStateOf("7.5") }
    var pistonConstantText by rememberSaveable { mutableStateOf("") }
    var currentCompressionText by rememberSaveable { mutableStateOf("7.2") }

    val selectedMode = CompressionCalculationMode.entries.firstOrNull { it.name == mode }
        ?: CompressionCalculationMode.FORWARD

    val bore = parseDecimal(boreText)
    val stroke = parseDecimal(strokeText)

    val forwardResult = if (selectedMode == CompressionCalculationMode.FORWARD) {
        val domeDiameter = parseDecimal(domeDiameterText)
        val domeVolume = parseDecimal(domeVolumeText)
        val squishBand = parseDecimal(squishBandText)
        val hasInput = bore != null && domeDiameter != null && stroke != null &&
            domeVolume != null && squishBand != null
        if (hasInput) {
            CompressionCalculator.calculate(
                boreMm = bore!!,
                domeDiameterMm = domeDiameter!!,
                strokeMm = stroke!!,
                domeVolumeMl = domeVolume!!,
                squishBandMm = squishBand!!,
            )
        } else {
            null
        }
    } else {
        null
    }

    val targetResult = if (selectedMode == CompressionCalculationMode.TARGET) {
        val targetCompression = parseDecimal(targetCompressionText)
        val pistonConstant = parseDecimal(pistonConstantText)
        val hasInput = bore != null && stroke != null && targetCompression != null
        if (hasInput) {
            CompressionCalculator.calculateTarget(
                boreMm = bore!!,
                strokeMm = stroke!!,
                targetCompressionRatio = targetCompression!!,
                pistonConstantMl = pistonConstant,
            )
        } else {
            null
        }
    } else {
        null
    }

    val changeResult = if (selectedMode == CompressionCalculationMode.CHANGE) {
        val currentCompression = parseDecimal(currentCompressionText)
        val targetCompression = parseDecimal(targetCompressionText)
        val hasInput = bore != null && stroke != null &&
            currentCompression != null && targetCompression != null
        if (hasInput) {
            CompressionCalculator.calculateChange(
                boreMm = bore!!,
                strokeMm = stroke!!,
                currentCompressionRatio = currentCompression!!,
                targetCompressionRatio = targetCompression!!,
            )
        } else {
            null
        }
    } else {
        null
    }

    CalculatorScaffold {
        CalculatorHeader(
            title = stringResource(R.string.compression_calculator_title),
            subtitle = when (selectedMode) {
                CompressionCalculationMode.FORWARD ->
                    stringResource(R.string.compression_calculator_subtitle)
                CompressionCalculationMode.TARGET ->
                    stringResource(R.string.compression_target_subtitle)
                CompressionCalculationMode.CHANGE ->
                    stringResource(R.string.compression_change_subtitle)
            },
        )

        val modeOptions = CompressionCalculationMode.entries.map { entry ->
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
            label = stringResource(R.string.compression_bore_label),
            suffix = stringResource(R.string.compression_unit_mm),
        )

        CalculatorDecimalField(
            value = strokeText,
            onValueChange = { strokeText = it.filterDecimalInput() },
            label = stringResource(R.string.compression_stroke_label),
            suffix = stringResource(R.string.compression_unit_mm),
        )

        when (selectedMode) {
            CompressionCalculationMode.FORWARD -> {
                CalculatorDecimalField(
                    value = domeDiameterText,
                    onValueChange = { domeDiameterText = it.filterDecimalInput() },
                    label = stringResource(R.string.compression_dome_diameter_label),
                    suffix = stringResource(R.string.compression_unit_mm),
                )
                CalculatorDecimalField(
                    value = domeVolumeText,
                    onValueChange = { domeVolumeText = it.filterDecimalInput() },
                    label = stringResource(R.string.compression_dome_volume_label),
                    suffix = stringResource(R.string.compression_unit_ml),
                )
                CalculatorDecimalField(
                    value = squishBandText,
                    onValueChange = { squishBandText = it.filterDecimalInput() },
                    label = stringResource(R.string.compression_squish_band_label),
                    suffix = stringResource(R.string.compression_unit_mm),
                    supportingText = stringResource(R.string.compression_squish_band_hint),
                )
            }
            CompressionCalculationMode.TARGET -> {
                CalculatorDecimalField(
                    value = targetCompressionText,
                    onValueChange = { targetCompressionText = it.filterDecimalInput() },
                    label = stringResource(R.string.compression_target_ratio_label),
                    suffix = stringResource(R.string.compression_unit_ratio),
                    supportingText = stringResource(R.string.compression_target_ratio_hint),
                )
                CalculatorDecimalField(
                    value = pistonConstantText,
                    onValueChange = { pistonConstantText = it.filterDecimalInput() },
                    label = stringResource(R.string.compression_piston_constant_label),
                    suffix = stringResource(R.string.compression_unit_ml),
                    supportingText = stringResource(R.string.compression_piston_constant_hint),
                )
            }
            CompressionCalculationMode.CHANGE -> {
                CalculatorDecimalField(
                    value = currentCompressionText,
                    onValueChange = { currentCompressionText = it.filterDecimalInput() },
                    label = stringResource(R.string.compression_current_ratio_label),
                    suffix = stringResource(R.string.compression_unit_ratio),
                )
                CalculatorDecimalField(
                    value = targetCompressionText,
                    onValueChange = { targetCompressionText = it.filterDecimalInput() },
                    label = stringResource(R.string.compression_target_ratio_label),
                    suffix = stringResource(R.string.compression_unit_ratio),
                )
            }
        }

        when (selectedMode) {
            CompressionCalculationMode.FORWARD -> ForwardResultCard(
                result = forwardResult,
                hasInput = bore != null && parseDecimal(domeDiameterText) != null &&
                    stroke != null && parseDecimal(domeVolumeText) != null &&
                    parseDecimal(squishBandText) != null,
            )
            CompressionCalculationMode.TARGET -> TargetResultCard(
                result = targetResult,
                hasInput = bore != null && stroke != null &&
                    parseDecimal(targetCompressionText) != null,
            )
            CompressionCalculationMode.CHANGE -> ChangeResultCard(
                result = changeResult,
                hasInput = bore != null && stroke != null &&
                    parseDecimal(currentCompressionText) != null &&
                    parseDecimal(targetCompressionText) != null,
            )
        }

        if (selectedMode == CompressionCalculationMode.FORWARD) {
            Text(
                text = stringResource(R.string.calculator_link_hint_compression_squish),
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.textSecondary(),
            )
            TextButton(onClick = { onNavigateToCalculator(CalculatorId.SQUISH_BAND) }) {
                Text(stringResource(R.string.calculator_link_squish_band))
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

private fun CompressionCalculationMode.labelRes(): Int = when (this) {
    CompressionCalculationMode.FORWARD -> R.string.compression_mode_forward
    CompressionCalculationMode.TARGET -> R.string.compression_mode_target
    CompressionCalculationMode.CHANGE -> R.string.compression_mode_change
}

@Composable
private fun ForwardResultCard(
    result: CompressionResult?,
    hasInput: Boolean,
) {
    CalculatorResultCard(title = stringResource(R.string.compression_result_title)) {
        when {
            !hasInput -> com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorEmptyResultText(
                stringResource(R.string.compression_result_empty),
            )
            result == null -> CalculatorInvalidResultText(stringResource(R.string.compression_result_invalid))
            else -> {
                CalculatorPrimaryResultText(
                    text = stringResource(
                        R.string.compression_result_ratio,
                        formatDecimal(result.compressionRatio, 2),
                    ),
                )
                Text(
                    text = stringResource(R.string.compression_result_ratio_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.textSecondary(),
                )
                CalculatorResultDivider()
                CalculatorResultRow(
                    label = stringResource(R.string.compression_result_displacement),
                    value = stringResource(
                        R.string.compression_result_value_ml,
                        formatDecimal(result.displacementMl, 2),
                    ),
                )
                CalculatorResultRow(
                    label = stringResource(R.string.compression_result_squish_volume),
                    value = stringResource(
                        R.string.compression_result_value_ml,
                        formatDecimal(result.squishBandVolumeMl, 2),
                    ),
                )
                CalculatorResultRow(
                    label = stringResource(R.string.compression_result_squish_area),
                    value = stringResource(
                        R.string.compression_result_value_percent,
                        formatDecimal(result.squishAreaPercent, 1),
                    ),
                    hint = stringResource(R.string.compression_result_squish_area_hint),
                )
                CalculatorResultRow(
                    label = stringResource(R.string.compression_result_bore_stroke_ratio),
                    value = formatDecimal(result.boreStrokeRatio, 2),
                    hint = stringResource(R.string.compression_result_bore_stroke_hint),
                )
            }
        }
    }
}

@Composable
private fun TargetResultCard(
    result: CompressionTargetResult?,
    hasInput: Boolean,
) {
    CalculatorResultCard(title = stringResource(R.string.compression_result_title)) {
        when {
            !hasInput -> com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorEmptyResultText(
                stringResource(R.string.compression_target_result_empty),
            )
            result == null -> CalculatorInvalidResultText(stringResource(R.string.compression_target_result_invalid))
            else -> {
                CalculatorPrimaryResultText(
                    text = stringResource(
                        R.string.compression_target_result_ratio,
                        formatDecimal(result.targetCompressionRatio, 2),
                    ),
                )
                CalculatorResultDivider()
                CalculatorResultRow(
                    label = stringResource(R.string.compression_result_displacement),
                    value = stringResource(
                        R.string.compression_result_value_ml,
                        formatDecimal(result.displacementMl, 2),
                    ),
                )
                CalculatorResultRow(
                    label = stringResource(R.string.compression_target_result_total_chamber),
                    value = stringResource(
                        R.string.compression_result_value_ml,
                        formatDecimal(result.totalChamberVolumeMl, 2),
                    ),
                    hint = stringResource(R.string.compression_target_result_total_chamber_hint),
                )
                if (result.domeVolumeMl != null) {
                    CalculatorResultRow(
                        label = stringResource(R.string.compression_target_result_dome),
                        value = stringResource(
                            R.string.compression_result_value_ml,
                            formatDecimal(result.domeVolumeMl, 2),
                        ),
                        hint = stringResource(R.string.compression_target_result_dome_hint),
                    )
                }
            }
        }
    }
}

@Composable
private fun ChangeResultCard(
    result: CompressionChangeResult?,
    hasInput: Boolean,
) {
    CalculatorResultCard(title = stringResource(R.string.compression_result_title)) {
        when {
            !hasInput -> com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorEmptyResultText(
                stringResource(R.string.compression_change_result_empty),
            )
            result == null -> CalculatorInvalidResultText(stringResource(R.string.compression_change_result_invalid))
            else -> {
                val volumeToRemove = result.volumeToRemoveMl
                val removingVolume = volumeToRemove >= 0.0
                CalculatorPrimaryResultText(
                    text = if (removingVolume) {
                        stringResource(
                            R.string.compression_change_result_remove_depth,
                            formatDecimal(result.millingDepthMm, 2),
                        )
                    } else {
                        stringResource(
                            R.string.compression_change_result_add_depth,
                            formatDecimal(result.millingDepthMm, 2),
                        )
                    },
                )
                Text(
                    text = stringResource(R.string.compression_change_result_depth_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.textSecondary(),
                )
                CalculatorResultDivider()
                CalculatorResultRow(
                    label = if (removingVolume) {
                        stringResource(R.string.compression_change_result_volume_remove)
                    } else {
                        stringResource(R.string.compression_change_result_volume_add)
                    },
                    value = stringResource(
                        R.string.compression_result_value_ml,
                        formatDecimal(kotlin.math.abs(volumeToRemove), 2),
                    ),
                    hint = if (removingVolume) {
                        stringResource(R.string.compression_change_result_remove_hint)
                    } else {
                        stringResource(R.string.compression_change_result_add_hint)
                    },
                )
                CalculatorResultDivider()
                CalculatorResultRow(
                    label = stringResource(R.string.compression_result_displacement),
                    value = stringResource(
                        R.string.compression_result_value_ml,
                        formatDecimal(result.displacementMl, 2),
                    ),
                )
                CalculatorResultRow(
                    label = stringResource(
                        R.string.compression_change_result_current_chamber,
                        formatDecimal(result.currentCompressionRatio, 2),
                    ),
                    value = stringResource(
                        R.string.compression_result_value_ml,
                        formatDecimal(result.currentChamberVolumeMl, 2),
                    ),
                )
                CalculatorResultRow(
                    label = stringResource(
                        R.string.compression_change_result_target_chamber,
                        formatDecimal(result.targetCompressionRatio, 2),
                    ),
                    value = stringResource(
                        R.string.compression_result_value_ml,
                        formatDecimal(result.targetChamberVolumeMl, 2),
                    ),
                )
            }
        }
    }
}

private fun formatDecimal(value: Double, decimals: Int): String =
    String.format(Locale.getDefault(), "%.${decimals}f", value)
