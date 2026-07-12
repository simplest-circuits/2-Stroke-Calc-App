package com.simplestsoft.twostrokecalc.ui.screens.calculators

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simplestsoft.twostrokecalc.R
import com.simplestsoft.twostrokecalc.domain.calculation.DynoRunSimulationInput
import com.simplestsoft.twostrokecalc.domain.calculation.DynoRunSimulationResult
import com.simplestsoft.twostrokecalc.domain.calculation.DynoRunSamplePoint
import com.simplestsoft.twostrokecalc.domain.calculation.EngineDynoDesignInput
import com.simplestsoft.twostrokecalc.domain.calculation.EngineDynoDesignResult
import com.simplestsoft.twostrokecalc.domain.calculation.FlywheelCalcMode
import com.simplestsoft.twostrokecalc.domain.calculation.FlywheelCylinderType
import com.simplestsoft.twostrokecalc.domain.calculation.FlywheelGeometryInput
import com.simplestsoft.twostrokecalc.domain.calculation.FlywheelInertiaCalculator
import com.simplestsoft.twostrokecalc.domain.calculation.FlywheelInertiaResult
import com.simplestsoft.twostrokecalc.domain.calculation.FlywheelMassInput
import com.simplestsoft.twostrokecalc.domain.calculation.FlywheelMaterial
import com.simplestsoft.twostrokecalc.domain.calculation.FlywheelTorqueModel
import com.simplestsoft.twostrokecalc.domain.calculation.SpeedReductionInput
import com.simplestsoft.twostrokecalc.domain.calculation.SpeedReductionResult
import com.simplestsoft.twostrokecalc.ui.components.AppColors
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorDecimalField
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorChoiceField
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorDropdownOption
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorEmptyResultText
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

