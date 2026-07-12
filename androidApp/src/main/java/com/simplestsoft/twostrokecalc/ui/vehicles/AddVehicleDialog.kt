package com.simplestsoft.twostrokecalc.ui.vehicles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.simplestsoft.twostrokecalc.R
import com.simplestsoft.twostrokecalc.domain.model.VehicleCatalogEntry
import com.simplestsoft.twostrokecalc.domain.model.VehicleType
import com.simplestsoft.twostrokecalc.ui.components.AppColors
import com.simplestsoft.twostrokecalc.ui.components.FormFieldDefaults
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorDecimalField
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorChoiceField
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorDropdownOption
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorInputModeSwitch
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorSection
import com.simplestsoft.twostrokecalc.ui.screens.vehicle.VehicleTextField
import com.simplestsoft.twostrokecalc.ui.theme.ContainerCornerRadius
import com.simplestsoft.twostrokecalc.ui.util.keyboardAwareScroll

data class VehicleBasicInfo(
    val name: String,
    val brand: String,
    val model: String,
    val year: String,
    val vehicleType: VehicleType,
    val licensePlate: String,
    val currentOdometerKm: String,
    val currentOperatingHours: String = "",
) {
    fun isValid(): Boolean =
        name.isNotBlank() || (brand.isNotBlank() && model.isNotBlank())
}

private enum class AddVehicleInputMode { MANUAL, CATALOG }

