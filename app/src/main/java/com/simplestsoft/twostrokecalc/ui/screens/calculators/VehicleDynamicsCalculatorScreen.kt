package com.simplestsoft.twostrokecalc.ui.screens.calculators

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.simplestsoft.twostrokecalc.R
import com.simplestsoft.twostrokecalc.domain.calculation.AccelerationCurvePoint
import com.simplestsoft.twostrokecalc.domain.calculation.GearCalculator
import com.simplestsoft.twostrokecalc.domain.calculation.TopSpeedLimit
import com.simplestsoft.twostrokecalc.domain.calculation.VehicleDynamicsCalculator
import com.simplestsoft.twostrokecalc.domain.calculation.VehicleDynamicsEngineMode
import com.simplestsoft.twostrokecalc.domain.calculation.VehicleDynamicsGearInput
import com.simplestsoft.twostrokecalc.domain.calculation.VehicleDynamicsInput
import com.simplestsoft.twostrokecalc.domain.calculation.VehicleDynamicsPreset
import com.simplestsoft.twostrokecalc.domain.calculation.VehicleDynamicsResult
import com.simplestsoft.twostrokecalc.domain.calculation.values
import com.simplestsoft.twostrokecalc.domain.model.GearPreset
import com.simplestsoft.twostrokecalc.domain.model.labelRes
import com.simplestsoft.twostrokecalc.domain.model.values as gearPresetValues
import com.simplestsoft.twostrokecalc.ui.components.AppColors
import com.simplestsoft.twostrokecalc.ui.components.FormFieldDefaults
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
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorSection
import com.simplestsoft.twostrokecalc.ui.components.calculator.filterDecimalInput
import com.simplestsoft.twostrokecalc.ui.components.calculator.parseDecimal
import java.util.Locale
import kotlin.math.max

private data class DynamicsGearStageUi(
    val pinionText: String,
    val gearText: String,
)

private fun defaultDynamicsStages(): List<DynamicsGearStageUi> = listOf(
    DynamicsGearStageUi("12", "57"),
    DynamicsGearStageUi("13", "42"),
    DynamicsGearStageUi("17", "38"),
    DynamicsGearStageUi("21", "35"),
)

private fun encodeDynamicsStages(stages: List<DynamicsGearStageUi>): String =
    stages.joinToString(";") { "${it.pinionText}|${it.gearText}" }

private fun decodeDynamicsStages(encoded: String): List<DynamicsGearStageUi> {
    if (encoded.isBlank()) return defaultDynamicsStages()
    return encoded.split(";").mapNotNull { part ->
        val tokens = part.split("|")
        if (tokens.size == 2) DynamicsGearStageUi(tokens[0], tokens[1]) else null
    }.takeIf { it.isNotEmpty() } ?: defaultDynamicsStages()
}