@Composable
fun FlywheelInertiaCalculatorScreen() {
    var modeName by rememberSaveable { mutableStateOf(FlywheelCalcMode.GEOMETRY.name) }
    val mode = FlywheelCalcMode.entries.firstOrNull { it.name == modeName } ?: FlywheelCalcMode.GEOMETRY

    var cylinderTypeName by rememberSaveable { mutableStateOf(FlywheelCylinderType.SOLID.name) }
    val cylinderType = FlywheelCylinderType.entries.firstOrNull { it.name == cylinderTypeName }
        ?: FlywheelCylinderType.SOLID

    var materialName by rememberSaveable { mutableStateOf(FlywheelMaterial.STEEL.name) }
    val material = FlywheelMaterial.entries.firstOrNull { it.name == materialName }
        ?: FlywheelMaterial.STEEL

    var outerDiameterText by rememberSaveable { mutableStateOf("300") }
    var innerDiameterText by rememberSaveable { mutableStateOf("0") }
    var lengthText by rememberSaveable { mutableStateOf("100") }
    var customDensityText by rememberSaveable { mutableStateOf("7850") }
    var massText by rememberSaveable { mutableStateOf("38") }
    var rollerRadiusText by rememberSaveable { mutableStateOf("150") }
    var additionalInertiaText by rememberSaveable { mutableStateOf("0") }

    var peakPowerText by rememberSaveable { mutableStateOf("50") }
    var peakRpmText by rememberSaveable { mutableStateOf("10000") }
    var runTimeText by rememberSaveable { mutableStateOf("10") }
    var ratioText by rememberSaveable { mutableStateOf("1") }
    var engineInertiaText by rememberSaveable { mutableStateOf("0") }

    var sourceInertiaText by rememberSaveable { mutableStateOf("2") }
    var sourceRpmText by rememberSaveable { mutableStateOf("3000") }
    var targetRpmText by rememberSaveable { mutableStateOf("1500") }

    var simInertiaText by rememberSaveable { mutableStateOf("0.5") }
    var simStartRpmText by rememberSaveable { mutableStateOf("3000") }
    var torqueModelName by rememberSaveable {
        mutableStateOf(FlywheelTorqueModel.CONSTANT_TORQUE.name)
    }
    val torqueModel = FlywheelTorqueModel.entries.firstOrNull { it.name == torqueModelName }
        ?: FlywheelTorqueModel.CONSTANT_TORQUE

    val outerDiameter = parseDecimal(outerDiameterText)
    val innerDiameter = parseDecimal(innerDiameterText)
    val length = parseDecimal(lengthText)
    val customDensity = parseDecimal(customDensityText)
    val mass = parseDecimal(massText)
    val rollerRadius = parseDecimal(rollerRadiusText)
    val additionalInertia = parseDecimal(additionalInertiaText) ?: 0.0

    val peakPower = parseDecimal(peakPowerText)
    val peakRpm = parseDecimal(peakRpmText)
    val runTime = parseDecimal(runTimeText)
    val ratio = parseDecimal(ratioText)
    val engineInertia = parseDecimal(engineInertiaText) ?: 0.0

    val sourceInertia = parseDecimal(sourceInertiaText)
    val sourceRpm = parseDecimal(sourceRpmText)
    val targetRpm = parseDecimal(targetRpmText)

    val simInertia = parseDecimal(simInertiaText)
    val simStartRpm = parseDecimal(simStartRpmText)
    val simEngineInertia = parseDecimal(engineInertiaText) ?: 0.0

    val geometryResult = if (
        mode == FlywheelCalcMode.GEOMETRY &&
        outerDiameter != null && length != null
    ) {
        FlywheelInertiaCalculator.inertiaFromGeometry(
            FlywheelGeometryInput(
                cylinderType = cylinderType,
                outerDiameterMm = outerDiameter,
                innerDiameterMm = innerDiameter ?: 0.0,
                lengthMm = length,
                material = material,
                customDensityKgM3 = customDensity ?: FlywheelInertiaCalculator.DEFAULT_STEEL_DENSITY_KG_M3,
                rollerRadiusMm = rollerRadius,
                additionalInertiaKgm2 = additionalInertia,
            ),
        )
    } else {
        null
    }

    val massResult = if (
        mode == FlywheelCalcMode.FROM_MASS &&
        mass != null && outerDiameter != null
    ) {
        FlywheelInertiaCalculator.inertiaFromMass(
            FlywheelMassInput(
                cylinderType = cylinderType,
                massKg = mass,
                outerDiameterMm = outerDiameter,
                innerDiameterMm = innerDiameter ?: 0.0,
                rollerRadiusMm = rollerRadius,
                additionalInertiaKgm2 = additionalInertia,
            ),
        )
    } else {
        null
    }

    val designResult = if (
        mode == FlywheelCalcMode.ENGINE_DYNO_DESIGN &&
        peakPower != null && peakRpm != null && runTime != null && ratio != null
    ) {
        FlywheelInertiaCalculator.designEngineDyno(
            EngineDynoDesignInput(
                peakPowerPs = peakPower,
                peakEngineRpm = peakRpm,
                runTimeSeconds = runTime,
                engineToFlywheelRatio = ratio,
                engineInertiaKgm2 = engineInertia,
                rollerRadiusMm = rollerRadius,
            ),
        )
    } else {
        null
    }

    val reductionResult = if (
        mode == FlywheelCalcMode.SPEED_REDUCTION &&
        sourceInertia != null && sourceRpm != null && targetRpm != null
    ) {
        FlywheelInertiaCalculator.reduceInertiaToSpeed(
            SpeedReductionInput(
                inertiaKgm2 = sourceInertia,
                sourceRpm = sourceRpm,
                targetRpm = targetRpm,
            ),
        )
    } else {
        null
    }

    val simulationResult = if (
        mode == FlywheelCalcMode.SIMULATION &&
        simInertia != null &&
        peakPower != null &&
        peakRpm != null &&
        simStartRpm != null
    ) {
        val totalInertia = simInertia + simEngineInertia
        FlywheelInertiaCalculator.simulateDynoRun(
            DynoRunSimulationInput(
                totalInertiaKgm2 = totalInertia,
                peakPowerPs = peakPower,
                peakEngineRpm = peakRpm,
                startRpm = simStartRpm,
                torqueModel = torqueModel,
            ),
        )
    } else {
        null
    }

    CalculatorScaffold {
        CalculatorHeader(
            title = stringResource(R.string.flywheel_calculator_title),
            subtitle = stringResource(R.string.flywheel_calculator_subtitle),
        )

        val modeOptions = FlywheelCalcMode.entries.map { item ->
            CalculatorDropdownOption(item.name, stringResource(item.labelRes()))
        }

        CalculatorChoiceField(
            label = stringResource(R.string.flywheel_section_mode),
            options = modeOptions,
            selectedKey = modeName,
            onOptionSelected = { modeName = it },
            supportingText = stringResource(mode.hintRes()),
        )

        when (mode) {
            FlywheelCalcMode.GEOMETRY,
            FlywheelCalcMode.FROM_MASS,
            -> {
                if (mode == FlywheelCalcMode.GEOMETRY) {
                    CylinderAndMaterialFields(
                        cylinderType = cylinderType,
                        onCylinderTypeChange = { cylinderTypeName = it.name },
                        material = material,
                        onMaterialChange = { materialName = it.name },
                        customDensityText = customDensityText,
                        onCustomDensityChange = { customDensityText = it.filterDecimalInput() },
                    )
                } else {
                    CalculatorSection(title = stringResource(R.string.flywheel_section_cylinder_type)) {
                        val cylinderTypeOptions = FlywheelCylinderType.entries.map { type ->
                            CalculatorDropdownOption(type.name, stringResource(type.labelRes()))
                        }
                        CalculatorChoiceField(
                            label = "",
                            options = cylinderTypeOptions,
                            selectedKey = cylinderType.name,
                            onOptionSelected = { cylinderTypeName = it },
                        )
                    }
                }

                DimensionFields(
                    cylinderType = cylinderType,
                    outerDiameterText = outerDiameterText,
                    onOuterDiameterChange = { outerDiameterText = it.filterDecimalInput() },
                    innerDiameterText = innerDiameterText,
                    onInnerDiameterChange = { innerDiameterText = it.filterDecimalInput() },
                    lengthText = lengthText,
                    onLengthChange = { lengthText = it.filterDecimalInput() },
                    showLength = mode == FlywheelCalcMode.GEOMETRY,
                    massText = massText,
                    onMassChange = { massText = it.filterDecimalInput() },
                    showMass = mode == FlywheelCalcMode.FROM_MASS,
                )
            }
            FlywheelCalcMode.ENGINE_DYNO_DESIGN -> {
                CalculatorSection(title = stringResource(R.string.flywheel_section_engine)) {
                    CalculatorDecimalField(
                        value = peakPowerText,
                        onValueChange = { peakPowerText = it.filterDecimalInput() },
                        label = stringResource(R.string.flywheel_peak_power_label),
                        suffix = stringResource(R.string.flywheel_unit_ps),
                    )
                    CalculatorDecimalField(
                        value = peakRpmText,
                        onValueChange = { peakRpmText = it.filterDecimalInput() },
                        label = stringResource(R.string.flywheel_peak_rpm_label),
                        suffix = stringResource(R.string.flywheel_unit_rpm),
                    )
                    CalculatorDecimalField(
                        value = runTimeText,
                        onValueChange = { runTimeText = it.filterDecimalInput() },
                        label = stringResource(R.string.flywheel_run_time_label),
                        suffix = stringResource(R.string.flywheel_unit_seconds),
                        supportingText = stringResource(R.string.flywheel_run_time_hint),
                    )
                    CalculatorDecimalField(
                        value = ratioText,
                        onValueChange = { ratioText = it.filterDecimalInput() },
                        label = stringResource(R.string.flywheel_ratio_label),
                        supportingText = stringResource(R.string.flywheel_ratio_hint),
                    )
                    CalculatorDecimalField(
                        value = engineInertiaText,
                        onValueChange = { engineInertiaText = it.filterDecimalInput() },
                        label = stringResource(R.string.flywheel_engine_inertia_label),
                        suffix = stringResource(R.string.flywheel_unit_kgm2),
                        supportingText = stringResource(R.string.flywheel_engine_inertia_hint),
                    )
                }
            }
            FlywheelCalcMode.SPEED_REDUCTION -> {
                CalculatorSection(title = stringResource(R.string.flywheel_section_reduction)) {
                    CalculatorDecimalField(
                        value = sourceInertiaText,
                        onValueChange = { sourceInertiaText = it.filterDecimalInput() },
                        label = stringResource(R.string.flywheel_source_inertia_label),
                        suffix = stringResource(R.string.flywheel_unit_kgm2),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CalculatorDecimalField(
                            value = sourceRpmText,
                            onValueChange = { sourceRpmText = it.filterDecimalInput() },
                            label = stringResource(R.string.flywheel_source_rpm_label),
                            suffix = stringResource(R.string.flywheel_unit_rpm),
                            modifier = Modifier.weight(1f),
                        )
                        CalculatorDecimalField(
                            value = targetRpmText,
                            onValueChange = { targetRpmText = it.filterDecimalInput() },
                            label = stringResource(R.string.flywheel_target_rpm_label),
                            suffix = stringResource(R.string.flywheel_unit_rpm),
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Text(
                        text = stringResource(R.string.flywheel_reduction_formula_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.textSecondary(),
                    )
                }
            }
            FlywheelCalcMode.SIMULATION -> {
                val torqueModelOptions = FlywheelTorqueModel.entries.map { model ->
                    CalculatorDropdownOption(model.name, stringResource(model.labelRes()))
                }

                CalculatorSection(title = stringResource(R.string.flywheel_section_simulation)) {
                    CalculatorDecimalField(
                        value = simInertiaText,
                        onValueChange = { simInertiaText = it.filterDecimalInput() },
                        label = stringResource(R.string.flywheel_sim_inertia_label),
                        suffix = stringResource(R.string.flywheel_unit_kgm2),
                        supportingText = stringResource(R.string.flywheel_sim_inertia_hint),
                    )
                    CalculatorDecimalField(
                        value = peakPowerText,
                        onValueChange = { peakPowerText = it.filterDecimalInput() },
                        label = stringResource(R.string.flywheel_peak_power_label),
                        suffix = stringResource(R.string.flywheel_unit_ps),
                    )
                    CalculatorDecimalField(
                        value = peakRpmText,
                        onValueChange = { peakRpmText = it.filterDecimalInput() },
                        label = stringResource(R.string.flywheel_peak_rpm_label),
                        suffix = stringResource(R.string.flywheel_unit_rpm),
                    )
                    CalculatorDecimalField(
                        value = simStartRpmText,
                        onValueChange = { simStartRpmText = it.filterDecimalInput() },
                        label = stringResource(R.string.flywheel_sim_start_rpm_label),
                        suffix = stringResource(R.string.flywheel_unit_rpm),
                    )
                    CalculatorDecimalField(
                        value = engineInertiaText,
                        onValueChange = { engineInertiaText = it.filterDecimalInput() },
                        label = stringResource(R.string.flywheel_engine_inertia_label),
                        suffix = stringResource(R.string.flywheel_unit_kgm2),
                        supportingText = stringResource(R.string.flywheel_engine_inertia_hint),
                    )
                    CalculatorChoiceField(
                        label = stringResource(R.string.flywheel_torque_model_label),
                        options = torqueModelOptions,
                        selectedKey = torqueModelName,
                        onOptionSelected = { torqueModelName = it },
                        supportingText = stringResource(torqueModel.hintRes()),
                    )
                }
            }
        }

        if (mode != FlywheelCalcMode.SPEED_REDUCTION && mode != FlywheelCalcMode.SIMULATION) {
            CalculatorSection(title = stringResource(R.string.flywheel_section_roller)) {
                CalculatorDecimalField(
                    value = rollerRadiusText,
                    onValueChange = { rollerRadiusText = it.filterDecimalInput() },
                    label = stringResource(R.string.flywheel_roller_radius_label),
                    suffix = stringResource(R.string.flywheel_unit_mm),
                    supportingText = stringResource(R.string.flywheel_roller_radius_hint),
                )
                if (mode != FlywheelCalcMode.ENGINE_DYNO_DESIGN) {
                    CalculatorDecimalField(
                        value = additionalInertiaText,
                        onValueChange = { additionalInertiaText = it.filterDecimalInput() },
                        label = stringResource(R.string.flywheel_additional_inertia_label),
                        suffix = stringResource(R.string.flywheel_unit_kgm2),
                        supportingText = stringResource(R.string.flywheel_additional_inertia_hint),
                    )
                }
            }
        }

        when (mode) {
            FlywheelCalcMode.GEOMETRY -> GeometryResultCard(
                result = geometryResult,
                hasInput = outerDiameter != null && length != null,
            )
            FlywheelCalcMode.FROM_MASS -> GeometryResultCard(
                result = massResult,
                hasInput = mass != null && outerDiameter != null,
            )
            FlywheelCalcMode.ENGINE_DYNO_DESIGN -> DesignResultCard(
                result = designResult,
                hasInput = peakPower != null && peakRpm != null && runTime != null && ratio != null,
            )
            FlywheelCalcMode.SPEED_REDUCTION -> ReductionResultCard(
                result = reductionResult,
                hasInput = sourceInertia != null && sourceRpm != null && targetRpm != null,
            )
            FlywheelCalcMode.SIMULATION -> SimulationResultSection(
                result = simulationResult,
                hasInput = simInertia != null && peakPower != null && peakRpm != null && simStartRpm != null,
            )
        }

        if (mode != FlywheelCalcMode.SIMULATION) {
            Text(
                text = stringResource(R.string.flywheel_guidelines_hint),
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.textSecondary(),
            )
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun CylinderAndMaterialFields(
    cylinderType: FlywheelCylinderType,
    onCylinderTypeChange: (FlywheelCylinderType) -> Unit,
    material: FlywheelMaterial,
    onMaterialChange: (FlywheelMaterial) -> Unit,
    customDensityText: String,
    onCustomDensityChange: (String) -> Unit,
) {
    CalculatorSection(title = stringResource(R.string.flywheel_section_cylinder_type)) {
        val cylinderTypeOptions = FlywheelCylinderType.entries.map { type ->
            CalculatorDropdownOption(type.name, stringResource(type.labelRes()))
        }
        CalculatorChoiceField(
            label = "",
            options = cylinderTypeOptions,
            selectedKey = cylinderType.name,
            onOptionSelected = { onCylinderTypeChange(FlywheelCylinderType.valueOf(it)) },
        )
    }

    CalculatorSection(title = stringResource(R.string.flywheel_section_material)) {
        val materialOptions = FlywheelMaterial.entries.map { item ->
            CalculatorDropdownOption(item.name, stringResource(item.labelRes()))
        }
        CalculatorChoiceField(
            label = "",
            options = materialOptions,
            selectedKey = material.name,
            onOptionSelected = { onMaterialChange(FlywheelMaterial.valueOf(it)) },
        )
        if (material == FlywheelMaterial.CUSTOM) {
            CalculatorDecimalField(
                value = customDensityText,
                onValueChange = onCustomDensityChange,
                label = stringResource(R.string.flywheel_custom_density_label),
                suffix = stringResource(R.string.flywheel_unit_density),
            )
        }
    }
}

@Composable
private fun DimensionFields(
    cylinderType: FlywheelCylinderType,
    outerDiameterText: String,
    onOuterDiameterChange: (String) -> Unit,
    innerDiameterText: String,
    onInnerDiameterChange: (String) -> Unit,
    lengthText: String,
    onLengthChange: (String) -> Unit,
    showLength: Boolean,
    massText: String,
    onMassChange: (String) -> Unit,
    showMass: Boolean,
) {
    CalculatorSection(title = stringResource(R.string.flywheel_section_dimensions)) {
        CalculatorDecimalField(
            value = outerDiameterText,
            onValueChange = onOuterDiameterChange,
            label = stringResource(R.string.flywheel_outer_diameter_label),
            suffix = stringResource(R.string.flywheel_unit_mm),
        )
        if (cylinderType == FlywheelCylinderType.HOLLOW) {
            CalculatorDecimalField(
                value = innerDiameterText,
                onValueChange = onInnerDiameterChange,
                label = stringResource(R.string.flywheel_inner_diameter_label),
                suffix = stringResource(R.string.flywheel_unit_mm),
            )
        }
        if (showLength) {
            CalculatorDecimalField(
                value = lengthText,
                onValueChange = onLengthChange,
                label = stringResource(R.string.flywheel_length_label),
                suffix = stringResource(R.string.flywheel_unit_mm),
            )
        }
        if (showMass) {
            CalculatorDecimalField(
                value = massText,
                onValueChange = onMassChange,
                label = stringResource(R.string.flywheel_mass_label),
                suffix = stringResource(R.string.flywheel_unit_kg),
            )
        }
    }
}

@Composable
private fun GeometryResultCard(
    result: FlywheelInertiaResult?,
    hasInput: Boolean,
) {
    CalculatorResultCard(title = stringResource(R.string.flywheel_result_title)) {
        when {
            !hasInput -> CalculatorEmptyResultText(stringResource(R.string.flywheel_result_empty))
            result == null -> CalculatorInvalidResultText(stringResource(R.string.flywheel_result_invalid))
            else -> {
                CalculatorPrimaryResultText(
                    text = stringResource(
                        R.string.flywheel_result_inertia,
                        formatInertia(result.inertiaKgm2),
                    ),
                )
                result.massKg?.let { mass ->
                    CalculatorResultRow(
                        label = stringResource(R.string.flywheel_result_mass_label),
                        value = stringResource(R.string.flywheel_result_mass_value, formatMass(mass)),
                    )
                }
                result.equivalentMassKg?.let { em ->
                    CalculatorResultDivider()
                    CalculatorResultRow(
                        label = stringResource(R.string.flywheel_result_equivalent_mass_label),
                        value = stringResource(R.string.flywheel_result_equivalent_mass_value, formatMass(em)),
                        hint = stringResource(R.string.flywheel_result_equivalent_mass_hint),
                    )
                }
            }
        }
    }
}

@Composable
private fun DesignResultCard(
    result: EngineDynoDesignResult?,
    hasInput: Boolean,
) {
    CalculatorResultCard(title = stringResource(R.string.flywheel_result_title)) {
        when {
            !hasInput -> CalculatorEmptyResultText(stringResource(R.string.flywheel_result_empty))
            result == null -> CalculatorInvalidResultText(stringResource(R.string.flywheel_result_invalid))
            else -> {
                CalculatorPrimaryResultText(
                    text = stringResource(
                        R.string.flywheel_result_required_inertia,
                        formatInertia(result.requiredFlywheelInertiaKgm2),
                    ),
                )
                if (result.totalInertiaKgm2 != result.requiredFlywheelInertiaKgm2) {
                    CalculatorResultRow(
                        label = stringResource(R.string.flywheel_result_total_inertia_label),
                        value = formatInertia(result.totalInertiaKgm2),
                    )
                }
                result.equivalentMassKg?.let { em ->
                    CalculatorResultRow(
                        label = stringResource(R.string.flywheel_result_equivalent_mass_label),
                        value = stringResource(R.string.flywheel_result_equivalent_mass_value, formatMass(em)),
                    )
                }
            }
        }
    }
}

@Composable
private fun SimulationResultSection(
    result: DynoRunSimulationResult?,
    hasInput: Boolean,
) {
    CalculatorResultCard(title = stringResource(R.string.flywheel_sim_result_title)) {
        when {
            !hasInput -> CalculatorEmptyResultText(stringResource(R.string.flywheel_result_empty))
            result == null -> CalculatorInvalidResultText(stringResource(R.string.flywheel_sim_result_invalid))
            else -> {
                val lastSample = result.samples.last()
                CalculatorPrimaryResultText(
                    text = stringResource(
                        R.string.flywheel_sim_result_run_time,
                        formatDecimal(result.runTimeToPeakSeconds, 2),
                    ),
                )
                CalculatorResultRow(
                    label = stringResource(R.string.flywheel_sim_result_peak_rpm_label),
                    value = stringResource(
                        R.string.flywheel_result_rpm_value,
                        formatDecimal(lastSample.engineRpm, 0),
                    ),
                )
                CalculatorResultRow(
                    label = stringResource(R.string.flywheel_sim_result_peak_torque_label),
                    value = stringResource(
                        R.string.flywheel_sim_result_torque_value,
                        formatDecimal(result.peakTorqueNm, 1),
                    ),
                )
                CalculatorResultRow(
                    label = stringResource(R.string.flywheel_sim_result_start_accel_label),
                    value = stringResource(
                        R.string.flywheel_sim_result_accel_value,
                        formatDecimal(result.samples.first().accelerationRpmPerSec, 0),
                    ),
                )
            }
        }
    }

    if (result != null) {
        CalculatorSection(title = stringResource(R.string.flywheel_sim_chart_title)) {
            Text(
                text = stringResource(R.string.flywheel_sim_chart_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.textSecondary(),
            )
            DynoRunChart(samples = result.samples)
        }
    }
}

@Composable
private fun DynoRunChart(
    samples: List<DynoRunSamplePoint>,
    modifier: Modifier = Modifier,
) {
    if (samples.size < 2) return

    val rpmColor = AppColors.primaryBlue()
    val powerColor = Color(0xFF4CAF50)
    val gridColor = AppColors.borderSubtle().copy(alpha = 0.45f)
    val axisLabelColor = AppColors.textSecondary()
    val axisLabelStyle = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)

    val maxTime = samples.maxOf { it.timeSeconds }.coerceAtLeast(0.001)
    val maxRpm = samples.maxOf { it.engineRpm }.coerceAtLeast(1.0)
    val maxPower = samples.maxOf { it.powerPs }.coerceAtLeast(1.0)

    Column(modifier = modifier.fillMaxWidth()) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.7f)
                .padding(top = 8.dp),
        ) {
            val yAxisWidth = 40.dp
            val xAxisHeight = 28.dp
            val chartTopInset = 12.dp
            val chartWidth = maxWidth - yAxisWidth
            val chartHeight = maxHeight - xAxisHeight

            Text(
                text = stringResource(R.string.flywheel_sim_chart_axis_rpm),
                style = axisLabelStyle,
                color = axisLabelColor,
                modifier = Modifier.width(yAxisWidth),
            )

            Box(
                modifier = Modifier
                    .width(yAxisWidth)
                    .height(chartHeight)
                    .padding(top = chartTopInset),
            ) {
                listOf(0, 1, 2, 3, 4).forEach { step ->
                    val fraction = step / 4f
                    Text(
                        text = formatDecimal(maxRpm * fraction, 0),
                        style = axisLabelStyle,
                        color = axisLabelColor,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(y = chartHeight * (1f - fraction) - 6.dp),
                    )
                }
            }

            Canvas(
                modifier = Modifier
                    .padding(start = yAxisWidth, top = chartTopInset)
                    .width(chartWidth)
                    .height(chartHeight),
            ) {
                val left = size.width * 0.04f
                val right = size.width * 0.98f
                val top = size.height * 0.04f
                val bottom = size.height * 0.96f

                for (step in 0..4) {
                    val y = bottom - (step / 4f) * (bottom - top)
                    drawLine(gridColor, Offset(left, y), Offset(right, y), strokeWidth = 1f)
                }
                for (step in 0..4) {
                    val x = left + (step / 4f) * (right - left)
                    drawLine(gridColor, Offset(x, top), Offset(x, bottom), strokeWidth = 1f)
                }

                fun point(time: Double, normalizedValue: Double): Offset {
                    val x = left + (time / maxTime).toFloat().coerceIn(0f, 1f) * (right - left)
                    val y = bottom - normalizedValue.toFloat().coerceIn(0f, 1f) * (bottom - top)
                    return Offset(x, y)
                }

                val rpmPath = Path().apply {
                    samples.forEachIndexed { index, sample ->
                        val offset = point(sample.timeSeconds, sample.engineRpm / maxRpm)
                        if (index == 0) moveTo(offset.x, offset.y) else lineTo(offset.x, offset.y)
                    }
                }
                drawPath(rpmPath, rpmColor, style = Stroke(width = 3f))

                val powerPath = Path().apply {
                    samples.forEachIndexed { index, sample ->
                        val offset = point(sample.timeSeconds, sample.powerPs / maxPower)
                        if (index == 0) moveTo(offset.x, offset.y) else lineTo(offset.x, offset.y)
                    }
                }
                drawPath(powerPath, powerColor, style = Stroke(width = 2.5f))
            }

            Box(
                modifier = Modifier
                    .padding(start = yAxisWidth, top = chartTopInset)
                    .width(chartWidth)
                    .height(xAxisHeight - 8.dp)
                    .offset(y = chartHeight),
            ) {
                listOf(0, 1, 2, 3, 4).forEach { step ->
                    val fraction = step / 4f
                    Text(
                        text = formatDecimal(maxTime * fraction, 1),
                        style = axisLabelStyle,
                        color = axisLabelColor,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(x = chartWidth * fraction - 12.dp),
                    )
                }
            }

            Text(
                text = stringResource(R.string.flywheel_sim_chart_axis_time),
                style = axisLabelStyle,
                color = axisLabelColor,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = yAxisWidth),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            DynoChartLegendItem(color = rpmColor, label = stringResource(R.string.flywheel_sim_chart_legend_rpm))
            DynoChartLegendItem(color = powerColor, label = stringResource(R.string.flywheel_sim_chart_legend_power))
        }
    }
}

@Composable
private fun DynoChartLegendItem(
    color: Color,
    label: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(width = 20.dp, height = 10.dp)) {
            drawLine(
                color = color,
                start = Offset(0f, size.height / 2f),
                end = Offset(size.width, size.height / 2f),
                strokeWidth = 3f,
            )
        }
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.textSecondary(),
        )
    }
}