@Composable
fun AddVehicleDialog(
    visible: Boolean,
    catalogBrands: List<String>,
    catalogEntryCount: Int,
    getModels: (String) -> List<String>,
    getYears: (String, String) -> List<Int>,
    getVariants: (String, String, Int) -> List<VehicleCatalogEntry>,
    onDismiss: () -> Unit,
    onConfirm: (VehicleBasicInfo, VehicleCatalogEntry?) -> Unit,
) {
    if (!visible) return

    var inputModeKey by rememberSaveable { mutableStateOf(AddVehicleInputMode.MANUAL.name) }
    var name by rememberSaveable { mutableStateOf("") }
    var brand by rememberSaveable { mutableStateOf("") }
    var model by rememberSaveable { mutableStateOf("") }
    var year by rememberSaveable { mutableStateOf("") }
    var vehicleTypeKey by rememberSaveable { mutableStateOf(VehicleType.MOFA.name) }
    var licensePlate by rememberSaveable { mutableStateOf("") }
    var odometer by rememberSaveable { mutableStateOf("") }
    var operatingHours by rememberSaveable { mutableStateOf("") }
    var showValidationError by rememberSaveable { mutableStateOf(false) }

    var selectedBrand by rememberSaveable { mutableStateOf("") }
    var selectedModel by rememberSaveable { mutableStateOf("") }
    var selectedYearKey by rememberSaveable { mutableStateOf("") }
    var selectedVariantId by rememberSaveable { mutableStateOf("") }

    val inputMode = AddVehicleInputMode.valueOf(inputModeKey)
    val modelOptions = if (selectedBrand.isNotBlank()) getModels(selectedBrand) else emptyList()
    val yearOptions = if (selectedBrand.isNotBlank() && selectedModel.isNotBlank()) {
        getYears(selectedBrand, selectedModel)
    } else {
        emptyList()
    }
    val selectedYear = selectedYearKey.toIntOrNull()
    val variantOptions = if (selectedBrand.isNotBlank() && selectedModel.isNotBlank() && selectedYear != null) {
        getVariants(selectedBrand, selectedModel, selectedYear)
    } else {
        emptyList()
    }
    val selectedVariant = variantOptions.firstOrNull { it.id == selectedVariantId }
        ?: variantOptions.singleOrNull()

    LaunchedEffect(visible) {
        if (visible) {
            inputModeKey = AddVehicleInputMode.MANUAL.name
            name = ""
            brand = ""
            model = ""
            year = ""
            vehicleTypeKey = VehicleType.MOFA.name
            licensePlate = ""
            odometer = ""
            operatingHours = ""
            showValidationError = false
            selectedBrand = ""
            selectedModel = ""
            selectedYearKey = ""
            selectedVariantId = ""
        }
    }

    LaunchedEffect(selectedBrand) {
        selectedModel = ""
        selectedYearKey = ""
        selectedVariantId = ""
    }

    LaunchedEffect(selectedModel) {
        selectedYearKey = ""
        selectedVariantId = ""
    }

    LaunchedEffect(selectedYearKey) {
        selectedVariantId = ""
    }

    LaunchedEffect(variantOptions) {
        if (variantOptions.size == 1) {
            selectedVariantId = variantOptions.first().id
        }
    }

    LaunchedEffect(inputMode, selectedVariant, selectedYear) {
        if (inputMode == AddVehicleInputMode.CATALOG && selectedVariant != null && selectedYear != null) {
            brand = selectedVariant.brand
            model = selectedVariant.model
            year = selectedYear.toString()
            vehicleTypeKey = selectedVariant.toVehicleType().name
            if (name.isBlank()) {
                name = selectedVariant.displayTitle(selectedYear)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val scrollState = rememberScrollState()
        val shape = RoundedCornerShape(ContainerCornerRadius)

        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = 640.dp),
            shape = shape,
            color = AppColors.surface(),
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .keyboardAwareScroll(scrollState)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(FormFieldDefaults.fieldSpacing),
            ) {
                Text(
                    text = stringResource(R.string.vehicles_add_dialog_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.textPrimary(),
                )
                Text(
                    text = stringResource(R.string.vehicles_add_dialog_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.textSecondary(),
                )
                if (catalogEntryCount > 0) {
                    Text(
                        text = stringResource(R.string.vehicles_catalog_entries_count, catalogEntryCount),
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.textSecondary(),
                    )
                }

                CalculatorInputModeSwitch(
                    optionALabel = stringResource(R.string.vehicles_add_mode_manual),
                    optionBLabel = stringResource(R.string.vehicles_add_mode_catalog),
                    useOptionA = inputMode == AddVehicleInputMode.MANUAL,
                    onUseOptionAChange = { useManual ->
                        inputModeKey = if (useManual) {
                            AddVehicleInputMode.MANUAL.name
                        } else {
                            AddVehicleInputMode.CATALOG.name
                        }
                    },
                )

                if (inputMode == AddVehicleInputMode.CATALOG) {
                    CalculatorSection(title = stringResource(R.string.vehicles_add_mode_catalog)) {
                        CalculatorChoiceField(
                            label = stringResource(R.string.vehicles_catalog_brand),
                            options = listOf(
                                CalculatorDropdownOption("", stringResource(R.string.vehicles_catalog_select_brand)),
                            ) + catalogBrands.map { CalculatorDropdownOption(it, it) },
                            selectedKey = selectedBrand,
                            onOptionSelected = { selectedBrand = it },
                        )
                        if (modelOptions.isNotEmpty()) {
                            CalculatorChoiceField(
                                label = stringResource(R.string.vehicles_catalog_model),
                                options = listOf(
                                    CalculatorDropdownOption("", stringResource(R.string.vehicles_catalog_select_model)),
                                ) + modelOptions.map { CalculatorDropdownOption(it, it) },
                                selectedKey = selectedModel,
                                onOptionSelected = { selectedModel = it },
                            )
                        }
                        if (yearOptions.isNotEmpty()) {
                            CalculatorChoiceField(
                                label = stringResource(R.string.vehicles_catalog_year),
                                options = listOf(
                                    CalculatorDropdownOption("", stringResource(R.string.vehicles_catalog_select_year)),
                                ) + yearOptions.map { y ->
                                    CalculatorDropdownOption(y.toString(), y.toString())
                                },
                                selectedKey = selectedYearKey,
                                onOptionSelected = { selectedYearKey = it },
                            )
                        }
                        if (variantOptions.size > 1) {
                            CalculatorChoiceField(
                                label = stringResource(R.string.vehicles_catalog_variant),
                                options = listOf(
                                    CalculatorDropdownOption("", stringResource(R.string.vehicles_catalog_select_variant)),
                                ) + variantOptions.map { entry ->
                                    CalculatorDropdownOption(
                                        entry.id,
                                        entry.displayModel().ifBlank { entry.model },
                                    )
                                },
                                selectedKey = selectedVariantId,
                                onOptionSelected = { selectedVariantId = it },
                            )
                        }
                        selectedVariant?.let { entry ->
                            CatalogPreview(entry)
                        }
                    }
                }

                VehicleTextField(
                    value = name,
                    label = stringResource(R.string.vehicles_field_name),
                    onValueChange = {
                        name = it
                        showValidationError = false
                    },
                )

                if (inputMode == AddVehicleInputMode.MANUAL) {
                    VehicleTextField(
                        value = brand,
                        label = stringResource(R.string.vehicles_field_brand),
                        onValueChange = {
                            brand = it
                            showValidationError = false
                        },
                    )
                    VehicleTextField(
                        value = model,
                        label = stringResource(R.string.vehicles_field_model),
                        onValueChange = {
                            model = it
                            showValidationError = false
                        },
                    )
                    VehicleTextField(
                        value = year,
                        label = stringResource(R.string.vehicles_field_year),
                        onValueChange = { year = it },
                    )
                    CalculatorChoiceField(
                        label = stringResource(R.string.vehicles_field_type),
                        options = VehicleType.entries.map {
                            CalculatorDropdownOption(it.name, addVehicleTypeLabel(it))
                        },
                        selectedKey = vehicleTypeKey,
                        onOptionSelected = { vehicleTypeKey = it },
                    )
                } else {
                    if (brand.isNotBlank()) {
                        Text(
                            text = "${stringResource(R.string.vehicles_field_brand)}: $brand",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppColors.textSecondary(),
                        )
                    }
                    if (model.isNotBlank()) {
                        Text(
                            text = "${stringResource(R.string.vehicles_field_model)}: $model",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppColors.textSecondary(),
                        )
                    }
                    if (year.isNotBlank()) {
                        Text(
                            text = "${stringResource(R.string.vehicles_field_year)}: $year",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppColors.textSecondary(),
                        )
                    }
                }

                VehicleTextField(
                    value = licensePlate,
                    label = stringResource(R.string.vehicles_field_license_plate),
                    onValueChange = { licensePlate = it },
                )
                CalculatorDecimalField(
                    value = odometer,
                    onValueChange = { odometer = it },
                    label = stringResource(R.string.vehicles_field_odometer),
                    suffix = "km",
                )
                CalculatorDecimalField(
                    value = operatingHours,
                    onValueChange = { operatingHours = it },
                    label = stringResource(R.string.vehicles_field_operating_hours),
                    suffix = "h",
                )

                if (showValidationError) {
                    Text(
                        text = stringResource(R.string.vehicles_add_validation_error),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                    Button(
                        onClick = {
                            val catalogEntry = if (inputMode == AddVehicleInputMode.CATALOG) {
                                selectedVariant
                            } else {
                                null
                            }
                            val info = VehicleBasicInfo(
                                name = name.trim(),
                                brand = brand.trim(),
                                model = model.trim(),
                                year = year.trim(),
                                vehicleType = if (catalogEntry != null) {
                                    catalogEntry.toVehicleType()
                                } else {
                                    VehicleType.valueOf(vehicleTypeKey)
                                },
                                licensePlate = licensePlate.trim(),
                                currentOdometerKm = odometer.trim(),
                                currentOperatingHours = operatingHours.trim(),
                            )
                            if (!info.isValid()) {
                                showValidationError = true
                                return@Button
                            }
                            if (inputMode == AddVehicleInputMode.CATALOG && catalogEntry == null) {
                                showValidationError = true
                                return@Button
                            }
                            onConfirm(info, catalogEntry)
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.vehicles_add_dialog_confirm))
                    }
                }
            }
        }
    }
}

@Composable
private fun CatalogPreview(entry: VehicleCatalogEntry) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(R.string.vehicles_catalog_preview_title),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.textPrimary(),
        )
        if (entry.engine.displacementCc.isNotBlank()) {
            Text(
                text = stringResource(
                    R.string.vehicles_catalog_preview_displacement,
                    entry.engine.displacementCc,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.textSecondary(),
            )
        }
        if (entry.engine.boreMm.isNotBlank() && entry.engine.strokeMm.isNotBlank()) {
            Text(
                text = stringResource(
                    R.string.vehicles_catalog_preview_bore_stroke,
                    entry.engine.boreMm,
                    entry.engine.strokeMm,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.textSecondary(),
            )
        }
        if (entry.carbIgnition.carbType.isNotBlank()) {
            Text(
                text = stringResource(
                    R.string.vehicles_catalog_preview_carb,
                    entry.carbIgnition.carbType,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.textSecondary(),
            )
        }
        if (entry.frameCode.isNotBlank()) {
            Text(
                text = stringResource(
                    R.string.vehicles_catalog_preview_frame,
                    entry.frameCode,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.textSecondary(),
            )
        }
    }
}

@Composable
private fun addVehicleTypeLabel(type: VehicleType): String = when (type) {
    VehicleType.MOFA -> stringResource(R.string.vehicles_type_mofa)
    VehicleType.MOKICK -> stringResource(R.string.vehicles_type_mokick)
    VehicleType.ROLLER -> stringResource(R.string.vehicles_type_roller)
    VehicleType.CROSS -> stringResource(R.string.vehicles_type_cross)
    VehicleType.FOUR_WHEEL -> stringResource(R.string.vehicles_type_four_wheel)
    VehicleType.OTHER -> stringResource(R.string.vehicles_type_other)
}
