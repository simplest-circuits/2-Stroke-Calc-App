package com.simplestsoft.twostrokecalc.ui.screens.vehicle

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.simplestsoft.twostrokecalc.R
import com.simplestsoft.twostrokecalc.domain.vehicles.formatVehicleDate as domainFormatVehicleDate
import com.simplestsoft.twostrokecalc.domain.vehicles.formatVehicleDateTime as domainFormatVehicleDateTime
import com.simplestsoft.twostrokecalc.domain.vehicles.parseVehicleDate as domainParseVehicleDate
import com.simplestsoft.twostrokecalc.domain.vehicles.parseVehicleDateTime as domainParseVehicleDateTime
import com.simplestsoft.twostrokecalc.ui.components.AppOutlinedTextField
import com.simplestsoft.twostrokecalc.ui.pro.LocalVehicleEditingEnabled
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

fun parseVehicleDateTime(raw: String): LocalDateTime? = domainParseVehicleDateTime(raw)

fun formatVehicleDateTime(dateTime: LocalDateTime): String = domainFormatVehicleDateTime(dateTime)

fun parseVehicleDate(raw: String): LocalDate? = domainParseVehicleDate(raw)

fun formatVehicleDate(date: LocalDate): String = domainFormatVehicleDate(date)

private fun vehicleDateDisplayValue(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return ""
    return parseVehicleDate(trimmed)?.let { formatVehicleDate(it) } ?: trimmed
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleDatePickerField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val editingEnabled = LocalVehicleEditingEnabled.current
    val parsedDate = remember(value) { parseVehicleDate(value) }
    val openDatePicker = { if (editingEnabled) showDatePicker = true }

    Box(modifier = modifier.fillMaxWidth()) {
        AppOutlinedTextField(
            value = vehicleDateDisplayValue(value),
            onValueChange = {},
            label = label,
            supportingText = supportingText,
            readOnly = true,
            trailingIcon = {
                IconButton(onClick = openDatePicker, enabled = editingEnabled) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = label,
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(end = 40.dp)
                .clickable(
                    enabled = editingEnabled,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = openDatePicker,
                ),
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = parsedDate
                ?.atStartOfDay(ZoneId.systemDefault())
                ?.toInstant()
                ?.toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val millis = datePickerState.selectedDateMillis ?: return@TextButton
                        val selectedDate = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        onValueChange(formatVehicleDate(selectedDate))
                        showDatePicker = false
                    },
                ) {
                    Text(stringResource(R.string.vehicles_datetime_picker_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleDateTimePickerField(
    value: LocalDateTime,
    onValueChange: (LocalDateTime) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var pendingDate by remember { mutableStateOf<LocalDate?>(null) }
    val editingEnabled = LocalVehicleEditingEnabled.current

    val openDatePicker = { if (editingEnabled) showDatePicker = true }

    Box(modifier = modifier.fillMaxWidth()) {
        AppOutlinedTextField(
            value = formatVehicleDateTime(value),
            onValueChange = {},
            label = label,
            readOnly = true,
            trailingIcon = {
                IconButton(onClick = openDatePicker, enabled = editingEnabled) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = label,
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(end = 40.dp)
                .clickable(
                    enabled = editingEnabled,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = openDatePicker,
                ),
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = value
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val millis = datePickerState.selectedDateMillis ?: return@TextButton
                        pendingDate = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        showDatePicker = false
                        showTimePicker = true
                    },
                ) {
                    Text(stringResource(R.string.vehicles_datetime_picker_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val date = pendingDate ?: value.toLocalDate()
        val timePickerState = rememberTimePickerState(
            initialHour = value.hour,
            initialMinute = value.minute,
        )
        AlertDialog(
            onDismissRequest = {
                showTimePicker = false
                pendingDate = null
            },
            title = { Text(stringResource(R.string.vehicles_datetime_picker_time_title)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onValueChange(
                            LocalDateTime.of(
                                date,
                                LocalTime.of(timePickerState.hour, timePickerState.minute),
                            ),
                        )
                        showTimePicker = false
                        pendingDate = null
                    },
                ) {
                    Text(stringResource(R.string.vehicles_datetime_picker_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showTimePicker = false
                    pendingDate = null
                }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            text = {
                TimePicker(state = timePickerState)
            },
        )
    }
}