@Composable
fun VehicleDynamicsCalculatorScreen() {
    var presetName by rememberSaveable { mutableStateOf(VehicleDynamicsPreset.ROLLER.name) }
    val preset = VehicleDynamicsPreset.entries.firstOrNull { it.name == presetName }
        ?: VehicleDynamicsPreset.ROLLER

    var engineModeName by rememberSaveable { mutableStateOf(VehicleDynamicsEngineMode.POWER.name) }
    val engineMode = VehicleDynamicsEngineMode.entries.firstOrNull { it.name == engineModeName }
        ?: VehicleDynamicsEngineMode.POWER

    var massText by rememberSaveable { mutableStateOf("180") }
    var powerText by rememberSaveable { mutableStateOf("12") }
    var torqueText by rememberSaveable { mutableStateOf("") }
    var engineRpmText by rememberSaveable { mutableStateOf("7500") }
    var maxEngineRpmText by rememberSaveable { mutableStateOf("9500") }

    var primaryPinionText by rememberSaveable { mutableStateOf("23") }
    var primaryGearText by rememberSaveable { mutableStateOf("64") }
    var stagesEncoded by rememberSaveable { mutableStateOf(encodeDynamicsStages(defaultDynamicsStages())) }
    val gearStages = decodeDynamicsStages(stagesEncoded)

    var wheelCircumferenceText by rememberSaveable { mutableStateOf("1307") }
    var shiftRpmText by rememberSaveable { mutableStateOf("8000") }
    var efficiencyText by rememberSaveable { mutableStateOf("90") }

    var dragCoefficientText by rememberSaveable { mutableStateOf("0.65") }
    var frontalAreaText by rememberSaveable { mutableStateOf("0.50") }
    var rollingResistanceText by rememberSaveable { mutableStateOf("0.015") }
    var airDensityText by rememberSaveable { mutableStateOf("1.204") }
    var massFactorText by rememberSaveable { mutableStateOf("1.10") }
    var gradientText by rememberSaveable { mutableStateOf("0") }
    var tractionLimitText by rememberSaveable { mutableStateOf("") }

    var analysisSpeedText by rememberSaveable { mutableStateOf("0") }
    var targetSpeedText by rememberSaveable { mutableStateOf("100") }
    var selectedGearIndex by rememberSaveable { mutableStateOf(0) }
    var appliedGearPresetName by rememberSaveable { mutableStateOf("") }

    fun markCustom() {
        presetName = VehicleDynamicsPreset.CUSTOM.name
    }

    fun applyVehiclePreset(selected: VehicleDynamicsPreset) {
        presetName = selected.name
        selected.values().let { values ->
            massText = formatDecimal(values.massKg, 0)
            powerText = formatDecimal(values.powerPs, 1)
            engineRpmText = formatDecimal(values.engineRpm, 0)
            wheelCircumferenceText = formatDecimal(values.wheelCircumferenceMm, 0)
            dragCoefficientText = formatDecimal(values.dragCoefficient, 2)
            frontalAreaText = formatDecimal(values.frontalAreaM2, 2)
            rollingResistanceText = formatDecimal(values.rollingResistance, 3)
            massFactorText = formatDecimal(values.massFactor, 2)
        }
    }

    fun applyGearPreset(selected: GearPreset) {
        appliedGearPresetName = selected.name
        selected.gearPresetValues()?.let { values ->
            primaryPinionText = values.primaryPinion.toString()
            primaryGearText = values.primaryGear.toString()
            stagesEncoded = encodeDynamicsStages(
                values.stages.map { DynamicsGearStageUi(it.secondaryPinion.toString(), it.secondaryGear.toString()) },
            )
            markCustom()
        }
    }

    fun updateStages(transform: (List<DynamicsGearStageUi>) -> List<DynamicsGearStageUi>) {
        stagesEncoded = encodeDynamicsStages(transform(gearStages))
        markCustom()
    }

    val mass = parseDecimal(massText)
    val power = parseDecimal(powerText)
    val torque = parseDecimal(torqueText)
    val engineRpm = parseDecimal(engineRpmText)
    val maxEngineRpm = parseDecimal(maxEngineRpmText)
    val primaryPinion = parsePositiveInteger(primaryPinionText)
    val primaryGear = parsePositiveInteger(primaryGearText)
    val wheelCircumference = parseDecimal(wheelCircumferenceText)
    val shiftRpm = parseDecimal(shiftRpmText)
    val efficiency = parseDecimal(efficiencyText)?.div(100.0)
    val dragCoefficient = parseDecimal(dragCoefficientText)
    val frontalArea = parseDecimal(frontalAreaText)
    val rollingResistance = parseDecimal(rollingResistanceText)
    val airDensity = parseDecimal(airDensityText)
    val massFactor = parseDecimal(massFactorText)
    val gradient = parseDecimal(gradientText) ?: 0.0
    val tractionLimit = parseDecimal(tractionLimitText)
    val analysisSpeed = parseDecimal(analysisSpeedText) ?: 0.0
    val targetSpeed = parseDecimal(targetSpeedText)

    val parsedStages = gearStages.mapNotNull { stage ->
        val pinion = parsePositiveInteger(stage.pinionText)
        val gear = parsePositiveInteger(stage.gearText)
        if (pinion != null && gear != null) VehicleDynamicsGearInput(pinion, gear) else null
    }

    val hasEngineInput = when (engineMode) {
        VehicleDynamicsEngineMode.POWER -> power != null && power > 0.0
        VehicleDynamicsEngineMode.TORQUE -> torque != null && torque > 0.0
    }

    val hasInput = mass != null && mass > 0.0 &&
        hasEngineInput &&
        engineRpm != null && engineRpm > 0.0 &&
        maxEngineRpm != null && maxEngineRpm > 0.0 &&
        primaryPinion != null && primaryGear != null &&
        parsedStages.size == gearStages.size &&
        parsedStages.isNotEmpty() &&
        wheelCircumference != null && wheelCircumference > 0.0 &&
        shiftRpm != null && shiftRpm > 0.0 &&
        efficiency != null &&
        dragCoefficient != null && dragCoefficient > 0.0 &&
        frontalArea != null && frontalArea > 0.0 &&
        rollingResistance != null && rollingResistance > 0.0 &&
        airDensity != null && airDensity > 0.0 &&
        massFactor != null && massFactor > 0.0 &&
        targetSpeed != null && targetSpeed > 0.0

    val result = if (hasInput) {
        VehicleDynamicsCalculator.calculate(
            VehicleDynamicsInput(
                massKg = mass!!,
                engineMode = engineMode,
                powerPs = if (engineMode == VehicleDynamicsEngineMode.POWER) power else null,
                torqueNm = if (engineMode == VehicleDynamicsEngineMode.TORQUE) torque else null,
                engineRpm = engineRpm!!,
                maxEngineRpm = maxEngineRpm!!,
                primaryPinion = primaryPinion!!,
                primaryGear = primaryGear!!,
                stages = parsedStages,
                wheelCircumferenceMm = wheelCircumference!!,
                shiftRpm = shiftRpm!!,
                drivetrainEfficiency = efficiency!!,
                dragCoefficient = dragCoefficient!!,
                frontalAreaM2 = frontalArea!!,
                rollingResistance = rollingResistance!!,
                airDensityKgM3 = airDensity!!,
                massFactor = massFactor!!,
                gradientPercent = gradient,
                tractionLimitG = tractionLimit?.takeIf { it > 0.0 },
                analysisSpeedKmh = analysisSpeed,
                targetSpeedKmh = targetSpeed!!,
                selectedGearIndex = selectedGearIndex.coerceIn(0, parsedStages.lastIndex),
            ),
        )
    } else {
        null
    }

    CalculatorScaffold {
        CalculatorHeader(
            title = stringResource(R.string.vehicle_dynamics_title),
            subtitle = stringResource(R.string.vehicle_dynamics_subtitle),
        )

        CalculatorSection(title = stringResource(R.string.vehicle_dynamics_section_vehicle)) {
            val vehiclePresetOptions = VehicleDynamicsPreset.entries.map { item ->
                CalculatorDropdownOption(item.name, stringResource(item.labelRes()))
            }
            CalculatorChoiceField(
                label = "",
                options = vehiclePresetOptions,
                selectedKey = preset.name,
                onOptionSelected = { key ->
                    val item = VehicleDynamicsPreset.entries.first { it.name == key }
                    if (item != VehicleDynamicsPreset.CUSTOM) {
                        applyVehiclePreset(item)
                    } else {
                        presetName = item.name
                    }
                },
            )
            CalculatorDecimalField(
                value = massText,
                onValueChange = { massText = it.filterDecimalInput(); markCustom() },
                label = stringResource(R.string.vehicle_dynamics_mass_label),
                suffix = stringResource(R.string.vehicle_dynamics_unit_kg),
                supportingText = stringResource(R.string.vehicle_dynamics_mass_hint),
            )
        }

        CalculatorSection(title = stringResource(R.string.vehicle_dynamics_section_engine)) {
            val engineModeOptions = VehicleDynamicsEngineMode.entries.map { mode ->
                CalculatorDropdownOption(mode.name, stringResource(mode.labelRes()))
            }
            CalculatorChoiceField(
                label = "",
                options = engineModeOptions,
                selectedKey = engineMode.name,
                onOptionSelected = { engineModeName = it },
            )
            if (engineMode == VehicleDynamicsEngineMode.POWER) {
                CalculatorDecimalField(
                    value = powerText,
                    onValueChange = { powerText = it.filterDecimalInput(); markCustom() },
                    label = stringResource(R.string.vehicle_dynamics_power_label),
                    suffix = stringResource(R.string.vehicle_dynamics_unit_ps),
                )
            } else {
                CalculatorDecimalField(
                    value = torqueText,
                    onValueChange = { torqueText = it.filterDecimalInput(); markCustom() },
                    label = stringResource(R.string.vehicle_dynamics_torque_label),
                    suffix = stringResource(R.string.vehicle_dynamics_unit_nm),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CalculatorDecimalField(
                    value = engineRpmText,
                    onValueChange = { engineRpmText = it.filterDecimalInput(); markCustom() },
                    label = stringResource(R.string.vehicle_dynamics_engine_rpm_label),
                    suffix = stringResource(R.string.vehicle_dynamics_unit_rpm),
                    modifier = Modifier.weight(1f),
                )
                CalculatorDecimalField(
                    value = maxEngineRpmText,
                    onValueChange = { maxEngineRpmText = it.filterDecimalInput(); markCustom() },
                    label = stringResource(R.string.vehicle_dynamics_max_rpm_label),
                    suffix = stringResource(R.string.vehicle_dynamics_unit_rpm),
                    modifier = Modifier.weight(1f),
                )
            }
        }

        CalculatorSection(title = stringResource(R.string.vehicle_dynamics_section_drivetrain)) {
            val gearPresetOptions = GearPreset.entries
                .filter { it != GearPreset.CUSTOM }
                .map { item -> CalculatorDropdownOption(item.name, stringResource(item.labelRes())) }
            CalculatorChoiceField(
                label = stringResource(R.string.gear_presets_label),
                options = gearPresetOptions,
                selectedKey = appliedGearPresetName,
                onOptionSelected = { key ->
                    GearPreset.entries.firstOrNull { it.name == key }?.let { applyGearPreset(it) }
                },
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CalculatorDecimalField(
                    value = primaryPinionText,
                    onValueChange = { primaryPinionText = it.filterIntegerInput(); markCustom() },
                    label = stringResource(R.string.gear_primary_pinion_label),
                    suffix = stringResource(R.string.gear_unit_teeth),
                    modifier = Modifier.weight(1f),
                )
                CalculatorDecimalField(
                    value = primaryGearText,
                    onValueChange = { primaryGearText = it.filterIntegerInput(); markCustom() },
                    label = stringResource(R.string.gear_primary_gear_label),
                    suffix = stringResource(R.string.gear_unit_teeth),
                    modifier = Modifier.weight(1f),
                )
            }
            gearStages.forEachIndexed { index, stage ->
                DynamicsGearStageRow(
                    stageNumber = index + 1,
                    state = stage,
                    canRemove = gearStages.size > GearCalculator.MIN_STAGE_COUNT,
                    onRemove = { updateStages { it.filterIndexed { i, _ -> i != index } } },
                    onStateChange = { updated ->
                        updateStages { stages -> stages.mapIndexed { i, s -> if (i == index) updated else s } }
                    },
                )
            }
            if (gearStages.size < GearCalculator.MAX_STAGE_COUNT) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = {
                            updateStages { it + DynamicsGearStageUi("12", "50") }
                        },
                    ) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.gear_add_stage))
                    }
                    Text(
                        text = stringResource(R.string.gear_add_stage),
                        style = MaterialTheme.typography.labelLarge,
                        color = AppColors.primaryBlue(),
                    )
                }
            }
            CalculatorDecimalField(
                value = wheelCircumferenceText,
                onValueChange = { wheelCircumferenceText = it.filterDecimalInput(); markCustom() },
                label = stringResource(R.string.gear_wheel_circumference_label),
                suffix = stringResource(R.string.gear_unit_mm),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CalculatorDecimalField(
                    value = shiftRpmText,
                    onValueChange = { shiftRpmText = it.filterDecimalInput(); markCustom() },
                    label = stringResource(R.string.gear_shift_rpm_label),
                    suffix = stringResource(R.string.gear_unit_rpm),
                    modifier = Modifier.weight(1f),
                )
                CalculatorDecimalField(
                    value = efficiencyText,
                    onValueChange = { efficiencyText = it.filterDecimalInput(); markCustom() },
                    label = stringResource(R.string.vehicle_dynamics_efficiency_label),
                    suffix = stringResource(R.string.vehicle_dynamics_unit_percent),
                    modifier = Modifier.weight(1f),
                )
            }
        }

        CalculatorSection(title = stringResource(R.string.vehicle_dynamics_section_aero)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CalculatorDecimalField(
                    value = dragCoefficientText,
                    onValueChange = { dragCoefficientText = it.filterDecimalInput(); markCustom() },
                    label = stringResource(R.string.vehicle_dynamics_cw_label),
                    modifier = Modifier.weight(1f),
                )
                CalculatorDecimalField(
                    value = frontalAreaText,
                    onValueChange = { frontalAreaText = it.filterDecimalInput(); markCustom() },
                    label = stringResource(R.string.vehicle_dynamics_frontal_area_label),
                    suffix = stringResource(R.string.vehicle_dynamics_unit_m2),
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CalculatorDecimalField(
                    value = rollingResistanceText,
                    onValueChange = { rollingResistanceText = it.filterDecimalInput(); markCustom() },
                    label = stringResource(R.string.vehicle_dynamics_cr_label),
                    modifier = Modifier.weight(1f),
                )
                CalculatorDecimalField(
                    value = airDensityText,
                    onValueChange = { airDensityText = it.filterDecimalInput(); markCustom() },
                    label = stringResource(R.string.vehicle_dynamics_air_density_label),
                    suffix = stringResource(R.string.vehicle_dynamics_unit_kg_m3),
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CalculatorDecimalField(
                    value = massFactorText,
                    onValueChange = { massFactorText = it.filterDecimalInput(); markCustom() },
                    label = stringResource(R.string.vehicle_dynamics_mass_factor_label),
                    supportingText = stringResource(R.string.vehicle_dynamics_mass_factor_hint),
                    modifier = Modifier.weight(1f),
                )
                CalculatorDecimalField(
                    value = gradientText,
                    onValueChange = { gradientText = it.filterDecimalInput(); markCustom() },
                    label = stringResource(R.string.vehicle_dynamics_gradient_label),
                    suffix = stringResource(R.string.vehicle_dynamics_unit_percent),
                    modifier = Modifier.weight(1f),
                )
            }
            CalculatorDecimalField(
                value = tractionLimitText,
                onValueChange = { tractionLimitText = it.filterDecimalInput(); markCustom() },
                label = stringResource(R.string.vehicle_dynamics_traction_limit_label),
                suffix = stringResource(R.string.vehicle_dynamics_unit_g),
                supportingText = stringResource(R.string.vehicle_dynamics_traction_limit_hint),
            )
        }

        CalculatorSection(title = stringResource(R.string.vehicle_dynamics_section_analysis)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CalculatorDecimalField(
                    value = analysisSpeedText,
                    onValueChange = { analysisSpeedText = it.filterDecimalInput() },
                    label = stringResource(R.string.vehicle_dynamics_analysis_speed_label),
                    suffix = stringResource(R.string.vehicle_dynamics_unit_kmh),
                    modifier = Modifier.weight(1f),
                )
                CalculatorDecimalField(
                    value = targetSpeedText,
                    onValueChange = { targetSpeedText = it.filterDecimalInput() },
                    label = stringResource(R.string.vehicle_dynamics_target_speed_label),
                    suffix = stringResource(R.string.vehicle_dynamics_unit_kmh),
                    modifier = Modifier.weight(1f),
                )
            }
            if (parsedStages.isNotEmpty()) {
                val gearOptions = parsedStages.indices.map { index ->
                    CalculatorDropdownOption(
                        key = index.toString(),
                        label = stringResource(R.string.vehicle_dynamics_gear_chip, index + 1),
                    )
                }
                CalculatorChoiceField(
                    label = stringResource(R.string.vehicle_dynamics_selected_gear_label),
                    options = gearOptions,
                    selectedKey = selectedGearIndex.toString(),
                    onOptionSelected = { selectedGearIndex = it.toInt() },
                )
            }
        }

        InstantResultCard(result = result, hasInput = hasInput)
        GearComparisonCard(result = result, hasInput = hasInput)
        TopSpeedCard(result = result, hasInput = hasInput)
        SprintCard(result = result, hasInput = hasInput, targetSpeed = targetSpeed)
        PowerBreakdownCard(result = result, hasInput = hasInput, analysisSpeed = analysisSpeed)
        GearingTargetCard(result = result, hasInput = hasInput, targetSpeed = targetSpeed, maxEngineRpm = maxEngineRpm)
        QuarterMileCard(result = result, hasInput = hasInput)
        AccelerationChartCard(result = result, hasInput = hasInput)

        Text(
            text = stringResource(R.string.vehicle_dynamics_disclaimer),
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.textSecondary(),
            modifier = Modifier.padding(top = 4.dp),
        )

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun DynamicsGearStageRow(
    stageNumber: Int,
    state: DynamicsGearStageUi,
    canRemove: Boolean,
    onRemove: () -> Unit,
    onStateChange: (DynamicsGearStageUi) -> Unit,
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
                IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
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
    }
}

