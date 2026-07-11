package com.simplestsoft.twostrokecalc.ui.screens.vehicle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simplestsoft.twostrokecalc.R
import com.simplestsoft.twostrokecalc.domain.model.EngineCycleType
import com.simplestsoft.twostrokecalc.domain.model.MaintenanceEntry
import com.simplestsoft.twostrokecalc.domain.model.Vehicle
import com.simplestsoft.twostrokecalc.domain.vehicles.resolvedCylinderCount
import com.simplestsoft.twostrokecalc.domain.vehicles.resolvedValveClearances
import com.simplestsoft.twostrokecalc.domain.vehicles.vehicleCapabilities
import com.simplestsoft.twostrokecalc.ui.components.AppColors
import com.simplestsoft.twostrokecalc.ui.theme.ContainerCornerRadius
import java.io.File

private data class InfoItem(val label: String, val value: String)

private data class OverviewSection(
    val title: String,
    val editTab: VehicleDetailTab,
    val items: List<InfoItem> = emptyList(),
)

@Composable
fun VehicleOverviewSummaryContent(
    vehicle: Vehicle,
    profileImageFile: File?,
    onEditTab: (VehicleDetailTab) -> Unit,
) {
    val sections = buildOverviewSections(vehicle)
    val maintenanceEntries = vehicle.maintenanceLog.sortedByDescending { it.date }.take(2)
    val attachmentCount = vehicle.attachments.size

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        VehicleOverviewHeader(vehicle = vehicle, profileImageFile = profileImageFile)
        VehicleOverviewQuickStats(vehicle = vehicle)

        if (sections.isEmpty() && maintenanceEntries.isEmpty() && attachmentCount == 0 && vehicle.notes.isBlank()) {
            VehicleOverviewEmptyHint(onEditTab = onEditTab)
        } else {
            VehicleOverviewCompactCard {
                sections.forEachIndexed { index, section ->
                    if (index > 0) {
                        OverviewSectionDivider()
                    }
                    CompactOverviewSectionBlock(
                        title = section.title,
                        onEdit = { onEditTab(section.editTab) },
                    ) {
                        section.items.forEach { item ->
                            CompactInfoRow(label = item.label, value = item.value)
                        }
                    }
                }

                if (maintenanceEntries.isNotEmpty() || maintenanceScheduleItems(vehicle).isNotEmpty()) {
                    if (sections.isNotEmpty()) OverviewSectionDivider()
                    CompactOverviewSectionBlock(
                        title = stringResource(R.string.vehicles_tab_maintenance),
                        onEdit = { onEditTab(VehicleDetailTab.MAINTENANCE) },
                    ) {
                        maintenanceScheduleItems(vehicle).forEach { item ->
                            CompactInfoRow(label = item.label, value = item.value)
                        }
                        maintenanceEntries.forEach { entry ->
                            CompactMaintenanceRow(entry)
                        }
                        if (vehicle.maintenanceLog.size > maintenanceEntries.size) {
                            Text(
                                text = stringResource(
                                    R.string.vehicles_overview_maintenance_more,
                                    vehicle.maintenanceLog.size - maintenanceEntries.size,
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = AppColors.textSecondary(),
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                    }
                }

                val docItems = documentItems(vehicle)
                if (docItems.isNotEmpty() || attachmentCount > 0) {
                    if (sections.isNotEmpty() || maintenanceEntries.isNotEmpty() ||
                        maintenanceScheduleItems(vehicle).isNotEmpty()
                    ) {
                        OverviewSectionDivider()
                    }
                    CompactOverviewSectionBlock(
                        title = stringResource(R.string.vehicles_tab_documents),
                        onEdit = { onEditTab(VehicleDetailTab.DOCUMENTS) },
                    ) {
                        docItems.forEach { item ->
                            CompactInfoRow(label = item.label, value = item.value)
                        }
                        if (attachmentCount > 0) {
                            CompactInfoRow(
                                label = stringResource(R.string.vehicles_subtab_attachments),
                                value = stringResource(R.string.vehicles_overview_attachments_count, attachmentCount),
                            )
                        }
                    }
                }

                if (vehicle.notes.isNotBlank()) {
                    OverviewSectionDivider()
                    CompactOverviewSectionBlock(
                        title = stringResource(R.string.vehicles_section_notes),
                        onEdit = { onEditTab(VehicleDetailTab.DOCUMENTS) },
                    ) {
                        Text(
                            text = vehicle.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.textPrimary(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VehicleOverviewHeader(
    vehicle: Vehicle,
    profileImageFile: File?,
) {
    val shape = RoundedCornerShape(ContainerCornerRadius)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(AppColors.cardBorderModifier(shape)),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = AppColors.surface()),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            VehicleProfileAvatar(
                imageFile = profileImageFile,
                size = 56.dp,
                contentDescription = vehicle.displayTitle(),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = vehicle.displayTitle(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.textPrimary(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val meta = buildList {
                    add(vehicleTypeLabel(vehicle.vehicleType))
                    if (vehicle.year.isNotBlank()) add(vehicle.year)
                    if (!vehicle.isActive) add(stringResource(R.string.vehicles_status_inactive))
                }.joinToString(" · ")
                Text(
                    text = meta,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.textSecondary(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (vehicle.licensePlate.isNotBlank()) {
                    Text(
                        text = vehicle.licensePlate,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.primaryBlue(),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VehicleOverviewQuickStats(vehicle: Vehicle) {
    val stats = buildList {
        if (vehicle.currentOdometerKm.isNotBlank()) {
            add(stringResource(R.string.vehicles_field_odometer) to "${vehicle.currentOdometerKm} km")
        }
        if (vehicle.currentOperatingHours.isNotBlank()) {
            add(stringResource(R.string.vehicles_field_operating_hours) to "${vehicle.currentOperatingHours} h")
        }
        vehicle.serviceSchedule.tuvInspectionDate.takeIf { it.isNotBlank() }?.let {
            add(stringResource(R.string.vehicles_field_tuv_date) to it)
        }
        vehicle.serviceSchedule.insuranceExpiryDate.takeIf { it.isNotBlank() }?.let {
            add(stringResource(R.string.vehicles_field_insurance_date) to it)
        }
        vehicle.serviceSchedule.nextServiceDueDate.takeIf { it.isNotBlank() }?.let {
            add(stringResource(R.string.vehicles_field_next_service_date) to it)
        }
        vehicle.serviceSchedule.nextServiceDueKm.takeIf { it.isNotBlank() }?.let {
            add(stringResource(R.string.vehicles_field_next_service_km) to "$it km")
        }
    }
    if (stats.isEmpty()) return

    val shape = RoundedCornerShape(ContainerCornerRadius)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(AppColors.cardBorderModifier(shape)),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = AppColors.surface()),
    ) {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            stats.forEach { (label, value) ->
                QuickStatCell(label = label, value = value)
            }
        }
    }
}

@Composable
private fun QuickStatCell(label: String, value: String) {
    Column(
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.textSecondary(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.textPrimary(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun VehicleOverviewCompactCard(content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(ContainerCornerRadius)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(AppColors.cardBorderModifier(shape)),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = AppColors.surface()),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun CompactOverviewSectionBlock(
    title: String,
    onEdit: () -> Unit,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = AppColors.primaryBlue(),
        )
        TextButton(
            onClick = onEdit,
            modifier = Modifier.padding(0.dp),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
        ) {
            Text(
                text = stringResource(R.string.vehicles_overview_edit),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        content()
    }
}

@Composable
private fun OverviewSectionDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 2.dp),
        color = AppColors.borderSubtle(),
    )
}

@Composable
private fun CompactInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(0.42f),
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.textSecondary(),
            lineHeight = 16.sp,
        )
        Text(
            text = value,
            modifier = Modifier.weight(0.58f),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = AppColors.textPrimary(),
            textAlign = TextAlign.End,
            lineHeight = 16.sp,
        )
    }
}

@Composable
private fun CompactMaintenanceRow(entry: MaintenanceEntry) {
    val summary = buildList {
        add(maintenanceTypeLabel(entry.type))
        if (entry.date.isNotBlank()) add(entry.date)
        if (entry.odometerKm.isNotBlank()) add("${entry.odometerKm} km")
    }.joinToString(" · ")
    CompactInfoRow(
        label = stringResource(R.string.vehicles_section_maintenance),
        value = summary,
    )
}

@Composable
private fun VehicleOverviewEmptyHint(onEditTab: (VehicleDetailTab) -> Unit) {
    val shape = RoundedCornerShape(ContainerCornerRadius)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(AppColors.cardBorderModifier(shape)),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = AppColors.surface()),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.vehicles_overview_empty_section),
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.textSecondary(),
            )
            TextButton(onClick = { onEditTab(VehicleDetailTab.BASIC) }) {
                Text(stringResource(R.string.vehicles_overview_edit))
            }
        }
    }
}

@Composable
private fun buildOverviewSections(vehicle: Vehicle): List<OverviewSection> {
    val basic = basicInfoItems(vehicle) + identificationItems(vehicle)
    return listOfNotNull(
        OverviewSection(
            title = stringResource(R.string.vehicles_section_basic),
            editTab = VehicleDetailTab.BASIC,
            items = basic,
        ).takeIf { it.items.isNotEmpty() },
        OverviewSection(
            title = stringResource(R.string.vehicles_tab_engine),
            editTab = VehicleDetailTab.ENGINE,
            items = engineItems(vehicle),
        ).takeIf { it.items.isNotEmpty() },
        OverviewSection(
            title = stringResource(R.string.vehicles_tab_tuning),
            editTab = VehicleDetailTab.TUNING,
            items = tuningItems(vehicle),
        ).takeIf { it.items.isNotEmpty() },
        OverviewSection(
            title = stringResource(R.string.vehicles_tab_drivetrain),
            editTab = VehicleDetailTab.DRIVETRAIN,
            items = drivetrainItems(vehicle),
        ).takeIf { it.items.isNotEmpty() },
        OverviewSection(
            title = stringResource(R.string.vehicles_tab_chassis),
            editTab = VehicleDetailTab.CHASSIS,
            items = chassisItems(vehicle),
        ).takeIf { it.items.isNotEmpty() },
        OverviewSection(
            title = stringResource(R.string.vehicles_tab_electrical),
            editTab = VehicleDetailTab.ELECTRICAL,
            items = electricalItems(vehicle),
        ).takeIf { it.items.isNotEmpty() },
    )
}

@Composable
private fun basicInfoItems(vehicle: Vehicle): List<InfoItem> = infoItems(
    stringResource(R.string.vehicles_field_name) to vehicle.name,
    stringResource(R.string.vehicles_field_brand) to vehicle.brand,
    stringResource(R.string.vehicles_field_model) to vehicle.model,
    stringResource(R.string.vehicles_field_color) to vehicle.color,
    stringResource(R.string.vehicles_field_purchase_date) to vehicle.purchaseDate,
    stringResource(R.string.vehicles_field_purchase_price) to suffix(vehicle.purchasePrice, "€"),
)

@Composable
private fun identificationItems(vehicle: Vehicle): List<InfoItem> = infoItems(
    stringResource(R.string.vehicles_field_frame_number) to vehicle.frameNumber,
    stringResource(R.string.vehicles_field_engine_number) to vehicle.engineNumber,
)

@Composable
private fun engineItems(vehicle: Vehicle): List<InfoItem> {
    val e = vehicle.engine
    val isTwoStroke = e.cycleType == EngineCycleType.TWO_STROKE
    val coreItems = infoItems(
        stringResource(R.string.vehicles_field_engine_cycle) to engineCycleLabel(e.cycleType),
        stringResource(R.string.vehicles_field_displacement) to suffix(e.displacementCc, "cm³"),
        stringResource(R.string.vehicles_field_bore) to suffix(e.boreMm, "mm"),
        stringResource(R.string.vehicles_field_stroke) to suffix(e.strokeMm, "mm"),
        stringResource(R.string.vehicles_field_compression) to e.compressionRatio,
        stringResource(R.string.vehicles_field_squish) to suffix(e.squishClearanceMm, "mm").takeIf { isTwoStroke },
        stringResource(R.string.vehicles_field_cylinder_head) to e.cylinderHead,
        stringResource(R.string.vehicles_field_piston) to e.piston,
        stringResource(R.string.vehicles_field_reed_valve) to e.reedValve.takeIf { isTwoStroke },
        stringResource(R.string.vehicles_field_intake_system) to e.intakeSystem,
        stringResource(R.string.vehicles_field_cooling) to e.coolingType,
        stringResource(R.string.vehicles_field_port_timing) to e.portTimingNotes.takeIf { isTwoStroke },
    )
    if (isTwoStroke) {
        return coreItems + infoItems(
            stringResource(R.string.vehicles_field_exhaust) to e.exhaustSystem,
            stringResource(R.string.vehicles_field_engine_notes) to e.engineNotes,
        )
    }
    val clearances = e.resolvedValveClearances()
    val valveInfo = buildList {
        if (e.resolvedCylinderCount() > 1) {
            e.cylinderCount.takeIf { it.isNotBlank() }?.let { count ->
                add(InfoItem(stringResource(R.string.vehicles_field_cylinder_count), count))
            }
        }
        clearances.forEachIndexed { index, clearance ->
            clearance.takeIf { it.isNotBlank() }?.let { value ->
                val label = if (clearances.size == 1) {
                    stringResource(R.string.vehicles_field_valve_clearance)
                } else {
                    stringResource(R.string.vehicles_field_valve_clearance_cylinder, index + 1)
                }
                add(InfoItem(label, value))
            }
        }
    }
    return coreItems + valveInfo + infoItems(
        stringResource(R.string.vehicles_field_exhaust) to e.exhaustSystem,
        stringResource(R.string.vehicles_field_engine_notes) to e.engineNotes,
    )
}

@Composable
private fun tuningItems(vehicle: Vehicle): List<InfoItem> {
    val t = vehicle.carbIgnition
    val capabilities = vehicleCapabilities(vehicle)
    return infoItems(
        stringResource(R.string.vehicles_field_carb_type) to t.carbType,
        stringResource(R.string.vehicles_field_main_jet) to t.mainJet,
        stringResource(R.string.vehicles_field_pilot_jet) to t.pilotJet,
        stringResource(R.string.vehicles_field_needle) to t.needle,
        stringResource(R.string.vehicles_field_fuel_mix) to t.fuelMixRatio.takeIf { capabilities.showFuelMix },
        stringResource(R.string.vehicles_field_fuel_type) to t.fuelType,
        stringResource(R.string.vehicles_field_oil_type) to t.oilType,
        stringResource(R.string.vehicles_field_oil_brand) to t.oilBrand,
        stringResource(R.string.vehicles_field_spark_plug) to t.sparkPlug,
        stringResource(R.string.vehicles_field_ignition_timing) to suffix(t.ignitionTimingDeg, "° vOT"),
        stringResource(R.string.vehicles_field_ignition_system) to t.ignitionSystem,
        stringResource(R.string.vehicles_field_cdi) to t.cdiBox,
        stringResource(R.string.vehicles_field_tuning_notes) to t.tuningNotes,
    )
}

@Composable
private fun drivetrainItems(vehicle: Vehicle): List<InfoItem> {
    val d = vehicle.drivetrain
    val capabilities = vehicleCapabilities(vehicle)
    return infoItems(
        stringResource(R.string.vehicles_field_drive_type) to d.driveType.takeIf {
            capabilities.showVariator || capabilities.showTransmission
        },
        stringResource(R.string.vehicles_field_variator_brand) to d.variatorBrand.takeIf { capabilities.showVariator },
        stringResource(R.string.vehicles_field_variator_weights) to d.variatorWeightsG.takeIf { capabilities.showVariator },
        stringResource(R.string.vehicles_field_variator_rollers) to d.variatorRollers.takeIf { capabilities.showVariator },
        stringResource(R.string.vehicles_field_variator_belt) to d.variatorBelt.takeIf { capabilities.showVariator },
        stringResource(R.string.vehicles_field_variator_guide) to d.variatorGuide.takeIf { capabilities.showVariator },
        stringResource(R.string.vehicles_field_clutch_type) to d.clutchType.takeIf {
            capabilities.showClutch || capabilities.showTransmission
        },
        stringResource(R.string.vehicles_field_clutch_springs) to d.clutchSprings.takeIf { capabilities.showClutch },
        stringResource(R.string.vehicles_field_gearing_primary) to d.gearingPrimary.takeIf {
            capabilities.showChainGearing || capabilities.showTransmission
        },
        stringResource(R.string.vehicles_field_gearing_secondary) to d.gearingSecondary.takeIf {
            capabilities.showChainGearing || capabilities.showTransmission
        },
        stringResource(R.string.vehicles_field_front_sprocket) to d.frontSprocketTeeth.takeIf { capabilities.showChainGearing },
        stringResource(R.string.vehicles_field_rear_sprocket) to d.rearSprocketTeeth.takeIf { capabilities.showChainGearing },
        stringResource(R.string.vehicles_field_chain_type) to d.chainType.takeIf { capabilities.showChainGearing },
        stringResource(R.string.vehicles_field_chain_links) to d.chainLinks.takeIf { capabilities.showChainGearing },
        stringResource(R.string.vehicles_field_drivetrain_notes) to d.finalDriveNotes,
    )
}

@Composable
private fun chassisItems(vehicle: Vehicle): List<InfoItem> {
    val c = vehicle.chassis
    return infoItems(
        stringResource(R.string.vehicles_field_front_tire) to c.frontTire,
        stringResource(R.string.vehicles_field_rear_tire) to c.rearTire,
        stringResource(R.string.vehicles_field_tire_pressure_front) to suffix(c.tirePressureFront, "bar"),
        stringResource(R.string.vehicles_field_tire_pressure_rear) to suffix(c.tirePressureRear, "bar"),
        stringResource(R.string.vehicles_field_rim_front) to c.rimFront,
        stringResource(R.string.vehicles_field_rim_rear) to c.rimRear,
        stringResource(R.string.vehicles_field_front_brake) to c.frontBrake,
        stringResource(R.string.vehicles_field_rear_brake) to c.rearBrake,
        stringResource(R.string.vehicles_field_brake_pads_front) to c.brakePadsFront,
        stringResource(R.string.vehicles_field_brake_pads_rear) to c.brakePadsRear,
        stringResource(R.string.vehicles_field_brake_fluid) to c.brakeFluid,
        stringResource(R.string.vehicles_field_fork_oil) to c.forkOil,
        stringResource(R.string.vehicles_field_rear_shock) to c.rearShock,
        stringResource(R.string.vehicles_field_steering_bearing) to c.steeringBearing,
        stringResource(R.string.vehicles_field_chassis_notes) to c.chassisNotes,
    )
}

@Composable
private fun electricalItems(vehicle: Vehicle): List<InfoItem> {
    val e = vehicle.electrical
    return infoItems(
        stringResource(R.string.vehicles_field_battery) to e.battery,
        stringResource(R.string.vehicles_field_battery_year) to e.batteryYear,
        stringResource(R.string.vehicles_field_alternator) to e.alternator,
        stringResource(R.string.vehicles_field_regulator) to e.regulator,
        stringResource(R.string.vehicles_field_ignition_coil) to e.ignitionCoil,
        stringResource(R.string.vehicles_field_wiring_notes) to e.wiringNotes,
        stringResource(R.string.vehicles_field_headlight) to e.headlightBulb,
        stringResource(R.string.vehicles_field_taillight) to e.tailLightBulb,
        stringResource(R.string.vehicles_field_horn) to e.horn,
        stringResource(R.string.vehicles_field_electrical_notes) to e.electricalNotes,
    )
}

@Composable
private fun maintenanceScheduleItems(vehicle: Vehicle): List<InfoItem> {
    val s = vehicle.serviceSchedule
    val capabilities = vehicleCapabilities(vehicle)
    return infoItems(
        stringResource(R.string.vehicles_field_oil_interval) to suffix(s.oilChangeIntervalKm, "km"),
        stringResource(R.string.vehicles_field_spark_interval) to suffix(s.sparkPlugIntervalKm, "km"),
        stringResource(R.string.vehicles_field_variator_interval) to suffix(s.variatorServiceIntervalKm, "km")
            .takeIf { capabilities.showVariatorMaintenance },
        stringResource(R.string.vehicles_field_brake_interval) to suffix(s.brakeServiceIntervalKm, "km"),
        stringResource(R.string.vehicles_field_tire_interval) to suffix(s.tireServiceIntervalKm, "km"),
        stringResource(R.string.vehicles_field_last_oil_date) to s.lastOilChangeDate,
        stringResource(R.string.vehicles_field_last_oil_km) to suffix(s.lastOilChangeKm, "km"),
    )
}

@Composable
private fun documentItems(vehicle: Vehicle): List<InfoItem> {
    val d = vehicle.documents
    return infoItems(
        stringResource(R.string.vehicles_field_insurance_company) to d.insuranceCompany,
        stringResource(R.string.vehicles_field_insurance_policy) to d.insurancePolicyNumber,
        stringResource(R.string.vehicles_field_insurance_type) to d.insuranceType,
        stringResource(R.string.vehicles_field_registration_cert) to d.registrationCertNumber,
        stringResource(R.string.vehicles_field_owner) to d.ownerOnPaper,
        stringResource(R.string.vehicles_field_purchase_from) to d.purchaseFrom,
        stringResource(R.string.vehicles_field_storage) to d.storageLocation,
        stringResource(R.string.vehicles_field_keys) to d.keyLocation,
        stringResource(R.string.vehicles_field_document_notes) to d.documentNotes,
    )
}

private fun infoItems(vararg pairs: Pair<String, String?>): List<InfoItem> =
    pairs.mapNotNull { (label, value) ->
        value?.takeIf { it.isNotBlank() }?.let { InfoItem(label, it) }
    }

private fun suffix(value: String, unit: String): String? =
    value.takeIf { it.isNotBlank() }?.let { "$it $unit" }
