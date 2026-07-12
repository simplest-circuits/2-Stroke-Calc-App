package com.simplestsoft.twostrokecalc.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Euro
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.simplestsoft.twostrokecalc.R
import com.simplestsoft.twostrokecalc.domain.model.MaintenanceEntry
import com.simplestsoft.twostrokecalc.domain.model.Vehicle
import com.simplestsoft.twostrokecalc.domain.vehicles.applicableMaintenanceTypes
import com.simplestsoft.twostrokecalc.domain.vehicles.vehicleCapabilities
import com.simplestsoft.twostrokecalc.ui.components.AppColors
import com.simplestsoft.twostrokecalc.ui.components.ProReadOnlyBanner
import com.simplestsoft.twostrokecalc.ui.pro.LocalCalculatorEditingEnabled
import com.simplestsoft.twostrokecalc.ui.pro.LocalVehicleEditingEnabled
import com.simplestsoft.twostrokecalc.ui.pro.requireProEdit
import com.simplestsoft.twostrokecalc.ui.screens.vehicle.MaintenanceEntryDialog
import com.simplestsoft.twostrokecalc.ui.screens.vehicle.VehicleBasicDataTabContent
import com.simplestsoft.twostrokecalc.ui.screens.vehicle.VehicleChassisTabContent
import com.simplestsoft.twostrokecalc.ui.screens.vehicle.VehicleDetailTab
import com.simplestsoft.twostrokecalc.ui.screens.vehicle.VehicleDocumentsSection
import com.simplestsoft.twostrokecalc.ui.screens.vehicle.VehicleDrivetrainTabContent
import com.simplestsoft.twostrokecalc.ui.screens.vehicle.VehicleElectricalTabContent
import com.simplestsoft.twostrokecalc.ui.screens.vehicle.VehicleEngineTabContent
import com.simplestsoft.twostrokecalc.ui.screens.vehicle.VehicleMaintenanceTabContent
import com.simplestsoft.twostrokecalc.ui.screens.vehicle.VehicleOverviewSummaryContent
import com.simplestsoft.twostrokecalc.ui.screens.vehicle.VehicleTuningTabContent
import com.simplestsoft.twostrokecalc.ui.theme.ContainerCornerRadius
import com.simplestsoft.twostrokecalc.ui.util.keyboardAwareScroll
import com.simplestsoft.twostrokecalc.ui.vehicles.VehiclesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleDetailScreen(
    vehicleId: String,
    onBack: () -> Unit,
    onOpenFuelLog: () -> Unit = {},
    onOpenCostOverview: () -> Unit = {},
    onEditLocked: () -> Unit = {},
    viewModel: VehiclesViewModel = hiltViewModel(),
) {
    val vehicles by viewModel.vehicles.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val canEdit by viewModel.canEditVehicles.collectAsStateWithLifecycle()
    val resolvedVehicleId = remember(vehicleId) { Uri.decode(vehicleId) }
    val persisted = vehicles.firstOrNull { it.id == resolvedVehicleId }?.withLegacyMigration()

    var draft by remember(resolvedVehicleId) { mutableStateOf<Vehicle?>(null) }
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var showMaintenanceDialog by rememberSaveable { mutableStateOf(false) }
    var editingMaintenance by remember { mutableStateOf<MaintenanceEntry?>(null) }
    var hasUnsavedChanges by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(persisted) {
        if (draft == null && persisted != null) {
            draft = persisted
        }
    }

    LaunchedEffect(persisted?.attachments, persisted?.profileImageFileName) {
        val current = persisted ?: return@LaunchedEffect
        if (draft != null) {
            draft = draft?.copy(
                attachments = current.attachments,
                profileImageFileName = current.profileImageFileName,
            )
        }
    }

    val vehicle = draft ?: persisted

    fun updateVehicle(transform: (Vehicle) -> Vehicle) {
        if (!canEdit) return
        vehicle?.let {
            draft = transform(it)
            hasUnsavedChanges = true
        }
    }

    fun saveAndBack() {
        if (canEdit) {
            vehicle?.let { viewModel.saveVehicle(it.withLegacyMigration()) }
        }
        onBack()
    }

    BackHandler {
        if (canEdit && hasUnsavedChanges) saveAndBack() else onBack()
    }

    if (vehicle == null) {
        LaunchedEffect(resolvedVehicleId, uiState.loading, vehicles) {
            if (!uiState.loading && vehicles.isNotEmpty() && vehicles.none { it.id == resolvedVehicleId }) {
                onBack()
            }
        }
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            if (uiState.loading) {
                CircularProgressIndicator()
            }
        }
        return
    }

    val capabilities = vehicleCapabilities(vehicle)

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.vehicles_delete_confirm_title)) },
            text = { Text(stringResource(R.string.vehicles_delete_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteVehicle(resolvedVehicleId)
                        showDeleteDialog = false
                        onBack()
                    },
                ) {
                    Text(stringResource(R.string.vehicles_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    if (showMaintenanceDialog) {
        MaintenanceEntryDialog(
            entry = editingMaintenance ?: MaintenanceEntry(),
            isNewEntry = editingMaintenance == null,
            maintenanceTypes = applicableMaintenanceTypes(vehicle),
            onDismiss = {
                showMaintenanceDialog = false
                editingMaintenance = null
            },
            onConfirm = { entry ->
                if (editingMaintenance != null) {
                    viewModel.updateMaintenanceEntry(resolvedVehicleId, entry)
                    updateVehicle {
                        it.copy(maintenanceLog = it.maintenanceLog.map { e -> if (e.id == entry.id) entry else e })
                    }
                } else {
                    viewModel.addMaintenanceEntry(resolvedVehicleId, entry)
                    updateVehicle { it.copy(maintenanceLog = it.maintenanceLog + entry) }
                }
                showMaintenanceDialog = false
                editingMaintenance = null
            },
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = AppColors.background(),
        topBar = {
            TopAppBar(
                title = {
                    Text(text = vehicle.displayTitle(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                navigationIcon = {
                    IconButton(onClick = ::saveAndBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.vehicles_back),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenCostOverview) {
                        Icon(
                            imageVector = Icons.Default.Euro,
                            contentDescription = stringResource(R.string.vehicles_quick_cost_overview),
                        )
                    }
                    IconButton(
                        onClick = {
                            requireProEdit(canEdit, onEditLocked) {
                                editingMaintenance = null
                                showMaintenanceDialog = true
                            }
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = stringResource(R.string.vehicles_quick_maintenance),
                        )
                    }
                    IconButton(onClick = onOpenFuelLog) {
                        Icon(
                            imageVector = Icons.Default.LocalGasStation,
                            contentDescription = stringResource(R.string.vehicles_quick_fuel_log),
                        )
                    }
                    IconButton(onClick = { requireProEdit(canEdit, onEditLocked) { showDeleteDialog = true } }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.vehicles_delete),
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        CompositionLocalProvider(
            LocalVehicleEditingEnabled provides canEdit,
            LocalCalculatorEditingEnabled provides canEdit,
        ) {
        val scrollState = rememberScrollState()
        val tabs = VehicleDetailTab.entries
        val selectedTab = tabs.getOrElse(selectedTabIndex) { VehicleDetailTab.OVERVIEW }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            if (!canEdit) {
                ProReadOnlyBanner(
                    onBuyPro = onEditLocked,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(ContainerCornerRadius),
            ) {
                ScrollableTabRow(
                    selectedTabIndex = selectedTabIndex,
                    edgePadding = 8.dp,
                    divider = {},
                ) {
                    tabs.forEachIndexed { index, tab ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            modifier = Modifier.height(44.dp),
                            text = {
                                Text(
                                    text = stringResource(tab.titleRes),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .keyboardAwareScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                when (selectedTab) {
                    VehicleDetailTab.OVERVIEW -> VehicleOverviewSummaryContent(
                        vehicle = vehicle,
                        profileImageFile = viewModel.getProfileImageFile(vehicle),
                        onEditTab = { tab -> selectedTabIndex = tab.ordinal },
                    )
                    VehicleDetailTab.BASIC -> VehicleBasicDataTabContent(
                        vehicle = vehicle,
                        profileImageFile = viewModel.getProfileImageFile(vehicle),
                        onUpdate = { updateVehicle { _ -> it } },
                        onProfileImageSelected = { uri ->
                            requireProEdit(canEdit, onEditLocked) {
                                viewModel.setProfileImage(resolvedVehicleId, uri) { fileName ->
                                    fileName?.let {
                                        updateVehicle { v -> v.copy(profileImageFileName = it) }
                                    }
                                }
                            }
                        },
                        onProfileImageRemove = {
                            requireProEdit(canEdit, onEditLocked) {
                                viewModel.clearProfileImage(resolvedVehicleId) {
                                    updateVehicle { it.copy(profileImageFileName = null) }
                                }
                            }
                        },
                    )
                    VehicleDetailTab.ENGINE -> VehicleEngineTabContent(
                        engine = vehicle.engine,
                        onUpdate = { updateVehicle { v -> v.copy(engine = it) } },
                    )
                    VehicleDetailTab.TUNING -> VehicleTuningTabContent(
                        specs = vehicle.carbIgnition,
                        capabilities = capabilities,
                        onUpdate = { updateVehicle { v -> v.copy(carbIgnition = it) } },
                    )
                    VehicleDetailTab.DRIVETRAIN -> VehicleDrivetrainTabContent(
                        specs = vehicle.drivetrain,
                        capabilities = capabilities,
                        onUpdate = { updateVehicle { v -> v.copy(drivetrain = it) } },
                    )
                    VehicleDetailTab.CHASSIS -> VehicleChassisTabContent(
                        specs = vehicle.chassis,
                        onUpdate = { updateVehicle { v -> v.copy(chassis = it) } },
                    )
                    VehicleDetailTab.ELECTRICAL -> VehicleElectricalTabContent(
                        specs = vehicle.electrical,
                        onUpdate = { updateVehicle { v -> v.copy(electrical = it) } },
                    )
                    VehicleDetailTab.MAINTENANCE -> VehicleMaintenanceTabContent(
                        schedule = vehicle.serviceSchedule,
                        entries = vehicle.maintenanceLog,
                        capabilities = capabilities,
                        onScheduleUpdate = { updateVehicle { v -> v.copy(serviceSchedule = it) } },
                        onEditEntry = { entry ->
                            requireProEdit(canEdit, onEditLocked) {
                                editingMaintenance = entry
                                showMaintenanceDialog = true
                            }
                        },
                        onDeleteEntry = { entryId ->
                            requireProEdit(canEdit, onEditLocked) {
                                viewModel.deleteMaintenanceEntry(resolvedVehicleId, entryId)
                                updateVehicle {
                                    it.copy(maintenanceLog = it.maintenanceLog.filterNot { e -> e.id == entryId })
                                }
                            }
                        },
                    )
                    VehicleDetailTab.DOCUMENTS -> VehicleDocumentsSection(
                        documents = vehicle.documents,
                        schedule = vehicle.serviceSchedule,
                        notes = vehicle.notes,
                        attachments = vehicle.attachments,
                        onDocumentsUpdate = { updateVehicle { v -> v.copy(documents = it) } },
                        onScheduleUpdate = { updateVehicle { v -> v.copy(serviceSchedule = it) } },
                        onNotesUpdate = { updateVehicle { v -> v.copy(notes = it) } },
                        onAddAttachments = { uris ->
                            requireProEdit(canEdit, onEditLocked) {
                                uris.forEach { uri ->
                                    viewModel.addAttachment(resolvedVehicleId, uri) { attachment ->
                                        attachment?.let {
                                            updateVehicle { v -> v.copy(attachments = v.attachments + it) }
                                        }
                                    }
                                }
                            }
                        },
                        onRemoveAttachment = { attachmentId ->
                            requireProEdit(canEdit, onEditLocked) {
                                viewModel.removeAttachment(resolvedVehicleId, attachmentId)
                                updateVehicle {
                                    it.copy(attachments = it.attachments.filterNot { a -> a.id == attachmentId })
                                }
                            }
                        },
                        onUpdateAttachment = { attachment ->
                            requireProEdit(canEdit, onEditLocked) {
                                viewModel.updateAttachment(resolvedVehicleId, attachment)
                                updateVehicle {
                                    it.copy(
                                        attachments = it.attachments.map { a ->
                                            if (a.id == attachment.id) attachment else a
                                        },
                                    )
                                }
                            }
                        },
                        onOpenAttachment = { viewModel.openAttachment(resolvedVehicleId, it) },
                    )
                }

                if (canEdit && selectedTab != VehicleDetailTab.OVERVIEW) {
                    Button(onClick = ::saveAndBack, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.vehicles_save))
                    }
                }
            }
        }
        }
    }
}
