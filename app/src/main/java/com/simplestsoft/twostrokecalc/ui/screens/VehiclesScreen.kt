package com.simplestsoft.twostrokecalc.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.simplestsoft.twostrokecalc.R
import com.simplestsoft.twostrokecalc.domain.model.Vehicle
import com.simplestsoft.twostrokecalc.ui.components.AppColors
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorIcon
import com.simplestsoft.twostrokecalc.ui.screens.vehicle.VehicleProfileAvatar
import com.simplestsoft.twostrokecalc.ui.theme.ContainerCornerRadius
import com.simplestsoft.twostrokecalc.ui.pro.requireProEdit
import com.simplestsoft.twostrokecalc.ui.util.screenSystemBarPadding
import com.simplestsoft.twostrokecalc.ui.vehicles.AddVehicleDialog
import com.simplestsoft.twostrokecalc.ui.vehicles.VehiclesViewModel

@Composable
fun VehiclesScreen(
    onVehicleClick: (String) -> Unit,
    onVehicleCreated: (String) -> Unit,
    onEditLocked: () -> Unit = {},
    modifier: Modifier = Modifier,
    vehicleAddHighlightModifier: Modifier = Modifier,
    viewModel: VehiclesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val canEdit by viewModel.canEditVehicles.collectAsStateWithLifecycle()
    val cardShape = RoundedCornerShape(ContainerCornerRadius)
    var showAddDialog by rememberSaveable { mutableStateOf(false) }

    AddVehicleDialog(
        visible = showAddDialog,
        catalogBrands = viewModel.catalogBrands(),
        catalogEntryCount = viewModel.catalogEntryCount(),
        getModels = viewModel::catalogModels,
        getYears = viewModel::catalogYears,
        getVariants = viewModel::catalogVariants,
        onDismiss = { showAddDialog = false },
        onConfirm = { basicInfo, catalogEntry ->
            val vehicle = viewModel.createVehicle(basicInfo, catalogEntry)
            showAddDialog = false
            onVehicleCreated(vehicle.id)
        },
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.background()),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .screenSystemBarPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.nav_vehicles),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.textPrimary(),
                )
                Text(
                    text = stringResource(R.string.vehicles_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.textSecondary(),
                )
            }

            if (uiState.vehicles.isEmpty() && !uiState.loading) {
                VehiclesEmptyState(
                    cardShape = cardShape,
                    onAddVehicle = {
                        requireProEdit(canEdit, onEditLocked) { showAddDialog = true }
                    },
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 72.dp),
                ) {
                    items(uiState.vehicles, key = { it.id }) { vehicle ->
                        VehicleListItem(
                            vehicle = vehicle,
                            profileImageFile = viewModel.getProfileImageFile(vehicle),
                            cardShape = cardShape,
                            onClick = { onVehicleClick(vehicle.id) },
                            onDelete = {
                                requireProEdit(canEdit, onEditLocked) {
                                    viewModel.deleteVehicle(vehicle.id)
                                }
                            },
                            canDelete = canEdit,
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { requireProEdit(canEdit, onEditLocked) { showAddDialog = true } },
            modifier = vehicleAddHighlightModifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 20.dp),
            containerColor = AppColors.primaryBlue(),
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(R.string.vehicles_add),
                tint = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VehiclesEmptyState(
    cardShape: RoundedCornerShape,
    onAddVehicle: () -> Unit,
) {
    Card(
        onClick = onAddVehicle,
        modifier = Modifier
            .fillMaxWidth()
            .then(AppColors.cardBorderModifier(cardShape)),
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = AppColors.surface()),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier.size(56.dp),
                contentAlignment = Alignment.Center,
            ) {
                CalculatorIcon(
                    imageVector = Icons.Default.TwoWheeler,
                    size = 48.dp,
                )
            }
            Text(
                text = stringResource(R.string.vehicles_empty_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AppColors.textPrimary(),
            )
            Text(
                text = stringResource(R.string.vehicles_empty_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.textSecondary(),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VehicleListItem(
    vehicle: Vehicle,
    profileImageFile: java.io.File?,
    cardShape: RoundedCornerShape,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    canDelete: Boolean,
) {
    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.vehicles_delete_confirm_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.vehicles_delete_confirm_message_named,
                        vehicle.displayTitle(),
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    },
                ) {
                    Text(
                        text = stringResource(R.string.vehicles_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(AppColors.cardBorderModifier(cardShape)),
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = AppColors.surface()),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onClick)
                    .padding(start = 16.dp, top = 16.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                VehicleProfileAvatar(
                    imageFile = profileImageFile,
                    size = 48.dp,
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = vehicle.displayTitle(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.textPrimary(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val subtitle = vehicle.displaySubtitle()
                    if (subtitle.isNotBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.textSecondary(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (!vehicle.isActive) {
                        Text(
                            text = stringResource(R.string.vehicles_status_inactive),
                            style = MaterialTheme.typography.labelSmall,
                            color = AppColors.textSecondary(),
                        )
                    }
                }
            }
            IconButton(onClick = { if (canDelete) showDeleteConfirm = true else onDelete() }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.vehicles_delete),
                    tint = AppColors.textSecondary(),
                )
            }
        }
    }
}