@Composable
private fun InstantResultCard(result: VehicleDynamicsResult?, hasInput: Boolean) {
    CalculatorResultCard(title = stringResource(R.string.vehicle_dynamics_result_instant_title)) {
        when {
            !hasInput -> CalculatorEmptyResultText(stringResource(R.string.vehicle_dynamics_result_empty))
            result?.selectedGear == null -> CalculatorInvalidResultText(stringResource(R.string.vehicle_dynamics_result_invalid))
            else -> {
                val gear = result.selectedGear
                CalculatorPrimaryResultText(
                    text = stringResource(
                        R.string.vehicle_dynamics_result_acceleration_g,
                        formatDecimal(gear.accelerationG, 2),
                    ),
                )
                if (gear.tractionLimited) {
                    Text(
                        text = stringResource(R.string.vehicle_dynamics_traction_limited_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.textSecondary(),
                    )
                }
                CalculatorResultDivider()
                CalculatorResultRow(
                    label = stringResource(R.string.vehicle_dynamics_result_acceleration_ms2),
                    value = "${formatDecimal(gear.accelerationMs2, 2)} m/s²",
                )
                CalculatorResultRow(
                    label = stringResource(R.string.vehicle_dynamics_result_drive_force),
                    value = "${formatDecimal(gear.forces.driveForceN, 0)} N",
                )
                CalculatorResultRow(
                    label = stringResource(R.string.vehicle_dynamics_result_net_force),
                    value = "${formatDecimal(gear.forces.netForceN, 0)} N",
                )
                CalculatorResultRow(
                    label = stringResource(R.string.vehicle_dynamics_result_torque),
                    value = "${formatDecimal(result.torqueAtEngineNm, 1)} Nm",
                )
                CalculatorResultRow(
                    label = stringResource(R.string.vehicle_dynamics_result_rpm_at_speed),
                    value = "${formatDecimal(gear.engineRpmAtAnalysisSpeed, 0)} ${stringResource(R.string.vehicle_dynamics_unit_rpm)}",
                )
            }
        }
    }
}

@Composable
private fun GearComparisonCard(result: VehicleDynamicsResult?, hasInput: Boolean) {
    CalculatorResultCard(title = stringResource(R.string.vehicle_dynamics_result_gear_comparison_title)) {
        when {
            !hasInput -> CalculatorEmptyResultText(stringResource(R.string.vehicle_dynamics_result_empty))
            result == null -> CalculatorInvalidResultText(stringResource(R.string.vehicle_dynamics_result_invalid))
            else -> {
                result.gearSnapshots.forEach { gear ->
                    CalculatorResultRow(
                        label = stringResource(
                            R.string.vehicle_dynamics_gear_row_label,
                            gear.stageNumber,
                            formatDecimal(gear.gearRatio, 2),
                        ),
                        value = stringResource(
                            R.string.vehicle_dynamics_gear_row_value,
                            formatDecimal(gear.speedAtEngineRpmKmh, 1),
                            formatDecimal(gear.accelerationG, 2),
                        ),
                        hint = stringResource(
                            R.string.vehicle_dynamics_gear_row_hint,
                            formatDecimal(gear.maxSpeedInGearKmh ?: 0.0, 1),
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun TopSpeedCard(result: VehicleDynamicsResult?, hasInput: Boolean) {
    CalculatorResultCard(title = stringResource(R.string.vehicle_dynamics_result_top_speed_title)) {
        when {
            !hasInput -> CalculatorEmptyResultText(stringResource(R.string.vehicle_dynamics_result_empty))
            result?.topSpeedKmh == null -> CalculatorInvalidResultText(stringResource(R.string.vehicle_dynamics_result_invalid))
            else -> {
                CalculatorPrimaryResultText(
                    text = stringResource(
                        R.string.vehicle_dynamics_result_top_speed_value,
                        formatDecimal(result.topSpeedKmh, 1),
                    ),
                )
                CalculatorResultRow(
                    label = stringResource(R.string.vehicle_dynamics_result_top_speed_gear),
                    value = stringResource(R.string.vehicle_dynamics_gear_chip, result.topSpeedGear ?: 0),
                )
                val limitText = when (result.topSpeedLimit) {
                    TopSpeedLimit.AERODYNAMIC -> stringResource(R.string.vehicle_dynamics_limit_aero)
                    TopSpeedLimit.RPM -> stringResource(R.string.vehicle_dynamics_limit_rpm)
                    null -> "–"
                }
                CalculatorResultRow(
                    label = stringResource(R.string.vehicle_dynamics_result_top_speed_limit),
                    value = limitText,
                )
            }
        }
    }
}

@Composable
private fun SprintCard(
    result: VehicleDynamicsResult?,
    hasInput: Boolean,
    targetSpeed: Double?,
) {
    CalculatorResultCard(title = stringResource(R.string.vehicle_dynamics_result_sprint_title)) {
        when {
            !hasInput || targetSpeed == null -> CalculatorEmptyResultText(stringResource(R.string.vehicle_dynamics_result_empty))
            result?.sprintToTarget == null -> CalculatorInvalidResultText(stringResource(R.string.vehicle_dynamics_result_invalid))
            else -> {
                val sprint = result.sprintToTarget
                CalculatorPrimaryResultText(
                    text = stringResource(
                        R.string.vehicle_dynamics_result_sprint_time,
                        formatDecimal(sprint.timeSeconds, 1),
                        formatDecimal(targetSpeed, 0),
                    ),
                )
                CalculatorResultRow(
                    label = stringResource(R.string.vehicle_dynamics_result_sprint_distance),
                    value = "${formatDecimal(sprint.distanceMeters, 0)} m",
                )
                CalculatorResultRow(
                    label = stringResource(R.string.vehicle_dynamics_result_sprint_final_speed),
                    value = "${formatDecimal(sprint.finalSpeedKmh, 1)} km/h",
                )
                CalculatorResultRow(
                    label = stringResource(R.string.vehicle_dynamics_result_sprint_shifts),
                    value = sprint.gearShifts.toString(),
                )
                if (!sprint.reachedTarget) {
                    Text(
                        text = stringResource(R.string.vehicle_dynamics_sprint_not_reached_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.textSecondary(),
                    )
                }
            }
        }
    }
}

@Composable
private fun PowerBreakdownCard(
    result: VehicleDynamicsResult?,
    hasInput: Boolean,
    analysisSpeed: Double,
) {
    CalculatorResultCard(title = stringResource(R.string.vehicle_dynamics_result_power_title)) {
        when {
            !hasInput -> CalculatorEmptyResultText(stringResource(R.string.vehicle_dynamics_result_empty))
            result?.powerAtAnalysisSpeed == null -> CalculatorInvalidResultText(stringResource(R.string.vehicle_dynamics_result_invalid))
            else -> {
                val power = result.powerAtAnalysisSpeed
                Text(
                    text = stringResource(
                        R.string.vehicle_dynamics_result_power_at_speed,
                        formatDecimal(analysisSpeed, 0),
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = AppColors.textSecondary(),
                )
                CalculatorResultRow(
                    label = stringResource(R.string.vehicle_dynamics_result_power_crank),
                    value = "${formatDecimal(power.crankPowerW / VehicleDynamicsCalculator.PS_TO_WATT, 1)} PS",
                )
                CalculatorResultRow(
                    label = stringResource(R.string.vehicle_dynamics_result_power_drag),
                    value = "${formatDecimal(power.dragPowerW, 0)} W (${formatDecimal(power.dragSharePercent, 0)}%)",
                )
                CalculatorResultRow(
                    label = stringResource(R.string.vehicle_dynamics_result_power_rolling),
                    value = "${formatDecimal(power.rollingPowerW, 0)} W (${formatDecimal(power.rollingSharePercent, 0)}%)",
                )
                if (power.gradientPowerW > 0.0) {
                    CalculatorResultRow(
                        label = stringResource(R.string.vehicle_dynamics_result_power_gradient),
                        value = "${formatDecimal(power.gradientPowerW, 0)} W",
                    )
                }
                if (power.accelerationPowerW > 0.0) {
                    CalculatorResultRow(
                        label = stringResource(R.string.vehicle_dynamics_result_power_accel),
                        value = "${formatDecimal(power.accelerationPowerW, 0)} W",
                    )
                }
            }
        }
    }
}

@Composable
private fun GearingTargetCard(
    result: VehicleDynamicsResult?,
    hasInput: Boolean,
    targetSpeed: Double?,
    maxEngineRpm: Double?,
) {
    CalculatorResultCard(title = stringResource(R.string.vehicle_dynamics_result_gearing_title)) {
        when {
            !hasInput || targetSpeed == null || maxEngineRpm == null ->
                CalculatorEmptyResultText(stringResource(R.string.vehicle_dynamics_result_empty))
            result?.requiredGearRatioForTarget == null ->
                CalculatorInvalidResultText(stringResource(R.string.vehicle_dynamics_result_invalid))
            else -> {
                CalculatorPrimaryResultText(
                    text = stringResource(
                        R.string.vehicle_dynamics_result_required_ratio,
                        formatDecimal(result.requiredGearRatioForTarget, 2),
                        formatDecimal(targetSpeed, 0),
                        formatDecimal(maxEngineRpm, 0),
                    ),
                )
            }
        }
    }
}

@Composable
private fun QuarterMileCard(result: VehicleDynamicsResult?, hasInput: Boolean) {
    CalculatorResultCard(title = stringResource(R.string.vehicle_dynamics_result_quarter_mile_title)) {
        when {
            !hasInput -> CalculatorEmptyResultText(stringResource(R.string.vehicle_dynamics_result_empty))
            result?.quarterMileEstimateSeconds == null ->
                CalculatorInvalidResultText(stringResource(R.string.vehicle_dynamics_result_invalid))
            else -> {
                CalculatorResultRow(
                    label = stringResource(R.string.vehicle_dynamics_result_quarter_mile_et),
                    value = "${formatDecimal(result.quarterMileEstimateSeconds ?: 0.0, 2)} s",
                )
                CalculatorResultRow(
                    label = stringResource(R.string.vehicle_dynamics_result_quarter_mile_trap),
                    value = "${formatDecimal(result.quarterMileTrapSpeedKmh ?: 0.0, 1)} km/h",
                )
                Text(
                    text = stringResource(R.string.vehicle_dynamics_quarter_mile_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.textSecondary(),
                )
            }
        }
    }
}

@Composable
private fun AccelerationChartCard(result: VehicleDynamicsResult?, hasInput: Boolean) {
    CalculatorResultCard(title = stringResource(R.string.vehicle_dynamics_result_chart_title)) {
        when {
            !hasInput -> CalculatorEmptyResultText(stringResource(R.string.vehicle_dynamics_result_empty))
            result == null || result.accelerationCurves.isEmpty() ->
                CalculatorInvalidResultText(stringResource(R.string.vehicle_dynamics_result_invalid))
            else -> {
                AccelerationCurveChart(curves = result.accelerationCurves)
            }
        }
    }
}

@Composable
private fun AccelerationCurveChart(curves: Map<Int, List<AccelerationCurvePoint>>) {
    val colors = listOf(
        AppColors.primaryBlue(),
        Color(0xFF4CAF50),
        Color(0xFFFF9800),
        Color(0xFFE91E63),
        Color(0xFF9C27B0),
        Color(0xFF00BCD4),
    )
    val maxSpeed = curves.values.flatten().maxOfOrNull { it.speedKmh } ?: 100.0
    val maxG = max(0.5, curves.values.flatten().maxOfOrNull { it.accelerationG } ?: 1.0)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.6f),
        ) {
            val padding = 40f
            val chartWidth = size.width - padding * 2
            val chartHeight = size.height - padding * 2

            curves.entries.sortedBy { it.key }.forEachIndexed { colorIndex, (_, points) ->
                if (points.size < 2) return@forEachIndexed
                val path = Path()
                points.forEachIndexed { index, point ->
                    val x = padding + (point.speedKmh / maxSpeed).toFloat() * chartWidth
                    val y = padding + chartHeight - (point.accelerationG / maxG).toFloat() * chartHeight
                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(
                    path = path,
                    color = colors[colorIndex % colors.size],
                    style = Stroke(width = 3f),
                )
            }

            drawLine(
                color = Color.Gray.copy(alpha = 0.4f),
                start = Offset(padding, padding),
                end = Offset(padding, padding + chartHeight),
                strokeWidth = 1f,
            )
            drawLine(
                color = Color.Gray.copy(alpha = 0.4f),
                start = Offset(padding, padding + chartHeight),
                end = Offset(padding + chartWidth, padding + chartHeight),
                strokeWidth = 1f,
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            curves.keys.sorted().forEachIndexed { index, gear ->
                Text(
                    text = stringResource(R.string.vehicle_dynamics_gear_chip, gear),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors[index % colors.size],
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Text(
            text = stringResource(R.string.vehicle_dynamics_chart_axis_hint),
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.textSecondary(),
        )
    }
}

private fun VehicleDynamicsPreset.labelRes(): Int = when (this) {
    VehicleDynamicsPreset.MOFA -> R.string.vehicle_dynamics_preset_mofa
    VehicleDynamicsPreset.MOKICK -> R.string.vehicle_dynamics_preset_mokick
    VehicleDynamicsPreset.ROLLER -> R.string.vehicle_dynamics_preset_roller
    VehicleDynamicsPreset.CROSS -> R.string.vehicle_dynamics_preset_cross
    VehicleDynamicsPreset.CUSTOM -> R.string.vehicle_dynamics_preset_custom
}

private fun VehicleDynamicsEngineMode.labelRes(): Int = when (this) {
    VehicleDynamicsEngineMode.POWER -> R.string.vehicle_dynamics_mode_power
    VehicleDynamicsEngineMode.TORQUE -> R.string.vehicle_dynamics_mode_torque
}

private fun parsePositiveInteger(text: String): Int? = text.toIntOrNull()?.takeIf { it > 0 }

private fun String.filterIntegerInput(): String = filter { it.isDigit() }

private fun formatDecimal(value: Double, decimals: Int): String =
    String.format(Locale.US, "%.${decimals}f", value)
