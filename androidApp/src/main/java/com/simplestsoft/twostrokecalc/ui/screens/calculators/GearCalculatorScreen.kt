package com.simplestsoft.twostrokecalc.ui.screens.calculators

import android.content.res.Configuration
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.ui.draw.clip
import com.simplestsoft.twostrokecalc.ui.util.DialogFullscreenEffect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.sp
import kotlin.math.ceil
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import com.simplestsoft.twostrokecalc.R
import com.simplestsoft.twostrokecalc.domain.calculation.GearCalculator
import com.simplestsoft.twostrokecalc.domain.calculation.GearCalculatorInput
import com.simplestsoft.twostrokecalc.domain.calculation.GearCalculatorResult
import com.simplestsoft.twostrokecalc.domain.calculation.GearDriveMode
import com.simplestsoft.twostrokecalc.domain.calculation.GearOutputType
import com.simplestsoft.twostrokecalc.domain.calculation.GearReferenceLabel
import com.simplestsoft.twostrokecalc.domain.calculation.GearStageConfig
import com.simplestsoft.twostrokecalc.domain.calculation.GearStageResult
import com.simplestsoft.twostrokecalc.domain.model.GearPreset
import com.simplestsoft.twostrokecalc.domain.model.labelRes
import com.simplestsoft.twostrokecalc.domain.model.values
import com.simplestsoft.twostrokecalc.ui.components.AppColors
import com.simplestsoft.twostrokecalc.ui.components.FormFieldDefaults
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorDecimalField
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorEmptyResultText
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorChoiceField
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorDropdownOption
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorFilterChip
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorFilterChipRow
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorHeader
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorInvalidResultText
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorResultCard
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorResultDivider
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorResultRow
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorScaffold
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorSection
import com.simplestsoft.twostrokecalc.ui.components.calculator.filterDecimalInput
import com.simplestsoft.twostrokecalc.ui.components.calculator.parseDecimal
import java.util.Locale

private data class GearStageUiState(
    val pinionText: String,
    val gearText: String,
    val referenceRpmText: String,
)

private fun defaultGearStages(): List<GearStageUiState> = listOf(
    GearStageUiState("12", "57", "3000"),
    GearStageUiState("13", "42", "9500"),
    GearStageUiState("17", "38", "6000"),
    GearStageUiState("21", "35", "8500"),
)

private fun encodeGearStages(stages: List<GearStageUiState>): String =
    stages.joinToString(";") { "${it.pinionText}|${it.gearText}|${it.referenceRpmText}" }

private fun decodeGearStages(encoded: String): List<GearStageUiState> {
    if (encoded.isBlank()) return defaultGearStages()
    return encoded.split(";").mapNotNull { part ->
        val tokens = part.split("|")
        if (tokens.size >= 3) {
            GearStageUiState(tokens[0], tokens[1], tokens[2])
        } else {
            null
        }
    }.takeIf { it.isNotEmpty() } ?: defaultGearStages()
}