@Composable
private fun ReductionResultCard(
    result: SpeedReductionResult?,
    hasInput: Boolean,
) {
    CalculatorResultCard(title = stringResource(R.string.flywheel_result_title)) {
        when {
            !hasInput -> CalculatorEmptyResultText(stringResource(R.string.flywheel_result_empty))
            result == null -> CalculatorInvalidResultText(stringResource(R.string.flywheel_result_invalid))
            else -> {
                CalculatorPrimaryResultText(
                    text = stringResource(
                        R.string.flywheel_result_reduced_inertia,
                        formatInertia(result.reducedInertiaKgm2),
                    ),
                )
            }
        }
    }
}

@StringRes
private fun FlywheelCalcMode.labelRes(): Int = when (this) {
    FlywheelCalcMode.GEOMETRY -> R.string.flywheel_mode_geometry
    FlywheelCalcMode.FROM_MASS -> R.string.flywheel_mode_from_mass
    FlywheelCalcMode.ENGINE_DYNO_DESIGN -> R.string.flywheel_mode_engine_dyno
    FlywheelCalcMode.SPEED_REDUCTION -> R.string.flywheel_mode_speed_reduction
    FlywheelCalcMode.SIMULATION -> R.string.flywheel_mode_simulation
}

@StringRes
private fun FlywheelCalcMode.hintRes(): Int = when (this) {
    FlywheelCalcMode.GEOMETRY -> R.string.flywheel_mode_geometry_hint
    FlywheelCalcMode.FROM_MASS -> R.string.flywheel_mode_from_mass_hint
    FlywheelCalcMode.ENGINE_DYNO_DESIGN -> R.string.flywheel_mode_engine_dyno_hint
    FlywheelCalcMode.SPEED_REDUCTION -> R.string.flywheel_mode_speed_reduction_hint
    FlywheelCalcMode.SIMULATION -> R.string.flywheel_mode_simulation_hint
}

