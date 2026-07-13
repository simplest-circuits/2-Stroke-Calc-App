package com.simplestsoft.twostrokecalc.ui.screens.vehicle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.simplestsoft.twostrokecalc.R
import com.simplestsoft.twostrokecalc.domain.model.MaintenanceEntry
import com.simplestsoft.twostrokecalc.domain.model.MaintenanceType
import com.simplestsoft.twostrokecalc.ui.components.FormFieldDefaults
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorDecimalField
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorChoiceField
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorDropdownOption

@Composable
internal fun MaintenanceEntryDialog(
    entry: MaintenanceEntry,
    isNewEntry: Boolean,
    maintenanceTypes: List<MaintenanceType>,
    onDismiss: () -> Unit,
    onConfirm: (MaintenanceEntry) -> Unit,
) {
    var date by rememberSaveable(entry.id) { mutableStateOf(entry.date) }
    var odometer by rememberSaveable(entry.id) { mutableStateOf(entry.odometerKm) }
    var type by rememberSaveable(entry.id) {
        mutableStateOf(
            entry.type.name.takeIf { maintenanceTypes.any { t -> t.name == entry.type.name } }
                ?: maintenanceTypes.first().name,
        )
    }
    var description by rememberSaveable(entry.id) { mutableStateOf(entry.entryDescription) }
    var cost by rememberSaveable(entry.id) { mutableStateOf(entry.cost) }
    var parts by rememberSaveable(entry.id) { mutableStateOf(entry.partsUsed) }
    var workshop by rememberSaveable(entry.id) { mutableStateOf(entry.workshop) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (isNewEntry) R.string.vehicles_maintenance_add else R.string.vehicles_maintenance_edit,
                ),
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(FormFieldDefaults.fieldSpacing)) {
                VehicleDatePickerField(
                    value = date,
                    label = stringResource(R.string.vehicles_maintenance_date),
                    onValueChange = { date = it },
                )
                CalculatorDecimalField(
                    value = odometer,
                    onValueChange = { odometer = it },
                    label = stringResource(R.string.vehicles_maintenance_odometer),
                    suffix = "km",
                )
                CalculatorChoiceField(
                    label = stringResource(R.string.vehicles_maintenance_type),
                    options = maintenanceTypes.map {
                        CalculatorDropdownOption(it.name, maintenanceTypeLabel(it))
                    },
                    selectedKey = type,
                    onOptionSelected = { type = it },
                )
                VehicleTextField(
                    value = description,
                    label = stringResource(R.string.vehicles_maintenance_description),
                    singleLine = false,
                    onValueChange = { description = it },
                )
                CalculatorDecimalField(
                    value = cost,
                    onValueChange = { cost = it },
                    label = stringResource(R.string.vehicles_maintenance_cost),
                    suffix = "€",
                )
                VehicleTextField(
                    value = parts,
                    label = stringResource(R.string.vehicles_maintenance_parts),
                    onValueChange = { parts = it },
                )
                VehicleTextField(
                    value = workshop,
                    label = stringResource(R.string.vehicles_maintenance_workshop),
                    onValueChange = { workshop = it },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        entry.copy(
                            date = date,
                            odometerKm = odometer,
                            type = MaintenanceType.valueOf(type),
                            entryDescription = description,
                            cost = cost,
                            partsUsed = parts,
                            workshop = workshop,
                        ),
                    )
                },
            ) {
                Text(stringResource(R.string.vehicles_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