@Composable
fun GearCalculatorScreen() {
    var driveModeName by rememberSaveable { mutableStateOf(GearDriveMode.MULTI_SPEED.name) }
    val driveMode = remember(driveModeName) {
        GearDriveMode.entries.firstOrNull { it.name == driveModeName } ?: GearDriveMode.MULTI_SPEED
    }

    var outputTypeName by rememberSaveable { mutableStateOf(GearOutputType.VEHICLE_SPEED.name) }
    val outputType = remember(outputTypeName) {
        GearOutputType.entries.firstOrNull { it.name == outputTypeName } ?: GearOutputType.VEHICLE_SPEED
    }

    var presetName by rememberSaveable { mutableStateOf(GearPreset.VESPA_PX_200.name) }
    val preset = remember(presetName) {
        GearPreset.entries.firstOrNull { it.name == presetName } ?: GearPreset.VESPA_PX_200
    }

    var wheelCircumferenceText by rememberSaveable { mutableStateOf("1307") }
    var shiftRpmText by rememberSaveable { mutableStateOf("8000") }
    var referenceLowRpmText by rememberSaveable { mutableStateOf("3000") }
    var referenceHighRpmText by rememberSaveable { mutableStateOf("9500") }
    var resonanceEntryRpmText by rememberSaveable { mutableStateOf("6000") }
    var resonancePeakRpmText by rememberSaveable { mutableStateOf("8500") }

    var primaryPinionText by rememberSaveable { mutableStateOf("23") }
    var primaryGearText by rememberSaveable { mutableStateOf("64") }

    var fixedPinionText by rememberSaveable { mutableStateOf("12") }
    var fixedGearText by rememberSaveable { mutableStateOf("57") }

    var stagesEncoded by rememberSaveable {
        mutableStateOf(encodeGearStages(defaultGearStages()))
    }
    val gearStages = decodeGearStages(stagesEncoded)

    fun markCustom() {
        presetName = GearPreset.CUSTOM.name
    }

    fun updateGearStages(transform: (List<GearStageUiState>) -> List<GearStageUiState>) {
        stagesEncoded = encodeGearStages(transform(gearStages))
        markCustom()
    }

    fun applyPreset(selected: GearPreset) {
        presetName = selected.name
        driveModeName = GearDriveMode.MULTI_SPEED.name
        outputTypeName = GearOutputType.VEHICLE_SPEED.name
        selected.values()?.let { values ->
            primaryPinionText = values.primaryPinion.toString()
            primaryGearText = values.primaryGear.toString()
            stagesEncoded = encodeGearStages(
                values.stages.mapIndexed { index, teeth ->
                    GearStageUiState(
                        pinionText = teeth.secondaryPinion.toString(),
                        gearText = teeth.secondaryGear.toString(),
                        referenceRpmText = GearCalculator.defaultReferenceRpm(index).toLong().toString(),
                    )
                },
            )
        }
    }

    val wheelCircumference = parseDecimal(wheelCircumferenceText)
    val shiftRpm = parseDecimal(shiftRpmText)
    val referenceLowRpm = parseDecimal(referenceLowRpmText)
    val referenceHighRpm = parseDecimal(referenceHighRpmText)
    val resonanceEntryRpm = parseDecimal(resonanceEntryRpmText)
    val resonancePeakRpm = parseDecimal(resonancePeakRpmText)
    val primaryPinion = parsePositiveInteger(primaryPinionText)
    val primaryGear = parsePositiveInteger(primaryGearText)
    val fixedPinion = parsePositiveInteger(fixedPinionText)
    val fixedGear = parsePositiveInteger(fixedGearText)

    val parsedStages = gearStages.mapNotNull { stage ->
        val pinion = parsePositiveInteger(stage.pinionText)
        val gear = parsePositiveInteger(stage.gearText)
        val referenceRpm = parseDecimal(stage.referenceRpmText)
        if (pinion != null && gear != null && referenceRpm != null && referenceRpm > 0.0) {
            GearStageConfig(
                secondaryPinion = pinion,
                secondaryGear = gear,
                referenceRpm = referenceRpm,
            )
        } else {
            null
        }
    }

    val fixedReferenceRpms = listOfNotNull(
        referenceLowRpm,
        referenceHighRpm,
        resonanceEntryRpm,
        resonancePeakRpm,
    )

    val hasMultiSpeedInput = wheelCircumference != null &&
        shiftRpm != null &&
        primaryPinion != null &&
        primaryGear != null &&
        parsedStages.size == gearStages.size &&
        parsedStages.size in GearCalculator.MIN_STAGE_COUNT..GearCalculator.MAX_STAGE_COUNT

    val hasFixedInput = fixedPinion != null &&
        fixedGear != null &&
        fixedReferenceRpms.size == 4 &&
        (outputType == GearOutputType.OUTPUT_RPM || wheelCircumference != null) &&
        (driveMode != GearDriveMode.FIXED_TWO_STAGE || (primaryPinion != null && primaryGear != null))

    val hasInput = when (driveMode) {
        GearDriveMode.MULTI_SPEED -> hasMultiSpeedInput
        GearDriveMode.FIXED_TWO_STAGE,
        GearDriveMode.SINGLE_STAGE,
        -> hasFixedInput
    }

    val result = if (hasInput) {
        when (driveMode) {
            GearDriveMode.MULTI_SPEED -> GearCalculator.calculate(
                GearCalculatorInput(
                    driveMode = driveMode,
                    outputType = GearOutputType.VEHICLE_SPEED,
                    wheelCircumferenceMm = wheelCircumference!!,
                    rpmConstant = GearCalculator.DEFAULT_RPM_CONSTANT,
                    shiftRpm = shiftRpm!!,
                    primaryPinion = primaryPinion!!,
                    primaryGear = primaryGear!!,
                    stages = parsedStages,
                ),
            )
            GearDriveMode.FIXED_TWO_STAGE -> GearCalculator.calculate(
                GearCalculatorInput(
                    driveMode = driveMode,
                    outputType = outputType,
                    wheelCircumferenceMm = wheelCircumference ?: 0.0,
                    rpmConstant = GearCalculator.DEFAULT_RPM_CONSTANT,
                    shiftRpm = 0.0,
                    primaryPinion = primaryPinion!!,
                    primaryGear = primaryGear!!,
                    stages = listOf(
                        GearStageConfig(
                            secondaryPinion = fixedPinion!!,
                            secondaryGear = fixedGear!!,
                            referenceRpm = fixedReferenceRpms[0],
                        ),
                    ),
                    fixedReferenceRpms = fixedReferenceRpms,
                ),
            )
            GearDriveMode.SINGLE_STAGE -> GearCalculator.calculate(
                GearCalculatorInput(
                    driveMode = driveMode,
                    outputType = outputType,
                    wheelCircumferenceMm = wheelCircumference ?: 0.0,
                    rpmConstant = GearCalculator.DEFAULT_RPM_CONSTANT,
                    shiftRpm = 0.0,
                    primaryPinion = 1,
                    primaryGear = 1,
                    stages = listOf(
                        GearStageConfig(
                            secondaryPinion = fixedPinion!!,
                            secondaryGear = fixedGear!!,
                            referenceRpm = fixedReferenceRpms[0],
                        ),
                    ),
                    fixedReferenceRpms = fixedReferenceRpms,
                ),
            )
        }
    } else {
        null
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val chartDataReady = result != null && when (driveMode) {
        GearDriveMode.MULTI_SPEED -> shiftRpm != null && wheelCircumference != null
        else -> outputType == GearOutputType.OUTPUT_RPM || wheelCircumference != null
    }

    var chartStyleName by rememberSaveable { mutableStateOf(GearChartStyle.LINE.name) }
    var selectedShiftPointId by rememberSaveable { mutableStateOf<Int?>(null) }

    if (isLandscape) {
        GearChartLandscapeOverlay(
            visible = true,
            chartDataReady = chartDataReady,
            result = result,
            shiftRpm = if (driveMode == GearDriveMode.MULTI_SPEED) shiftRpm else null,
            wheelCircumferenceMm = wheelCircumference,
            resonanceEntryRpm = resonanceEntryRpm,
            resonancePeakRpm = resonancePeakRpm,
            chartStyleName = chartStyleName,
            onChartStyleNameChange = { chartStyleName = it },
            selectedShiftPointId = selectedShiftPointId,
            onSelectedShiftPointIdChange = { selectedShiftPointId = it },
        )
    }

    if (!isLandscape) {
        CalculatorScaffold {
        CalculatorHeader(
            title = stringResource(R.string.gear_calculator_title),
            subtitle = stringResource(R.string.gear_calculator_subtitle),
        )

        CalculatorSection(title = stringResource(R.string.gear_section_drive_mode)) {
            val driveModeOptions = GearDriveMode.entries.map { mode ->
                CalculatorDropdownOption(mode.name, stringResource(mode.labelRes()))
            }
            CalculatorChoiceField(
                label = "",
                options = driveModeOptions,
                selectedKey = driveMode.name,
                onOptionSelected = { key ->
                    val mode = GearDriveMode.entries.first { it.name == key }
                    driveModeName = mode.name
                    when (mode) {
                        GearDriveMode.MULTI_SPEED -> outputTypeName = GearOutputType.VEHICLE_SPEED.name
                        GearDriveMode.SINGLE_STAGE -> outputTypeName = GearOutputType.OUTPUT_RPM.name
                        GearDriveMode.FIXED_TWO_STAGE -> Unit
                    }
                    markCustom()
                },
                supportingText = stringResource(driveMode.hintRes()),
            )
        }

        if (driveMode == GearDriveMode.MULTI_SPEED) {
            CalculatorSection(title = stringResource(R.string.gear_presets_label)) {
                val presetOptions = GearPreset.entries.map { item ->
                    CalculatorDropdownOption(item.name, stringResource(item.labelRes()))
                }
                CalculatorChoiceField(
                    label = "",
                    options = presetOptions,
                    selectedKey = preset.name,
                    onOptionSelected = { key ->
                        GearPreset.entries.firstOrNull { it.name == key }?.let { applyPreset(it) }
                    },
                )
            }
        }

        if (driveMode != GearDriveMode.MULTI_SPEED) {
            CalculatorSection(title = stringResource(R.string.gear_section_output)) {
                val outputTypeOptions = listOf(
                    CalculatorDropdownOption(
                        GearOutputType.VEHICLE_SPEED.name,
                        stringResource(R.string.gear_output_vehicle_speed),
                    ),
                    CalculatorDropdownOption(
                        GearOutputType.OUTPUT_RPM.name,
                        stringResource(R.string.gear_output_shaft_rpm),
                    ),
                )
                CalculatorChoiceField(
                    label = "",
                    options = outputTypeOptions,
                    selectedKey = outputType.name,
                    onOptionSelected = {
                        outputTypeName = it
                        markCustom()
                    },
                    supportingText = stringResource(
                        if (outputType == GearOutputType.VEHICLE_SPEED) {
                            R.string.gear_output_vehicle_speed_hint
                        } else {
                            R.string.gear_output_shaft_rpm_hint
                        },
                    ),
                )
            }
        }

        val showWheelCircumference =
            driveMode == GearDriveMode.MULTI_SPEED || outputType == GearOutputType.VEHICLE_SPEED
        val showShiftRpm = driveMode == GearDriveMode.MULTI_SPEED
        if (showWheelCircumference || showShiftRpm) {
            CalculatorSection(title = stringResource(R.string.gear_section_global)) {
                if (showWheelCircumference) {
                    CalculatorDecimalField(
                        value = wheelCircumferenceText,
                        onValueChange = {
                            wheelCircumferenceText = it.filterDecimalInput()
                            markCustom()
                        },
                        label = stringResource(R.string.gear_wheel_circumference_label),
                        suffix = stringResource(R.string.gear_unit_mm),
                    )
                }
                if (showShiftRpm) {
                    CalculatorDecimalField(
                        value = shiftRpmText,
                        onValueChange = {
                            shiftRpmText = it.filterDecimalInput()
                            markCustom()
                        },
                        label = stringResource(R.string.gear_shift_rpm_label),
                        suffix = stringResource(R.string.gear_unit_rpm),
                    )
                }
            }
        }

        if (driveMode != GearDriveMode.SINGLE_STAGE) {
            CalculatorSection(title = stringResource(R.string.gear_section_primary)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CalculatorDecimalField(
                        value = primaryPinionText,
                        onValueChange = {
                            primaryPinionText = it.filterIntegerInput()
                            markCustom()
                        },
                        label = stringResource(R.string.gear_primary_pinion_label),
                        suffix = stringResource(R.string.gear_unit_teeth),
                        modifier = Modifier.weight(1f),
                    )
                    CalculatorDecimalField(
                        value = primaryGearText,
                        onValueChange = {
                            primaryGearText = it.filterIntegerInput()
                            markCustom()
                        },
                        label = stringResource(R.string.gear_primary_gear_label),
                        suffix = stringResource(R.string.gear_unit_teeth),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        when (driveMode) {
            GearDriveMode.MULTI_SPEED -> {
                CalculatorSection(title = stringResource(R.string.gear_section_stages)) {
                    Text(
                        text = stringResource(
                            R.string.gear_stage_count_hint,
                            GearCalculator.MIN_STAGE_COUNT,
                            GearCalculator.MAX_STAGE_COUNT,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.textSecondary(),
                    )
                    gearStages.forEachIndexed { index, stage ->
                        GearStageInputRow(
                            stageNumber = index + 1,
                            state = stage,
                            onStateChange = { updated ->
                                updateGearStages { stages ->
                                    stages.toMutableList().also { it[index] = updated }
                                }
                            },
                            canRemove = gearStages.size > GearCalculator.MIN_STAGE_COUNT,
                            onRemove = {
                                updateGearStages { stages ->
                                    stages.toMutableList().also { it.removeAt(index) }
                                }
                            },
                        )
                    }
                    if (gearStages.size < GearCalculator.MAX_STAGE_COUNT) {
                        TextButton(
                            onClick = {
                                updateGearStages { stages ->
                                    val last = stages.lastOrNull()
                                    stages + GearStageUiState(
                                        pinionText = last?.pinionText.orEmpty(),
                                        gearText = last?.gearText.orEmpty(),
                                        referenceRpmText = GearCalculator
                                            .defaultReferenceRpm(stages.size)
                                            .toLong()
                                            .toString(),
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.gear_add_stage))
                        }
                    }
                }
            }
            GearDriveMode.FIXED_TWO_STAGE -> {
                CalculatorSection(title = stringResource(R.string.gear_section_fixed_secondary)) {
                    GearSimpleRatioInputRow(
                        pinionText = fixedPinionText,
                        onPinionChange = {
                            fixedPinionText = it
                            markCustom()
                        },
                        gearText = fixedGearText,
                        onGearChange = {
                            fixedGearText = it
                            markCustom()
                        },
                    )
                }
                FixedReferenceRpmFields(
                    referenceLowRpmText = referenceLowRpmText,
                    onReferenceLowChange = {
                        referenceLowRpmText = it
                        markCustom()
                    },
                    referenceHighRpmText = referenceHighRpmText,
                    onReferenceHighChange = {
                        referenceHighRpmText = it
                        markCustom()
                    },
                    resonanceEntryRpmText = resonanceEntryRpmText,
                    onResonanceEntryChange = {
                        resonanceEntryRpmText = it
                        markCustom()
                    },
                    resonancePeakRpmText = resonancePeakRpmText,
                    onResonancePeakChange = {
                        resonancePeakRpmText = it
                        markCustom()
                    },
                )
            }
            GearDriveMode.SINGLE_STAGE -> {
                CalculatorSection(title = stringResource(R.string.gear_section_single_gear)) {
                    GearSimpleRatioInputRow(
                        pinionText = fixedPinionText,
                        onPinionChange = {
                            fixedPinionText = it
                            markCustom()
                        },
                        gearText = fixedGearText,
                        onGearChange = {
                            fixedGearText = it
                            markCustom()
                        },
                    )
                }
                FixedReferenceRpmFields(
                    referenceLowRpmText = referenceLowRpmText,
                    onReferenceLowChange = {
                        referenceLowRpmText = it
                        markCustom()
                    },
                    referenceHighRpmText = referenceHighRpmText,
                    onReferenceHighChange = {
                        referenceHighRpmText = it
                        markCustom()
                    },
                    resonanceEntryRpmText = resonanceEntryRpmText,
                    onResonanceEntryChange = {
                        resonanceEntryRpmText = it
                        markCustom()
                    },
                    resonancePeakRpmText = resonancePeakRpmText,
                    onResonancePeakChange = {
                        resonancePeakRpmText = it
                        markCustom()
                    },
                )
            }
        }

        GearResultsCard(result = result, hasInput = hasInput)

        if (chartDataReady) {
            GearSpeedChart(
                result = result!!,
                shiftRpm = if (driveMode == GearDriveMode.MULTI_SPEED) shiftRpm else null,
                wheelCircumferenceMm = wheelCircumference,
                resonanceEntryRpm = resonanceEntryRpm,
                resonancePeakRpm = resonancePeakRpm,
                displayMode = GearChartDisplayMode.EMBEDDED,
                chartStyleName = chartStyleName,
                onChartStyleNameChange = { chartStyleName = it },
                selectedShiftPointId = selectedShiftPointId,
                onSelectedShiftPointIdChange = { selectedShiftPointId = it },
            )
        }

        Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun GearSimpleRatioInputRow(
    pinionText: String,
    onPinionChange: (String) -> Unit,
    gearText: String,
    onGearChange: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CalculatorDecimalField(
            value = pinionText,
            onValueChange = { onPinionChange(it.filterIntegerInput()) },
            label = stringResource(R.string.gear_secondary_pinion_label),
            suffix = stringResource(R.string.gear_unit_teeth),
            modifier = Modifier.weight(1f),
        )
        CalculatorDecimalField(
            value = gearText,
            onValueChange = { onGearChange(it.filterIntegerInput()) },
            label = stringResource(R.string.gear_secondary_gear_label),
            suffix = stringResource(R.string.gear_unit_teeth),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun FixedReferenceRpmFields(
    referenceLowRpmText: String,
    onReferenceLowChange: (String) -> Unit,
    referenceHighRpmText: String,
    onReferenceHighChange: (String) -> Unit,
    resonanceEntryRpmText: String,
    onResonanceEntryChange: (String) -> Unit,
    resonancePeakRpmText: String,
    onResonancePeakChange: (String) -> Unit,
) {
    CalculatorSection(title = stringResource(R.string.gear_section_fixed_reference)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CalculatorDecimalField(
                value = referenceLowRpmText,
                onValueChange = { onReferenceLowChange(it.filterDecimalInput()) },
                label = stringResource(R.string.gear_reference_low_rpm_label),
                suffix = stringResource(R.string.gear_unit_rpm),
                modifier = Modifier.weight(1f),
            )
            CalculatorDecimalField(
                value = referenceHighRpmText,
                onValueChange = { onReferenceHighChange(it.filterDecimalInput()) },
                label = stringResource(R.string.gear_reference_high_rpm_label),
                suffix = stringResource(R.string.gear_unit_rpm),
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CalculatorDecimalField(
                value = resonanceEntryRpmText,
                onValueChange = { onResonanceEntryChange(it.filterDecimalInput()) },
                label = stringResource(R.string.gear_resonance_entry_rpm_label),
                suffix = stringResource(R.string.gear_unit_rpm),
                modifier = Modifier.weight(1f),
            )
            CalculatorDecimalField(
                value = resonancePeakRpmText,
                onValueChange = { onResonancePeakChange(it.filterDecimalInput()) },
                label = stringResource(R.string.gear_resonance_peak_rpm_label),
                suffix = stringResource(R.string.gear_unit_rpm),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private fun GearDriveMode.labelRes(): Int = when (this) {
    GearDriveMode.MULTI_SPEED -> R.string.gear_drive_mode_multi_speed
    GearDriveMode.FIXED_TWO_STAGE -> R.string.gear_drive_mode_fixed_two_stage
    GearDriveMode.SINGLE_STAGE -> R.string.gear_drive_mode_single_stage
}

private fun GearDriveMode.hintRes(): Int = when (this) {
    GearDriveMode.MULTI_SPEED -> R.string.gear_drive_mode_multi_speed_hint
    GearDriveMode.FIXED_TWO_STAGE -> R.string.gear_drive_mode_fixed_hint
    GearDriveMode.SINGLE_STAGE -> R.string.gear_drive_mode_single_hint
}

@Composable
private fun GearStageInputRow(
    stageNumber: Int,
    state: GearStageUiState,
    onStateChange: (GearStageUiState) -> Unit,
    canRemove: Boolean,
    onRemove: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(FormFieldDefaults.fieldSpacing)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.gear_stage_label, stageNumber),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.textPrimary(),
                modifier = Modifier.weight(1f),
            )
            if (canRemove) {
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = stringResource(R.string.gear_remove_stage),
                        tint = AppColors.textSecondary(),
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CalculatorDecimalField(
                value = state.pinionText,
                onValueChange = { onStateChange(state.copy(pinionText = it.filterIntegerInput())) },
                label = stringResource(R.string.gear_secondary_pinion_label),
                suffix = stringResource(R.string.gear_unit_teeth),
                modifier = Modifier.weight(1f),
            )
            CalculatorDecimalField(
                value = state.gearText,
                onValueChange = { onStateChange(state.copy(gearText = it.filterIntegerInput())) },
                label = stringResource(R.string.gear_secondary_gear_label),
                suffix = stringResource(R.string.gear_unit_teeth),
                modifier = Modifier.weight(1f),
            )
        }
        CalculatorDecimalField(
            value = state.referenceRpmText,
            onValueChange = { onStateChange(state.copy(referenceRpmText = it.filterDecimalInput())) },
            label = stringResource(R.string.gear_stage_reference_rpm_label),
            suffix = stringResource(R.string.gear_unit_rpm),
        )
    }
}

@Composable
private fun gearStageColors(): List<Color> = listOf(
    AppColors.primaryBlue(),
    Color(0xFF4CAF50),
    Color(0xFFFF9800),
    Color(0xFFE91E63),
    Color(0xFF9C27B0),
    Color(0xFF00BCD4),
)

private fun gearChartResoBandColor(): Color = Color(0xFFFF9800).copy(alpha = 0.16f)

@Composable
private fun GearResultsCard(
    result: GearCalculatorResult?,
    hasInput: Boolean,
) {
    val stageColors = gearStageColors()
    CalculatorResultCard(title = stringResource(R.string.gear_result_title)) {
        when {
            !hasInput -> CalculatorEmptyResultText(stringResource(R.string.gear_result_empty))
            result == null -> CalculatorInvalidResultText(stringResource(R.string.gear_result_invalid))
            else -> {
                if (result.driveMode != GearDriveMode.MULTI_SPEED) {
                    Text(
                        text = stringResource(
                            if (result.outputType == GearOutputType.OUTPUT_RPM) {
                                R.string.gear_result_mode_output_rpm
                            } else {
                                R.string.gear_result_mode_vehicle_speed
                            },
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.primaryBlue(),
                    )
                }
                when (result.driveMode) {
                    GearDriveMode.MULTI_SPEED -> GearMultiSpeedResults(
                        stages = result.stages,
                        outputType = result.outputType,
                        stageColors = stageColors,
                    )
                    else -> GearFixedRatioResults(
                        stage = result.stages.first(),
                        outputType = result.outputType,
                        referenceRpms = result.fixedReferenceRpms,
                    )
                }
            }
        }
    }
}

@Composable
private fun GearFixedRatioResults(
    stage: GearStageResult,
    outputType: GearOutputType,
    referenceRpms: List<Double>,
) {
    val labels = listOf(
        GearReferenceLabel.LOW,
        GearReferenceLabel.HIGH,
        GearReferenceLabel.RESONANCE_ENTRY,
        GearReferenceLabel.RESONANCE_PEAK,
    )
    CalculatorResultRow(
        label = stringResource(R.string.gear_result_ratio),
        value = formatDecimal(stage.gearRatio, 2),
    )
    stage.speedsAtReferenceRpmKmh.forEachIndexed { index, value ->
        val rpm = referenceRpms.getOrNull(index) ?: return@forEachIndexed
        val label = labels.getOrElse(index) { GearReferenceLabel.ADDITIONAL }
        CalculatorResultRow(
            label = referenceOutputLabel(label, outputType),
            value = formatGearOutputValue(value, outputType),
            hint = stringResource(
                R.string.gear_result_rpm_value,
                formatDecimal(rpm, 0),
            ),
        )
    }
}

@Composable
private fun GearMultiSpeedResults(
    stages: List<GearStageResult>,
    outputType: GearOutputType,
    stageColors: List<Color>,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        stages.forEachIndexed { index, stage ->
            if (index > 0) {
                CalculatorResultDivider()
            }
            GearStageResultBlock(
                stage = stage,
                outputType = outputType,
                accentColor = stageColors.getOrElse(index) { AppColors.primaryBlue() },
            )
        }
    }
}

@Composable
private fun GearStageResultBlock(
    stage: GearStageResult,
    outputType: GearOutputType,
    accentColor: Color,
) {
    val refSpeed = stage.speedsAtReferenceRpmKmh[stage.stageNumber - 1]
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GearStageBadge(
                stageNumber = stage.stageNumber,
                color = accentColor,
            )
            Text(
                text = stringResource(R.string.gear_result_stage_header, stage.stageNumber),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.textPrimary(),
            )
        }
        CalculatorResultRow(
            label = stringResource(R.string.gear_result_ratio),
            value = formatDecimal(stage.gearRatio, 2),
        )
        CalculatorResultRow(
            label = stringResource(R.string.gear_result_shift_speed),
            value = formatGearOutputValue(stage.shiftSpeedKmh, outputType),
        )
        CalculatorResultRow(
            label = referenceRpmShortLabel(stage.referenceLabel),
            value = formatGearOutputValue(refSpeed, outputType),
            hint = stringResource(
                R.string.gear_result_rpm_value,
                formatDecimal(stage.referenceRpm, 0),
            ),
        )
        if (stage.speedJumpKmh != null && stage.rpmJump != null) {
            val speedJumpKmh = stage.speedJumpKmh!!
            val rpmJump = stage.rpmJump!!
            CalculatorResultRow(
                label = stringResource(R.string.gear_result_speed_jump),
                value = stringResource(
                    R.string.gear_result_speed_kmh,
                    formatDecimal(speedJumpKmh, 1),
                ),
            )
            CalculatorResultRow(
                label = stringResource(R.string.gear_result_rpm_jump),
                value = stringResource(
                    R.string.gear_result_rpm_value,
                    formatDecimal(rpmJump, 0),
                ),
            )
        }
    }
}

@Composable
private fun GearStageBadge(
    stageNumber: Int,
    color: Color,
    size: Dp = 24.dp,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stageNumber.toString(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = if (size < 24.dp) 10.sp else MaterialTheme.typography.labelSmall.fontSize,
            ),
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
    }
}

@Composable
private fun referenceRpmShortLabel(label: GearReferenceLabel): String = when (label) {
    GearReferenceLabel.LOW -> stringResource(R.string.gear_result_ref_low_short)
    GearReferenceLabel.HIGH -> stringResource(R.string.gear_result_ref_high_short)
    GearReferenceLabel.RESONANCE_ENTRY -> stringResource(R.string.gear_result_ref_reso_entry_short)
    GearReferenceLabel.RESONANCE_PEAK -> stringResource(R.string.gear_result_ref_reso_peak_short)
    GearReferenceLabel.ADDITIONAL -> stringResource(R.string.gear_result_ref_additional_short)
}

@Composable
private fun referenceOutputLabel(label: GearReferenceLabel, outputType: GearOutputType): String =
    when (outputType) {
        GearOutputType.VEHICLE_SPEED -> when (label) {
            GearReferenceLabel.LOW -> stringResource(R.string.gear_result_speed_low_ref)
            GearReferenceLabel.HIGH -> stringResource(R.string.gear_result_speed_high_ref)
            GearReferenceLabel.RESONANCE_ENTRY -> stringResource(R.string.gear_result_speed_reso_entry)
            GearReferenceLabel.RESONANCE_PEAK -> stringResource(R.string.gear_result_speed_reso_peak)
            GearReferenceLabel.ADDITIONAL -> stringResource(R.string.gear_result_ref_additional_short)
        }
        GearOutputType.OUTPUT_RPM -> when (label) {
            GearReferenceLabel.LOW -> stringResource(R.string.gear_result_output_low_ref)
            GearReferenceLabel.HIGH -> stringResource(R.string.gear_result_output_high_ref)
            GearReferenceLabel.RESONANCE_ENTRY -> stringResource(R.string.gear_result_output_reso_entry_ref)
            GearReferenceLabel.RESONANCE_PEAK -> stringResource(R.string.gear_result_output_reso_peak_ref)
            GearReferenceLabel.ADDITIONAL -> stringResource(R.string.gear_result_ref_additional_short)
        }
    }

@Composable
private fun gearChartLineLegendLabels(
    result: GearCalculatorResult,
    isMultiSpeed: Boolean,
): List<String> =
    if (isMultiSpeed) {
        result.stages.map { stage ->
            stringResource(R.string.gear_chart_legend_gear, stage.stageNumber)
        }
    } else {
        listOf(stringResource(R.string.gear_chart_legend_fixed_ratio))
    }

@Composable
private fun GearChartLandscapeOverlay(
    visible: Boolean,
    chartDataReady: Boolean,
    result: GearCalculatorResult?,
    shiftRpm: Double?,
    wheelCircumferenceMm: Double?,
    resonanceEntryRpm: Double?,
    resonancePeakRpm: Double?,
    chartStyleName: String,
    onChartStyleNameChange: (String) -> Unit,
    selectedShiftPointId: Int?,
    onSelectedShiftPointIdChange: (Int?) -> Unit,
) {
    if (!visible) return

    val isMultiSpeed = result?.driveMode == GearDriveMode.MULTI_SPEED
    val chartStyle = resolveGearChartStyle(chartStyleName, isMultiSpeed)
    val gearColors = gearStageColors()
    val resonanceRpmRange = result?.let {
        GearCalculator.resolveResonanceRpmRange(
            result = it,
            fallbackEntryRpm = resonanceEntryRpm,
            fallbackPeakRpm = resonancePeakRpm,
        )
    }
    val showResoBand = chartStyle == GearChartStyle.LINE && resonanceRpmRange != null

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
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = AppColors.background(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.gear_chart_fullscreen_title),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.textPrimary(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    CalculatorFilterChip(
                        label = stringResource(R.string.gear_chart_style_line),
                        selected = chartStyle == GearChartStyle.LINE,
                        onClick = { onChartStyleNameChange(GearChartStyle.LINE.name) },
                    )
                    if (isMultiSpeed) {
                        Spacer(modifier = Modifier.width(6.dp))
                        CalculatorFilterChip(
                            label = stringResource(R.string.gear_chart_style_bar),
                            selected = chartStyle == GearChartStyle.BAR,
                            onClick = { onChartStyleNameChange(GearChartStyle.BAR.name) },
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.gear_chart_fullscreen_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.textSecondary(),
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp, bottom = 4.dp),
                )

                if (chartDataReady && result != null) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth(GEAR_CHART_FULLSCREEN_WIDTH_FRACTION)
                                .fillMaxHeight(),
                        ) {
                            GearSpeedChart(
                                result = result,
                                shiftRpm = shiftRpm,
                                wheelCircumferenceMm = wheelCircumferenceMm,
                                resonanceEntryRpm = resonanceEntryRpm,
                                resonancePeakRpm = resonancePeakRpm,
                                displayMode = GearChartDisplayMode.FULLSCREEN,
                                chartStyleName = chartStyleName,
                                onChartStyleNameChange = onChartStyleNameChange,
                                selectedShiftPointId = selectedShiftPointId,
                                onSelectedShiftPointIdChange = onSelectedShiftPointIdChange,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .fillMaxHeight(),
                            )

                            GearChartLegend(
                                colors = if (chartStyle == GearChartStyle.LINE) gearColors else gearColors,
                                labels = if (chartStyle == GearChartStyle.LINE) {
                                    gearChartLineLegendLabels(result, isMultiSpeed)
                                } else {
                                    result.stages.map { stage ->
                                        referenceRpmShortLabel(stage.referenceLabel)
                                    } + stringResource(R.string.gear_chart_legend_shift)
                                },
                                showLineStyle = chartStyle == GearChartStyle.LINE,
                                showShiftPointMarker = isMultiSpeed,
                                resoBandColor = if (showResoBand) gearChartResoBandColor() else null,
                                resoBandLabel = if (showResoBand) {
                                    stringResource(R.string.gear_chart_legend_reso_band)
                                } else {
                                    null
                                },
                                horizontal = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.gear_chart_fullscreen_empty),
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

private enum class GearChartDisplayMode {
    EMBEDDED,
    FULLSCREEN,
}

@Composable
private fun GearSpeedChart(
    result: GearCalculatorResult,
    shiftRpm: Double?,
    wheelCircumferenceMm: Double?,
    resonanceEntryRpm: Double?,
    resonancePeakRpm: Double?,
    displayMode: GearChartDisplayMode,
    chartStyleName: String,
    onChartStyleNameChange: (String) -> Unit,
    selectedShiftPointId: Int?,
    onSelectedShiftPointIdChange: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isMultiSpeed = result.driveMode == GearDriveMode.MULTI_SPEED
    val chartStyle = resolveGearChartStyle(chartStyleName, isMultiSpeed)
    val resonanceRpmRange = GearCalculator.resolveResonanceRpmRange(
        result = result,
        fallbackEntryRpm = resonanceEntryRpm,
        fallbackPeakRpm = resonancePeakRpm,
    )
    val showResoBand = chartStyle == GearChartStyle.LINE && resonanceRpmRange != null
    val resoBandColor = gearChartResoBandColor()

    val gearColors = gearStageColors()
    val referenceColors = gearColors
    val shiftColor = Color.White.copy(alpha = 0.85f)
    val gridColor = AppColors.borderSubtle().copy(alpha = 0.45f)
    val axisLabelColor = AppColors.textSecondary()
    val axisLabelStyle = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)

    fun chartOutputValue(rpm: Double, ratio: Double): Double = GearCalculator.outputValue(
        rpm = rpm,
        gearRatio = ratio,
        outputType = result.outputType,
        wheelCircumferenceMm = wheelCircumferenceMm ?: 0.0,
        rpmConstant = GearCalculator.DEFAULT_RPM_CONSTANT,
    )

    val referenceRpms = if (isMultiSpeed) {
        result.stages.map { it.referenceRpm }
    } else {
        result.fixedReferenceRpms
    }
    val baseChartMaxRpm = niceChartMaximum(
        values = referenceRpms + listOfNotNull(shiftRpm),
        step = 500.0,
    )
    val multiSpeedGearSegments = if (isMultiSpeed && shiftRpm != null) {
        GearCalculator.buildMultiSpeedChartSegments(
            stages = result.stages,
            shiftRpm = shiftRpm,
            chartMaxRpm = baseChartMaxRpm,
            outputType = result.outputType,
            wheelCircumferenceMm = wheelCircumferenceMm ?: 0.0,
        )
    } else {
        null
    }
    val chartMaxRpm = if (multiSpeedGearSegments != null) {
        niceChartMaximum(
            values = listOf(baseChartMaxRpm) + multiSpeedGearSegments.flatMap { segment ->
                listOf(segment.startRpm, segment.endRpm, segment.coreEndRpm)
            },
            step = 500.0,
        )
    } else {
        baseChartMaxRpm
    }
    val chartMaxSpeed = niceChartMaximum(
        values = result.stages.map { stage ->
            chartOutputValue(chartMaxRpm, stage.gearRatio)
        } + result.stages.flatMap { stage ->
            if (isMultiSpeed) {
                stage.speedsAtReferenceRpmKmh + listOf(stage.shiftSpeedKmh)
            } else {
                stage.speedsAtReferenceRpmKmh
            }
        } + (multiSpeedGearSegments?.flatMap { segment ->
            listOf(segment.startOutput, segment.endOutput)
        } ?: emptyList()),
        step = if (result.outputType == GearOutputType.OUTPUT_RPM) 500.0 else 20.0,
    )

    val shiftPoints = if (shiftRpm != null && isMultiSpeed) {
        result.stages.mapIndexed { index, stage ->
            GearShiftPoint(
                stageNumber = stage.stageNumber,
                rpm = shiftRpm,
                speedKmh = stage.shiftSpeedKmh,
                speedJumpKmh = stage.speedJumpKmh,
                rpmJump = stage.rpmJump,
                color = gearColors[index],
            )
        }
    } else {
        emptyList()
    }
    val onShiftPointSelected: (Int?) -> Unit = { id ->
        onSelectedShiftPointIdChange(if (selectedShiftPointId == id) null else id)
    }

    val gearAxisTicks = result.stages.map { stage ->
        ChartAxisTick(
            fraction = stage.stageNumber.toFloat() / (result.stages.size + 1),
            label = stringResource(R.string.gear_chart_axis_gear_tick, stage.stageNumber),
        )
    }

    val drawChart: DrawScope.(
        chartLeft: Float,
        chartRight: Float,
        chartTop: Float,
        chartBottom: Float,
    ) -> Unit = { chartLeft, chartRight, chartTop, chartBottom ->
        if (showResoBand) {
            val (startRpm, endRpm) = resonanceRpmRange!!
            drawGearChartResonanceBand(
                startRpm = startRpm,
                endRpm = endRpm,
                chartMaxRpm = chartMaxRpm,
                color = resoBandColor,
                chartLeft = chartLeft,
                chartRight = chartRight,
                chartTop = chartTop,
                chartBottom = chartBottom,
            )
        }
        drawGearChartGrid(
            chartLeft = chartLeft,
            chartRight = chartRight,
            chartTop = chartTop,
            chartBottom = chartBottom,
            verticalLineCount = 5,
            gridColor = gridColor,
        )

        when (chartStyle) {
            GearChartStyle.LINE -> {
                val outputBuffer = GearCalculator.chartSegmentOutputBuffer(result.outputType)
                val gearSegments = multiSpeedGearSegments ?: result.stages.map { stage ->
                    val coreEndOutput = chartOutputValue(chartMaxRpm, stage.gearRatio)
                    val bufferedEndOutput = coreEndOutput + outputBuffer
                    GearCalculator.GearChartSegment(
                        startRpm = 0.0,
                        startOutput = 0.0,
                        endRpm = GearCalculator.engineRpmAtOutputValue(
                            outputValue = bufferedEndOutput,
                            gearRatio = stage.gearRatio,
                            outputType = result.outputType,
                            wheelCircumferenceMm = wheelCircumferenceMm ?: 0.0,
                            rpmConstant = GearCalculator.DEFAULT_RPM_CONSTANT,
                        ),
                        endOutput = bufferedEndOutput,
                        coreStartRpm = 0.0,
                        coreStartOutput = 0.0,
                        coreEndRpm = chartMaxRpm,
                        coreEndOutput = coreEndOutput,
                    )
                }
                gearSegments.forEachIndexed { index, segment ->
                    drawGearChartLine(
                        xValues = listOf(segment.startRpm, segment.endRpm),
                        yValues = listOf(segment.startOutput, segment.endOutput),
                        xMax = chartMaxRpm,
                        yMax = chartMaxSpeed,
                        color = gearColors[index],
                        chartLeft = chartLeft,
                        chartRight = chartRight,
                        chartTop = chartTop,
                        chartBottom = chartBottom,
                    )
                }
                if (isMultiSpeed && shiftRpm != null) {
                    gearSegments.drop(1).forEachIndexed { index, segment ->
                        val previousStage = result.stages[index]
                        drawGearChartLine(
                            xValues = listOf(shiftRpm, segment.coreStartRpm),
                            yValues = listOf(previousStage.shiftSpeedKmh, segment.coreStartOutput),
                            xMax = chartMaxRpm,
                            yMax = chartMaxSpeed,
                            color = gearColors[index + 1].copy(alpha = 0.45f),
                            chartLeft = chartLeft,
                            chartRight = chartRight,
                            chartTop = chartTop,
                            chartBottom = chartBottom,
                            strokeWidth = 1.5f,
                            pointRadius = 0f,
                            dashed = true,
                        )
                    }
                }
                (listOfNotNull(shiftRpm) + referenceRpms)
                    .distinct()
                    .filter { it in 1.0..chartMaxRpm }
                    .forEach { rpm ->
                        val x = chartLeft + (rpm / chartMaxRpm).toFloat() * (chartRight - chartLeft)
                        drawLine(
                            color = gridColor.copy(alpha = 0.8f),
                            start = Offset(x, chartTop),
                            end = Offset(x, chartBottom),
                            strokeWidth = 1f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)),
                        )
                    }
            }
            GearChartStyle.BAR -> {
                drawGearSpeedBars(
                    result = result,
                    chartColors = referenceColors,
                    shiftColor = shiftColor,
                    maxSpeed = chartMaxSpeed,
                    chartLeft = chartLeft,
                    chartRight = chartRight,
                    chartTop = chartTop,
                    chartBottom = chartBottom,
                )
            }
        }

        shiftPoints.forEach { point ->
            val center = computeShiftPointCenter(
                point = point,
                chartStyle = chartStyle,
                stageCount = result.stages.size,
                chartLeft = chartLeft,
                chartRight = chartRight,
                chartTop = chartTop,
                chartBottom = chartBottom,
                chartMaxRpm = chartMaxRpm,
                chartMaxSpeed = chartMaxSpeed,
            )
            drawShiftPointMarker(
                center = center,
                color = point.color,
                selected = point.stageNumber == selectedShiftPointId,
            )
        }
    }

    val chartAxes = @Composable { chartModifier: Modifier, compact: Boolean ->
        GearChartWithAxes(
            expandToFill = displayMode == GearChartDisplayMode.FULLSCREEN,
            compact = compact,
            modifier = chartModifier,
            yAxisTitle = stringResource(
                if (result.outputType == GearOutputType.OUTPUT_RPM) {
                    R.string.gear_chart_axis_output_rpm
                } else {
                    R.string.gear_chart_axis_speed
                },
            ),
            xAxisTitle = stringResource(
                if (chartStyle == GearChartStyle.LINE) {
                    R.string.gear_chart_axis_rpm
                } else {
                    R.string.gear_chart_axis_gear
                },
            ),
            yTicks = buildOutputAxisTicks(chartMaxSpeed, result.outputType),
            xTicks = if (chartStyle == GearChartStyle.LINE) {
                buildUniformRpmAxisTicks(chartMaxRpm)
            } else {
                gearAxisTicks
            },
            axisLabelStyle = axisLabelStyle,
            axisLabelColor = axisLabelColor,
            shiftPoints = shiftPoints,
            chartStyle = chartStyle,
            chartMaxRpm = chartMaxRpm,
            chartMaxSpeed = chartMaxSpeed,
            stageCount = result.stages.size,
            selectedShiftPointId = selectedShiftPointId,
            onShiftPointSelected = onShiftPointSelected,
            chartContent = drawChart,
        )
    }

    if (displayMode == GearChartDisplayMode.EMBEDDED) {
        CalculatorSection(title = stringResource(R.string.gear_chart_title)) {
            if (isMultiSpeed) {
                CalculatorFilterChipRow {
                    CalculatorFilterChip(
                        label = stringResource(R.string.gear_chart_style_line),
                        selected = chartStyle == GearChartStyle.LINE,
                        onClick = { onChartStyleNameChange(GearChartStyle.LINE.name) },
                    )
                    CalculatorFilterChip(
                        label = stringResource(R.string.gear_chart_style_bar),
                        selected = chartStyle == GearChartStyle.BAR,
                        onClick = { onChartStyleNameChange(GearChartStyle.BAR.name) },
                    )
                }
            }

            Text(
                text = stringResource(
                    when {
                        isMultiSpeed && chartStyle == GearChartStyle.BAR -> R.string.gear_chart_subtitle_bar
                        isMultiSpeed -> R.string.gear_chart_subtitle_line
                        else -> R.string.gear_chart_subtitle_line_fixed
                    },
                ),
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.textSecondary(),
            )

            if (isMultiSpeed) {
                Text(
                    text = stringResource(R.string.gear_chart_shift_point_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.textSecondary(),
                )
            }

            chartAxes(Modifier.fillMaxWidth(), false)

            selectedShiftPointId?.let { selectedId ->
                shiftPoints.firstOrNull { it.stageNumber == selectedId }?.let { point ->
                    GearShiftPointDetailCard(point = point)
                }
            }

            GearChartLegend(
                colors = if (chartStyle == GearChartStyle.LINE) gearColors else referenceColors,
                labels = if (chartStyle == GearChartStyle.LINE) {
                    gearChartLineLegendLabels(result, isMultiSpeed)
                } else {
                    result.stages.map { stage ->
                        referenceRpmShortLabel(stage.referenceLabel)
                    } + stringResource(R.string.gear_chart_legend_shift)
                },
                showLineStyle = chartStyle == GearChartStyle.LINE,
                showShiftPointMarker = isMultiSpeed,
                resoBandColor = if (showResoBand) resoBandColor else null,
                resoBandLabel = if (showResoBand) {
                    stringResource(R.string.gear_chart_legend_reso_band)
                } else {
                    null
                },
            )
        }
    } else {
        Box(modifier = modifier.fillMaxSize()) {
            chartAxes(Modifier.fillMaxSize(), true)

            selectedShiftPointId?.let { selectedId ->
                shiftPoints.firstOrNull { it.stageNumber == selectedId }?.let { point ->
                    GearShiftPointCompactBar(
                        point = point,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

private data class GearShiftPoint(
    val stageNumber: Int,
    val rpm: Double,
    val speedKmh: Double,
    val speedJumpKmh: Double?,
    val rpmJump: Double?,
    val color: Color,
)

@Composable
private fun GearShiftPointCompactBar(
    point: GearShiftPoint,
    modifier: Modifier = Modifier,
) {
    val summary = buildString {
        append(stringResource(R.string.gear_chart_shift_point_title, point.stageNumber))
        append(" · ")
        append(
            stringResource(
                R.string.gear_result_rpm_value,
                formatDecimal(point.rpm, 0),
            ),
        )
        append(" · ")
        append(
            stringResource(
                R.string.gear_result_speed_kmh,
                formatDecimal(point.speedKmh, 1),
            ),
        )
        when {
            point.speedJumpKmh != null && point.rpmJump != null -> {
                append(" · ↑ ")
                append(
                    stringResource(
                        R.string.gear_result_speed_kmh,
                        formatDecimal(point.speedJumpKmh, 1),
                    ),
                )
            }
        }
    }

    Text(
        text = summary,
        modifier = modifier
            .background(
                color = AppColors.surface().copy(alpha = 0.92f),
                shape = MaterialTheme.shapes.small,
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
        style = MaterialTheme.typography.labelSmall,
        color = AppColors.textPrimary(),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun GearShiftPointDetailCard(point: GearShiftPoint) {
    CalculatorResultCard(title = stringResource(R.string.gear_chart_shift_point_title, point.stageNumber)) {
        CalculatorResultRow(
            label = stringResource(R.string.gear_chart_shift_point_rpm_label),
            value = stringResource(
                R.string.gear_result_rpm_value,
                formatDecimal(point.rpm, 0),
            ),
        )
        CalculatorResultRow(
            label = stringResource(R.string.gear_chart_shift_point_speed_label),
            value = stringResource(
                R.string.gear_result_speed_kmh,
                formatDecimal(point.speedKmh, 1),
            ),
        )
        when {
            point.speedJumpKmh != null && point.rpmJump != null -> {
                CalculatorResultRow(
                    label = stringResource(R.string.gear_chart_shift_point_speed_jump_label),
                    value = stringResource(
                        R.string.gear_result_speed_kmh,
                        formatDecimal(point.speedJumpKmh, 1),
                    ),
                )
                CalculatorResultRow(
                    label = stringResource(R.string.gear_chart_shift_point_rpm_jump_label),
                    value = stringResource(
                        R.string.gear_result_rpm_value,
                        formatDecimal(point.rpmJump, 0),
                    ),
                )
            }
            else -> {
                Text(
                    text = stringResource(R.string.gear_chart_shift_point_final),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.textSecondary(),
                )
            }
        }
    }
}

private data class ChartAxisTick(
    val fraction: Float,
    val label: String,
)

private const val CHART_INSET_FRACTION = 0.02f
private const val GEAR_CHART_FULLSCREEN_WIDTH_FRACTION = 0.9f

private fun plotAxisFraction(inset: Float, dataFraction: Float): Float =
    inset + (1f - 2f * inset) * dataFraction.coerceIn(0f, 1f)

private fun clampAxisTickOffset(
    tickAreaWidth: Dp,
    labelWidth: Dp,
    dataFraction: Float,
): Dp {
    val center = tickAreaWidth * plotAxisFraction(
        inset = CHART_INSET_FRACTION,
        dataFraction = dataFraction,
    )
    val halfLabel = labelWidth / 2
    return (center - halfLabel).coerceIn(0.dp, (tickAreaWidth - labelWidth).coerceAtLeast(0.dp))
}

@Composable
private fun GearChartWithAxes(
    yAxisTitle: String,
    xAxisTitle: String,
    yTicks: List<ChartAxisTick>,
    xTicks: List<ChartAxisTick>,
    axisLabelStyle: androidx.compose.ui.text.TextStyle,
    axisLabelColor: Color,
    shiftPoints: List<GearShiftPoint>,
    chartStyle: GearChartStyle,
    chartMaxRpm: Double,
    chartMaxSpeed: Double,
    stageCount: Int,
    selectedShiftPointId: Int?,
    onShiftPointSelected: (Int?) -> Unit,
    expandToFill: Boolean = false,
    compact: Boolean = false,
    modifier: Modifier = Modifier,
    chartContent: DrawScope.(
        chartLeft: Float,
        chartRight: Float,
        chartTop: Float,
        chartBottom: Float,
    ) -> Unit,
) {
    if (expandToFill) {
        GearChartWithAxesFillLayout(
            yAxisTitle = yAxisTitle,
            xAxisTitle = xAxisTitle,
            yTicks = yTicks,
            xTicks = xTicks,
            axisLabelStyle = axisLabelStyle,
            axisLabelColor = axisLabelColor,
            shiftPoints = shiftPoints,
            chartStyle = chartStyle,
            chartMaxRpm = chartMaxRpm,
            chartMaxSpeed = chartMaxSpeed,
            stageCount = stageCount,
            selectedShiftPointId = selectedShiftPointId,
            onShiftPointSelected = onShiftPointSelected,
            compact = compact,
            modifier = modifier,
            chartContent = chartContent,
        )
        return
    }

    val hitRadiusPx = with(LocalDensity.current) { 28.dp.toPx() }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.55f)
            .padding(top = 8.dp),
    ) {
        val yAxisWidth = 48.dp
        val xAxisHeight = 56.dp
        val chartTopInset = 18.dp
        val chartWidth = maxWidth - yAxisWidth
        val chartHeight = maxHeight - xAxisHeight

        Text(
            text = yAxisTitle,
            style = axisLabelStyle,
            color = axisLabelColor,
            modifier = Modifier
                .width(yAxisWidth)
                .padding(top = 2.dp),
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        Box(
            modifier = Modifier
                .width(yAxisWidth)
                .height(chartHeight)
                .padding(top = chartTopInset),
        ) {
            yTicks.forEach { tick ->
                Text(
                    text = tick.label,
                    style = axisLabelStyle,
                    color = axisLabelColor,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopStart)
                        .offset(
                            y = chartHeight * plotAxisFraction(
                                inset = CHART_INSET_FRACTION,
                                dataFraction = 1f - tick.fraction,
                            ) - 7.dp,
                        ),
                )
            }
        }

        GearChartCanvas(
            modifier = Modifier
                .padding(start = yAxisWidth, top = chartTopInset)
                .width(chartWidth)
                .height(chartHeight),
            shiftPoints = shiftPoints,
            chartStyle = chartStyle,
            chartMaxRpm = chartMaxRpm,
            chartMaxSpeed = chartMaxSpeed,
            stageCount = stageCount,
            selectedShiftPointId = selectedShiftPointId,
            onShiftPointSelected = onShiftPointSelected,
            hitRadiusPx = hitRadiusPx,
            chartContent = chartContent,
        )

        Box(
            modifier = Modifier
                .padding(start = yAxisWidth, top = chartTopInset)
                .width(chartWidth)
                .height(xAxisHeight - 20.dp)
                .offset(y = chartHeight),
        ) {
            val tickLabelWidth = 44.dp
            xTicks.forEach { tick ->
                Text(
                    text = tick.label,
                    style = axisLabelStyle,
                    color = axisLabelColor,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .width(tickLabelWidth)
                        .align(Alignment.TopStart)
                        .offset(
                            x = clampAxisTickOffset(
                                tickAreaWidth = chartWidth,
                                labelWidth = tickLabelWidth,
                                dataFraction = tick.fraction,
                            ),
                        ),
                )
            }
        }

        Text(
            text = xAxisTitle,
            style = axisLabelStyle,
            color = axisLabelColor,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = yAxisWidth),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun GearChartWithAxesFillLayout(
    yAxisTitle: String,
    xAxisTitle: String,
    yTicks: List<ChartAxisTick>,
    xTicks: List<ChartAxisTick>,
    axisLabelStyle: androidx.compose.ui.text.TextStyle,
    axisLabelColor: Color,
    shiftPoints: List<GearShiftPoint>,
    chartStyle: GearChartStyle,
    chartMaxRpm: Double,
    chartMaxSpeed: Double,
    stageCount: Int,
    selectedShiftPointId: Int?,
    onShiftPointSelected: (Int?) -> Unit,
    compact: Boolean,
    modifier: Modifier,
    chartContent: DrawScope.(
        chartLeft: Float,
        chartRight: Float,
        chartTop: Float,
        chartBottom: Float,
    ) -> Unit,
) {
    val hitRadiusPx = with(LocalDensity.current) { 28.dp.toPx() }
    val yAxisWidth = if (compact) 36.dp else 48.dp
    val xTickHeight = if (compact) 22.dp else 30.dp
    val xTitleHeight = if (compact) 12.dp else 16.dp
    val yTitleHeight = if (compact) 12.dp else 16.dp
    val compactAxisStyle = if (compact) {
        axisLabelStyle.copy(fontSize = 9.sp)
    } else {
        axisLabelStyle
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .width(yAxisWidth)
                    .fillMaxHeight(),
            ) {
                Text(
                    text = yAxisTitle,
                    style = compactAxisStyle,
                    color = axisLabelColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(yTitleHeight),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    val plotHeight = maxHeight
                    yTicks.forEach { tick ->
                        Text(
                            text = tick.label,
                            style = compactAxisStyle,
                            color = axisLabelColor,
                            textAlign = TextAlign.End,
                            maxLines = 1,
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset(
                                    y = plotHeight * plotAxisFraction(
                                        inset = CHART_INSET_FRACTION,
                                        dataFraction = 1f - tick.fraction,
                                    ) - 6.dp,
                                ),
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                GearChartCanvas(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .fillMaxHeight(),
                    shiftPoints = shiftPoints,
                    chartStyle = chartStyle,
                    chartMaxRpm = chartMaxRpm,
                    chartMaxSpeed = chartMaxSpeed,
                    stageCount = stageCount,
                    selectedShiftPointId = selectedShiftPointId,
                    onShiftPointSelected = onShiftPointSelected,
                    hitRadiusPx = hitRadiusPx,
                    chartContent = chartContent,
                )

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(xTickHeight),
                ) {
                    val tickAreaWidth = maxWidth
                    val tickLabelWidth = 36.dp
                    xTicks.forEach { tick ->
                        Text(
                            text = tick.label,
                            style = compactAxisStyle,
                            color = axisLabelColor,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .width(tickLabelWidth)
                                .offset(
                                    x = clampAxisTickOffset(
                                        tickAreaWidth = tickAreaWidth,
                                        labelWidth = tickLabelWidth,
                                        dataFraction = tick.fraction,
                                    ),
                                ),
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(xTitleHeight),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(modifier = Modifier.width(yAxisWidth))
            Text(
                text = xAxisTitle,
                style = compactAxisStyle,
                color = axisLabelColor,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun GearChartCanvas(
    shiftPoints: List<GearShiftPoint>,
    chartStyle: GearChartStyle,
    chartMaxRpm: Double,
    chartMaxSpeed: Double,
    stageCount: Int,
    selectedShiftPointId: Int?,
    onShiftPointSelected: (Int?) -> Unit,
    hitRadiusPx: Float,
    modifier: Modifier = Modifier,
    chartContent: DrawScope.(
        chartLeft: Float,
        chartRight: Float,
        chartTop: Float,
        chartBottom: Float,
    ) -> Unit,
) {
    Canvas(
        modifier = modifier.pointerInput(
            shiftPoints,
            chartStyle,
            chartMaxRpm,
            chartMaxSpeed,
            selectedShiftPointId,
        ) {
            detectTapGestures { tapOffset ->
                val chartLeft = size.width * CHART_INSET_FRACTION
                val chartRight = size.width * (1f - CHART_INSET_FRACTION)
                val chartTop = size.height * CHART_INSET_FRACTION
                val chartBottom = size.height * (1f - CHART_INSET_FRACTION)

                val tappedPoint = shiftPoints.firstOrNull { point ->
                    val center = computeShiftPointCenter(
                        point = point,
                        chartStyle = chartStyle,
                        stageCount = stageCount,
                        chartLeft = chartLeft,
                        chartRight = chartRight,
                        chartTop = chartTop,
                        chartBottom = chartBottom,
                        chartMaxRpm = chartMaxRpm,
                        chartMaxSpeed = chartMaxSpeed,
                    )
                    (tapOffset - center).getDistance() <= hitRadiusPx
                }
                onShiftPointSelected(tappedPoint?.stageNumber)
            }
        },
    ) {
        val chartLeft = size.width * CHART_INSET_FRACTION
        val chartRight = size.width * (1f - CHART_INSET_FRACTION)
        val chartTop = size.height * CHART_INSET_FRACTION
        val chartBottom = size.height * (1f - CHART_INSET_FRACTION)
        chartContent(chartLeft, chartRight, chartTop, chartBottom)
    }
}

private fun buildOutputAxisTicks(
    maxValue: Double,
    outputType: GearOutputType,
): List<ChartAxisTick> {
    val steps = 4
    return (0..steps).map { step ->
        ChartAxisTick(
            fraction = step.toFloat() / steps,
            label = formatChartOutputYTick(maxValue / steps * step, outputType),
        )
    }
}

private fun buildUniformRpmAxisTicks(chartMaxRpm: Double): List<ChartAxisTick> {
    val steps = 4
    return (0..steps).map { step ->
        val rpm = chartMaxRpm / steps * step
        ChartAxisTick(
            fraction = (step.toFloat() / steps).coerceIn(0f, 1f),
            label = formatChartRpm(rpm),
        )
    }
}

private fun niceChartMaximum(values: List<Double>, step: Double): Double {
    val maxValue = values.maxOrNull()?.coerceAtLeast(step) ?: step
    return ceil(maxValue / step) * step
}

private fun formatChartOutputYTick(value: Double, outputType: GearOutputType): String =
    when (outputType) {
        GearOutputType.OUTPUT_RPM -> String.format(Locale.getDefault(), "%.0f", value)
        GearOutputType.VEHICLE_SPEED -> String.format(Locale.getDefault(), "%.0f", value)
    }

private fun formatChartRpm(value: Double): String =
    String.format(Locale.getDefault(), "%.0f", value)

private enum class GearChartStyle {
    LINE,
    BAR,
}

private fun resolveGearChartStyle(
    chartStyleName: String,
    supportsBarChart: Boolean,
): GearChartStyle {
    val requested = GearChartStyle.entries.firstOrNull { it.name == chartStyleName }
        ?: GearChartStyle.LINE
    return if (supportsBarChart) requested else GearChartStyle.LINE
}

private fun computeShiftPointCenter(
    point: GearShiftPoint,
    chartStyle: GearChartStyle,
    stageCount: Int,
    chartLeft: Float,
    chartRight: Float,
    chartTop: Float,
    chartBottom: Float,
    chartMaxRpm: Double,
    chartMaxSpeed: Double,
): Offset = when (chartStyle) {
    GearChartStyle.LINE -> {
        val x = chartLeft + (point.rpm / chartMaxRpm).toFloat().coerceIn(0f, 1f) * (chartRight - chartLeft)
        val y = chartBottom - (point.speedKmh / chartMaxSpeed).toFloat().coerceIn(0f, 1f) * (chartBottom - chartTop)
        Offset(x, y)
    }
    GearChartStyle.BAR -> {
        val chartWidth = chartRight - chartLeft
        val barGroupWidth = chartWidth / (stageCount + 1)
        val groupCenterX = chartLeft + barGroupWidth * point.stageNumber
        val chartHeight = chartBottom - chartTop
        val shiftHeight = (point.speedKmh / chartMaxSpeed * chartHeight).toFloat()
        Offset(groupCenterX, chartBottom - shiftHeight / 2f)
    }
}

private fun DrawScope.drawShiftPointMarker(
    center: Offset,
    color: Color,
    selected: Boolean,
) {
    val outerRadius = if (selected) 16f else 12f
    drawCircle(
        color = Color.White.copy(alpha = 0.95f),
        radius = outerRadius,
        center = center,
    )
    drawCircle(
        color = color,
        radius = outerRadius - 3.5f,
        center = center,
    )
    drawCircle(
        color = Color.White,
        radius = if (selected) 5f else 4f,
        center = center,
    )
    if (selected) {
        drawCircle(
            color = color.copy(alpha = 0.35f),
            radius = outerRadius + 6f,
            center = center,
            style = Stroke(width = 2f),
        )
    }
}

private fun DrawScope.drawGearChartResonanceBand(
    startRpm: Double,
    endRpm: Double,
    chartMaxRpm: Double,
    color: Color,
    chartLeft: Float,
    chartRight: Float,
    chartTop: Float,
    chartBottom: Float,
) {
    if (chartMaxRpm <= 0.0) return
    val clampedStart = startRpm.coerceIn(0.0, chartMaxRpm)
    val clampedEnd = endRpm.coerceIn(0.0, chartMaxRpm)
    if (clampedEnd <= clampedStart) return

    val chartWidth = chartRight - chartLeft
    val xStart = chartLeft + (clampedStart / chartMaxRpm).toFloat() * chartWidth
    val xEnd = chartLeft + (clampedEnd / chartMaxRpm).toFloat() * chartWidth
    drawRect(
        color = color,
        topLeft = Offset(xStart, chartTop),
        size = Size(xEnd - xStart, chartBottom - chartTop),
    )
}

private fun DrawScope.drawGearChartGrid(
    chartLeft: Float,
    chartRight: Float,
    chartTop: Float,
    chartBottom: Float,
    verticalLineCount: Int,
    gridColor: Color,
) {
    for (step in 0..4) {
        val y = chartBottom - (step / 4f) * (chartBottom - chartTop)
        drawLine(
            color = gridColor,
            start = Offset(chartLeft, y),
            end = Offset(chartRight, y),
            strokeWidth = 1f,
        )
    }
    if (verticalLineCount > 1) {
        for (index in 0 until verticalLineCount) {
            val x = chartLeft + (index.toFloat() / (verticalLineCount - 1)) * (chartRight - chartLeft)
            drawLine(
                color = gridColor,
                start = Offset(x, chartTop),
                end = Offset(x, chartBottom),
                strokeWidth = 1f,
            )
        }
    }
}

private fun DrawScope.drawGearChartLine(
    xValues: List<Double>,
    yValues: List<Double>,
    xMax: Double,
    yMax: Double,
    color: Color,
    chartLeft: Float,
    chartRight: Float,
    chartTop: Float,
    chartBottom: Float,
    strokeWidth: Float = 3f,
    pointRadius: Float = 4f,
    dashed: Boolean = false,
) {
    if (xValues.isEmpty() || yValues.isEmpty() || xMax <= 0.0 || yMax <= 0.0) return

    val points = xValues.zip(yValues).map { (xValue, yValue) ->
        val x = chartLeft + (xValue / xMax).toFloat().coerceIn(0f, 1f) * (chartRight - chartLeft)
        val y = chartBottom - (yValue / yMax).toFloat().coerceIn(0f, 1f) * (chartBottom - chartTop)
        Offset(x, y)
    }

    val path = Path().apply {
        moveTo(points.first().x, points.first().y)
        points.drop(1).forEach { lineTo(it.x, it.y) }
    }
    drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = strokeWidth,
            pathEffect = if (dashed) {
                PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
            } else {
                null
            },
        ),
    )
    points.drop(1).forEach { point ->
        drawCircle(color = color, radius = pointRadius, center = point)
    }
}

private fun DrawScope.drawGearSpeedBars(
    result: GearCalculatorResult,
    chartColors: List<Color>,
    shiftColor: Color,
    maxSpeed: Double,
    chartLeft: Float,
    chartRight: Float,
    chartTop: Float,
    chartBottom: Float,
) {
    val chartWidth = chartRight - chartLeft
    val barGroupWidth = chartWidth / (result.stages.size + 1)
    val barWidth = barGroupWidth * 0.18f
    val chartHeight = chartBottom - chartTop
    if (maxSpeed <= 0.0) return

    result.stages.forEachIndexed { stageIndex, stage ->
        val groupCenterX = chartLeft + barGroupWidth * (stageIndex + 1)
        stage.speedsAtReferenceRpmKmh.forEachIndexed { speedIndex, speed ->
            val barHeight = (speed / maxSpeed * chartHeight).toFloat()
            val x = groupCenterX + (speedIndex - 1.5f) * barWidth
            drawRoundRect(
                color = chartColors[speedIndex],
                topLeft = Offset(x, chartBottom - barHeight),
                size = Size(barWidth * 0.9f, barHeight),
                cornerRadius = CornerRadius(4f, 4f),
            )
        }
        val shiftHeight = (stage.shiftSpeedKmh / maxSpeed * chartHeight).toFloat()
        drawRoundRect(
            color = shiftColor,
            topLeft = Offset(groupCenterX - barWidth * 0.45f, chartBottom - shiftHeight),
            size = Size(barWidth * 0.9f, shiftHeight),
            cornerRadius = CornerRadius(4f, 4f),
        )
    }
}

@Composable
private fun GearChartLegend(
    colors: List<Color>,
    labels: List<String>,
    showLineStyle: Boolean,
    showShiftPointMarker: Boolean = false,
    resoBandColor: Color? = null,
    resoBandLabel: String? = null,
    horizontal: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val legendItem: @Composable (Color?, String, Boolean) -> Unit = { color, label, filledBand ->
        val markerColor = color ?: AppColors.primaryBlue()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Canvas(
                modifier = Modifier.size(
                    width = if (color == null) 16.dp else if (horizontal) 16.dp else 20.dp,
                    height = if (color == null) 16.dp else if (horizontal) 10.dp else 12.dp,
                ),
            ) {
                when {
                    color == null -> {
                        drawShiftPointMarker(
                            center = Offset(size.width / 2f, size.height / 2f),
                            color = markerColor,
                            selected = false,
                        )
                    }
                    filledBand -> {
                        drawRoundRect(
                            color = color,
                            size = Size(size.width, size.height),
                            cornerRadius = CornerRadius(2f, 2f),
                        )
                    }
                    showLineStyle -> {
                        val y = size.height / 2f
                        drawLine(
                            color = color,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 3f,
                        )
                        drawCircle(
                            color = color,
                            radius = 4f,
                            center = Offset(size.width / 2f, y),
                        )
                    }
                    else -> {
                        drawRoundRect(
                            color = color,
                            size = Size(size.width, size.height),
                            cornerRadius = CornerRadius(2f, 2f),
                        )
                    }
                }
            }
            Spacer(Modifier.width(if (horizontal) 4.dp else 8.dp))
            Text(
                text = label,
                style = if (horizontal) {
                    MaterialTheme.typography.labelSmall
                } else {
                    MaterialTheme.typography.bodySmall
                },
                color = AppColors.textSecondary(),
                textAlign = TextAlign.Start,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }

    if (horizontal) {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.Start),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showShiftPointMarker) {
                legendItem(null, stringResource(R.string.gear_chart_legend_shift_point), false)
            }
            if (resoBandColor != null && resoBandLabel != null) {
                legendItem(resoBandColor, resoBandLabel, true)
            }
            labels.forEachIndexed { index, label ->
                val legendColor = if (index < colors.size) colors[index] else Color.White
                legendItem(legendColor, label, false)
            }
        }
    } else {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (showShiftPointMarker) {
                legendItem(null, stringResource(R.string.gear_chart_legend_shift_point), false)
            }
            if (resoBandColor != null && resoBandLabel != null) {
                legendItem(resoBandColor, resoBandLabel, true)
            }
            labels.forEachIndexed { index, label ->
                val legendColor = if (index < colors.size) colors[index] else Color.White
                legendItem(legendColor, label, false)
            }
        }
    }
}

private fun parsePositiveInteger(text: String): Int? =
    text.trim().toIntOrNull()?.takeIf { it > 0 }

private fun String.filterIntegerInput(): String = filter { it.isDigit() }

private fun formatDecimal(value: Double, decimals: Int): String =
    String.format(Locale.getDefault(), "%.${decimals}f", value)

@Composable
private fun formatGearOutputNumber(value: Double, outputType: GearOutputType): String =
    if (outputType == GearOutputType.OUTPUT_RPM) {
        formatDecimal(value, 0)
    } else {
        formatDecimal(value, 1)
    }

@Composable
private fun formatGearOutputValue(value: Double, outputType: GearOutputType): String =
    if (outputType == GearOutputType.OUTPUT_RPM) {
        stringResource(R.string.gear_result_output_rpm_value, formatGearOutputNumber(value, outputType))
    } else {
        stringResource(R.string.gear_result_speed_kmh, formatGearOutputNumber(value, outputType))
    }