@StringRes
private fun FlywheelTorqueModel.labelRes(): Int = when (this) {
    FlywheelTorqueModel.CONSTANT_TORQUE -> R.string.flywheel_torque_model_constant_torque
    FlywheelTorqueModel.CONSTANT_POWER -> R.string.flywheel_torque_model_constant_power
}

@StringRes
private fun FlywheelTorqueModel.hintRes(): Int = when (this) {
    FlywheelTorqueModel.CONSTANT_TORQUE -> R.string.flywheel_torque_model_constant_torque_hint
    FlywheelTorqueModel.CONSTANT_POWER -> R.string.flywheel_torque_model_constant_power_hint
}

@StringRes
private fun FlywheelCylinderType.labelRes(): Int = when (this) {
    FlywheelCylinderType.SOLID -> R.string.flywheel_cylinder_solid
    FlywheelCylinderType.HOLLOW -> R.string.flywheel_cylinder_hollow
}

@StringRes
private fun FlywheelMaterial.labelRes(): Int = when (this) {
    FlywheelMaterial.STEEL -> R.string.flywheel_material_steel
    FlywheelMaterial.CAST_IRON -> R.string.flywheel_material_cast_iron
    FlywheelMaterial.ALUMINUM -> R.string.flywheel_material_aluminum
    FlywheelMaterial.CUSTOM -> R.string.flywheel_material_custom
}

private fun formatInertia(value: Double): String =
    String.format(Locale.getDefault(), "%.4f", value)

private fun formatMass(value: Double): String =
    String.format(Locale.getDefault(), "%.1f", value)

private fun formatDecimal(value: Double, decimals: Int): String =
    String.format(Locale.getDefault(), "%.${decimals}f", value)
