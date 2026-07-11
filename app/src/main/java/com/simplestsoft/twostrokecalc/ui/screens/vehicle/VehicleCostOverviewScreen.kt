package com.simplestsoft.twostrokecalc.ui.screens.vehicle

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.simplestsoft.twostrokecalc.R
import com.simplestsoft.twostrokecalc.domain.model.MaintenanceEntry
import com.simplestsoft.twostrokecalc.domain.model.MaintenanceType
import com.simplestsoft.twostrokecalc.domain.vehicles.COST_GROUP_UNKNOWN_DATE
import com.simplestsoft.twostrokecalc.domain.vehicles.COST_GROUP_UNKNOWN_WORKSHOP
import com.simplestsoft.twostrokecalc.domain.vehicles.CostOverviewGroupBy
import com.simplestsoft.twostrokecalc.domain.vehicles.MaintenanceCostGroup
import com.simplestsoft.twostrokecalc.domain.vehicles.MaintenanceCostItem
import com.simplestsoft.twostrokecalc.domain.vehicles.VehicleCostCalculator
import com.simplestsoft.twostrokecalc.domain.vehicles.VehicleCostSummary
import com.simplestsoft.twostrokecalc.domain.vehicles.applicableMaintenanceTypes
import com.simplestsoft.twostrokecalc.ui.components.AppColors
import com.simplestsoft.twostrokecalc.ui.components.ProReadOnlyBanner
import com.simplestsoft.twostrokecalc.ui.pro.LocalCalculatorEditingEnabled
import com.simplestsoft.twostrokecalc.ui.pro.requireProEdit
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorChoiceField
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorDropdownOption
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorSection
import com.simplestsoft.twostrokecalc.ui.theme.ContainerCornerRadius
import com.simplestsoft.twostrokecalc.ui.util.keyboardAwareScroll
import com.simplestsoft.twostrokecalc.ui.vehicles.VehiclesViewModel
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class CostOverviewLevel {
    Overview,
    GroupEntries,
    EntryDetail,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleCostOverviewScreen(
    vehicleId: String,
    onBack: () -> Unit,
    onEditLocked: () -> Unit = {},
    viewModel: VehiclesViewModel = hiltViewModel(),
) {
    val vehicles by viewModel.vehicles.collectAsStateWithLifecycle()
    val canEdit by viewModel.canEditVehicles.collectAsStateWithLifecycle()
    val vehicle = vehicles.firstOrNull { it.id == vehicleId }?.withLegacyMigration()

    LaunchedEffect(vehicleId, vehicles) {
        if (vehicles.isNotEmpty() && vehicle == null) {
            onBack()
        }
    }

    if (vehicle == null) return

    val summary = remember(vehicle.purchasePrice, vehicle.maintenanceLog) {
        VehicleCostCalculator.summarize(vehicle)
    }

    var groupByName by rememberSaveable { mutableStateOf(CostOverviewGroupBy.MAINTENANCE_TYPE.name) }
    var selectedGroupKey by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedEntryId by rememberSaveable { mutableStateOf<String?>(null) }
    var showEditDialog by rememberSaveable { mutableStateOf(false) }

    val groupBy = CostOverviewGroupBy.entries.firstOrNull { it.name == groupByName }
        ?: CostOverviewGroupBy.MAINTENANCE_TYPE

    val costGroups = remember(summary.maintenanceEntriesWithCost, groupBy) {
        VehicleCostCalculator.groupMaintenanceCosts(summary.maintenanceEntriesWithCost, groupBy)
    }

    val selectedEntry = selectedEntryId?.let { entryId ->
        vehicle.maintenanceLog.firstOrNull { it.id == entryId }
    }
    val selectedEntryItem = selectedEntry?.let { entry ->
        summary.maintenanceEntriesWithCost.firstOrNull { it.entry.id == entry.id }
    }
    val selectedGroup = selectedGroupKey?.let { key ->
        costGroups.firstOrNull { it.key == key }
    }

    val level = when {
        selectedEntryItem != null -> CostOverviewLevel.EntryDetail
        selectedGroupKey != null -> CostOverviewLevel.GroupEntries
        else -> CostOverviewLevel.Overview
    }

    LaunchedEffect(summary.maintenanceEntriesWithCost, selectedEntryId) {
        if (selectedEntryId != null && selectedEntryItem == null) {
            selectedEntryId = null
        }
    }

    LaunchedEffect(costGroups, selectedGroupKey, level) {
        if (level == CostOverviewLevel.GroupEntries && selectedGroupKey != null && selectedGroup == null) {
            selectedGroupKey = null
            selectedEntryId = null
        }
    }

    val groupEntries = remember(summary.maintenanceEntriesWithCost, groupBy, selectedGroupKey) {
        val key = selectedGroupKey ?: return@remember emptyList()
        VehicleCostCalculator.itemsInGroup(summary.maintenanceEntriesWithCost, groupBy, key)
    }

    val topBarTitle = when (level) {
        CostOverviewLevel.Overview -> stringResource(R.string.vehicles_cost_overview_title)
        CostOverviewLevel.GroupEntries -> selectedGroupKey?.let { costGroupLabel(groupBy, it) }
            ?: stringResource(R.string.vehicles_cost_overview_title)
        CostOverviewLevel.EntryDetail -> selectedEntryItem?.let { maintenanceCostEntryTitle(it.entry) }
            ?: stringResource(R.string.vehicles_cost_overview_title)
    }

    fun navigateBack() {
        when (level) {
            CostOverviewLevel.EntryDetail -> selectedEntryId = null
            CostOverviewLevel.GroupEntries -> {
                selectedGroupKey = null
                selectedEntryId = null
            }
            CostOverviewLevel.Overview -> onBack()
        }
    }

    if (showEditDialog && selectedEntry != null) {
        MaintenanceEntryDialog(
            entry = selectedEntry,
            isNewEntry = false,
            maintenanceTypes = applicableMaintenanceTypes(vehicle),
            onDismiss = { showEditDialog = false },
            onConfirm = { entry ->
                viewModel.updateMaintenanceEntry(vehicleId, entry)
                showEditDialog = false
            },
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = AppColors.background(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = topBarTitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = ::navigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.vehicles_back),
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        CompositionLocalProvider(LocalCalculatorEditingEnabled provides canEdit) {
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .keyboardAwareScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!canEdit) {
                ProReadOnlyBanner(onBuyPro = onEditLocked)
            }
            if (level == CostOverviewLevel.Overview) {
                Text(
                    text = vehicle.displayTitle(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.textPrimary(),
                )
            }

            when (level) {
                CostOverviewLevel.Overview -> {
                    if (!summary.hasAnyCostData) {
                        Text(
                            text = stringResource(R.string.vehicles_cost_overview_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppColors.textSecondary(),
                        )
                    } else {
                        VehicleCostSummaryCard(summary = summary)

                        if (summary.maintenanceEntriesWithCost.isNotEmpty()) {
                            CalculatorChoiceField(
                                label = stringResource(R.string.vehicles_cost_overview_group_by),
                                options = CostOverviewGroupBy.entries.map { option ->
                                    CalculatorDropdownOption(
                                        key = option.name,
                                        label = costGroupByLabel(option),
                                    )
                                },
                                selectedKey = groupBy.name,
                                onOptionSelected = { key ->
                                    if (key != groupBy.name) {
                                        groupByName = key
                                        selectedGroupKey = null
                                        selectedEntryId = null
                                    }
                                },
                            )

                            if (costGroups.isNotEmpty()) {
                                CalculatorSection(title = stringResource(R.string.vehicles_cost_overview_breakdown)) {
                                    costGroups.forEach { group ->
                                        CostGroupRow(
                                            label = costGroupLabel(groupBy, group.key),
                                            group = group,
                                            onClick = { selectedGroupKey = group.key },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                CostOverviewLevel.GroupEntries -> {
                    selectedGroup?.let { group ->
                        GroupEntriesSummaryCard(group = group)
                    }
                    CalculatorSection(title = stringResource(R.string.vehicles_cost_overview_entries)) {
                        groupEntries.forEach { item ->
                            CostEntryListRow(
                                item = item,
                                onClick = { selectedEntryId = item.entry.id },
                            )
                        }
                    }
                }

                CostOverviewLevel.EntryDetail -> {
                    selectedEntryItem?.let { item ->
                        MaintenanceCostEntryDetail(
                            item = item,
                            onEdit = {
                                requireProEdit(canEdit, onEditLocked) { showEditDialog = true }
                            },
                            onDelete = {
                                requireProEdit(canEdit, onEditLocked) {
                                    viewModel.deleteMaintenanceEntry(vehicleId, item.entry.id)
                                    selectedEntryId = null
                                }
                            },
                        )
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun costGroupByLabel(groupBy: CostOverviewGroupBy): String = when (groupBy) {
    CostOverviewGroupBy.MAINTENANCE_TYPE -> stringResource(R.string.vehicles_cost_overview_group_type)
    CostOverviewGroupBy.YEAR -> stringResource(R.string.vehicles_cost_overview_group_year)
    CostOverviewGroupBy.MONTH -> stringResource(R.string.vehicles_cost_overview_group_month)
    CostOverviewGroupBy.WORKSHOP -> stringResource(R.string.vehicles_cost_overview_group_workshop)
}

@Composable
private fun costGroupLabel(groupBy: CostOverviewGroupBy, key: String): String = when (groupBy) {
    CostOverviewGroupBy.MAINTENANCE_TYPE -> {
        MaintenanceType.entries.firstOrNull { it.name == key }?.let { maintenanceTypeLabel(it) } ?: key
    }
    CostOverviewGroupBy.YEAR -> {
        if (key == COST_GROUP_UNKNOWN_DATE) {
            stringResource(R.string.vehicles_cost_overview_no_date)
        } else {
            key
        }
    }
    CostOverviewGroupBy.MONTH -> {
        if (key == COST_GROUP_UNKNOWN_DATE) {
            stringResource(R.string.vehicles_cost_overview_no_date)
        } else {
            val parts = key.split("-")
            val year = parts.getOrNull(0)?.toIntOrNull()
            val month = parts.getOrNull(1)?.toIntOrNull()
            if (year != null && month != null) {
                YearMonth.of(year, month)
                    .format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))
            } else {
                key
            }
        }
    }
    CostOverviewGroupBy.WORKSHOP -> {
        if (key == COST_GROUP_UNKNOWN_WORKSHOP) {
            stringResource(R.string.vehicles_cost_overview_no_workshop)
        } else {
            key
        }
    }
}

@Composable
private fun VehicleCostSummaryCard(summary: VehicleCostSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(ContainerCornerRadius),
        colors = CardDefaults.cardColors(containerColor = AppColors.surface()),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.vehicles_cost_overview_total),
                style = MaterialTheme.typography.labelLarge,
                color = AppColors.textSecondary(),
            )
            Text(
                text = summary.totalCost?.let(::formatEuro)
                    ?: stringResource(R.string.vehicles_cost_overview_unavailable),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = AppColors.textPrimary(),
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = AppColors.borderSubtle(),
            )

            CostSummaryRow(
                label = stringResource(R.string.vehicles_cost_overview_purchase),
                value = summary.purchaseCost?.let(::formatEuro)
                    ?: stringResource(R.string.vehicles_cost_overview_unavailable),
            )
            CostSummaryRow(
                label = stringResource(R.string.vehicles_cost_overview_maintenance),
                value = if (summary.maintenanceEntriesWithCost.isEmpty()) {
                    stringResource(R.string.vehicles_cost_overview_unavailable)
                } else {
                    formatEuro(summary.maintenanceTotal)
                },
            )
        }
    }
}

@Composable
private fun CostSummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.textSecondary(),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.textPrimary(),
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun GroupEntriesSummaryCard(group: MaintenanceCostGroup) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(ContainerCornerRadius),
        colors = CardDefaults.cardColors(containerColor = AppColors.surface()),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(
                        R.string.vehicles_cost_overview_entry_count,
                        group.entryCount,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.textSecondary(),
                )
            }
            Text(
                text = formatEuro(group.total),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.textPrimary(),
            )
        }
    }
}

@Composable
private fun CostGroupRow(
    label: String,
    group: MaintenanceCostGroup,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.textPrimary(),
            )
            Text(
                text = stringResource(
                    R.string.vehicles_cost_overview_entry_count,
                    group.entryCount,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.textSecondary(),
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = formatEuro(group.total),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.textPrimary(),
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = AppColors.textSecondary(),
            )
        }
    }
}

@Composable
private fun CostEntryListRow(
    item: MaintenanceCostItem,
    onClick: () -> Unit,
) {
    val entry = item.entry
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = maintenanceCostEntryTitle(entry),
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.textPrimary(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (entry.description.isNotBlank()) {
                Text(
                    text = entry.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.textSecondary(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = formatEuro(item.cost),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.textPrimary(),
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = AppColors.textSecondary(),
            )
        }
    }
}

@Composable
private fun MaintenanceCostEntryDetail(
    item: MaintenanceCostItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val entry = item.entry
    CalculatorSection(title = stringResource(R.string.vehicles_cost_overview_entry_detail)) {
        if (entry.date.isNotBlank()) {
            CostInfoRow(stringResource(R.string.vehicles_maintenance_date), entry.date)
        }
        if (entry.odometerKm.isNotBlank()) {
            CostInfoRow(
                stringResource(R.string.vehicles_maintenance_odometer),
                "${entry.odometerKm} km",
            )
        }
        CostInfoRow(
            stringResource(R.string.vehicles_maintenance_type),
            maintenanceTypeLabel(entry.type),
        )
        if (entry.description.isNotBlank()) {
            CostInfoRow(stringResource(R.string.vehicles_maintenance_description), entry.description)
        }
        CostInfoRow(stringResource(R.string.vehicles_maintenance_cost), formatEuro(item.cost))
        if (entry.partsUsed.isNotBlank()) {
            CostInfoRow(stringResource(R.string.vehicles_maintenance_parts), entry.partsUsed)
        }
        if (entry.workshop.isNotBlank()) {
            CostInfoRow(stringResource(R.string.vehicles_maintenance_workshop), entry.workshop)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onEdit) {
                Text(stringResource(R.string.vehicles_maintenance_edit))
            }
            TextButton(onClick = onDelete) {
                Text(stringResource(R.string.vehicles_delete), color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun CostInfoRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(bottom = 4.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = AppColors.textSecondary())
        Text(value, style = MaterialTheme.typography.bodyMedium, color = AppColors.textPrimary())
    }
}

@Composable
private fun maintenanceCostEntryTitle(entry: MaintenanceEntry): String {
    val typeLabel = maintenanceTypeLabel(entry.type)
    return if (entry.date.isNotBlank()) "$typeLabel · ${entry.date}" else typeLabel
}

private fun formatEuro(amount: Double): String =
    String.format(Locale.GERMAN, "%.2f €", amount)
