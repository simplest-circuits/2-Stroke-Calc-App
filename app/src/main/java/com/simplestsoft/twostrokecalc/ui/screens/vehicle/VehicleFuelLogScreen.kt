package com.simplestsoft.twostrokecalc.ui.screens.vehicle

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.simplestsoft.twostrokecalc.R
import com.simplestsoft.twostrokecalc.domain.model.FuelLogEntry
import com.simplestsoft.twostrokecalc.domain.vehicles.FuelConsumptionCalculator
import com.simplestsoft.twostrokecalc.domain.vehicles.FuelConsumptionSegment
import com.simplestsoft.twostrokecalc.domain.vehicles.FuelConsumptionSummary
import com.simplestsoft.twostrokecalc.domain.vehicles.FuelCounterUnit
import com.simplestsoft.twostrokecalc.ui.components.AppColors
import com.simplestsoft.twostrokecalc.ui.components.ProReadOnlyBanner
import com.simplestsoft.twostrokecalc.ui.pro.LocalCalculatorEditingEnabled
import com.simplestsoft.twostrokecalc.ui.pro.LocalVehicleEditingEnabled
import com.simplestsoft.twostrokecalc.ui.pro.requireProEdit
import com.simplestsoft.twostrokecalc.ui.components.FormFieldDefaults
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorCollapsibleSection
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorDecimalField
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorResultRow
import com.simplestsoft.twostrokecalc.ui.theme.ContainerCornerRadius
import com.simplestsoft.twostrokecalc.ui.util.keyboardAwareScroll
import com.simplestsoft.twostrokecalc.ui.vehicles.VehiclesViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleFuelLogScreen(
    vehicleId: String,
    onBack: () -> Unit,
    onEditLocked: () -> Unit = {},
    viewModel: VehiclesViewModel = hiltViewModel(),
) {
    val vehicles by viewModel.vehicles.collectAsStateWithLifecycle()
    val canEdit by viewModel.canEditVehicles.collectAsStateWithLifecycle()
    val vehicle = vehicles.firstOrNull { it.id == vehicleId }?.withLegacyMigration()
    var showEntryDialog by rememberSaveable { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<FuelLogEntry?>(null) }

    LaunchedEffect(vehicleId, vehicles) {
        if (vehicles.isNotEmpty() && vehicle == null) {
            onBack()
        }
    }

    if (vehicle == null) return

    val summary = remember(vehicle.fuelLog) { FuelConsumptionCalculator.summarize(vehicle.fuelLog) }
    val sortedEntries = remember(vehicle.fuelLog) {
        vehicle.fuelLog.sortedByDescending { entry ->
            FuelConsumptionCalculator.entryCounterValue(entry)?.first ?: 0.0
        }
    }

    if (showEntryDialog) {
        FuelLogEntryDialog(
            entry = editingEntry ?: FuelLogEntry(),
            isNewEntry = editingEntry == null,
            onDismiss = {
                showEntryDialog = false
                editingEntry = null
            },
            onConfirm = { entry ->
                if (editingEntry != null) {
                    viewModel.updateFuelLogEntry(vehicleId, entry)
                } else {
                    viewModel.addFuelLogEntry(vehicleId, entry)
                }
                showEntryDialog = false
                editingEntry = null
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
                        text = stringResource(R.string.vehicles_fuel_log_title),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.vehicles_back),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    requireProEdit(canEdit, onEditLocked) {
                        editingEntry = null
                        showEntryDialog = true
                    }
                },
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.vehicles_fuel_log_add))
            }
        },
    ) { paddingValues ->
        CompositionLocalProvider(
            LocalVehicleEditingEnabled provides canEdit,
            LocalCalculatorEditingEnabled provides canEdit,
        ) {
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
            Text(
                text = vehicle.displayTitle(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.textPrimary(),
            )

            FuelConsumptionSummaryCard(summary = summary)

            if (summary.segments.isNotEmpty()) {
                FuelConsumptionChartCard(
                    segments = summary.segments,
                    entries = vehicle.fuelLog,
                    onEditEntry = { entry ->
                        editingEntry = entry
                        showEntryDialog = true
                    },
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.vehicles_fuel_log_entries),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.textPrimary(),
                )
                if (sortedEntries.isEmpty()) {
                    Text(
                        text = stringResource(R.string.vehicles_fuel_log_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.textSecondary(),
                    )
                } else {
                    sortedEntries.forEach { entry ->
                        FuelLogEntryCard(
                            entry = entry,
                            onEdit = {
                                requireProEdit(canEdit, onEditLocked) {
                                    editingEntry = entry
                                    showEntryDialog = true
                                }
                            },
                            onDelete = {
                                requireProEdit(canEdit, onEditLocked) {
                                    viewModel.deleteFuelLogEntry(vehicleId, entry.id)
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
private fun FuelConsumptionSummaryCard(summary: FuelConsumptionSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(ContainerCornerRadius),
        colors = CardDefaults.cardColors(containerColor = AppColors.surface()),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.vehicles_fuel_log_average),
                style = MaterialTheme.typography.labelLarge,
                color = AppColors.textSecondary(),
            )
            val averageText = summary.averageConsumption?.let { formatConsumption(it, summary.unit) }
                ?: stringResource(R.string.vehicles_fuel_log_average_unavailable)
            Text(
                text = averageText,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = AppColors.textPrimary(),
            )
            if (summary.unit != null && summary.averageConsumption == null) {
                Text(
                    text = stringResource(R.string.vehicles_fuel_log_average_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.textSecondary(),
                )
            }
        }
    }
}

@Composable
private fun FuelConsumptionChartCard(
    segments: List<FuelConsumptionSegment>,
    entries: List<FuelLogEntry>,
    onEditEntry: (FuelLogEntry) -> Unit,
) {
    val unit = segments.first().unit
    var selectedEntryId by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedSegment = segments.firstOrNull { it.entryId == selectedEntryId }
    val selectedEntry = entries.firstOrNull { it.id == selectedEntryId }

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
                text = stringResource(R.string.vehicles_fuel_log_chart_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.textPrimary(),
            )
            Text(
                text = stringResource(R.string.vehicles_fuel_log_chart_tap_hint),
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.textSecondary(),
            )
            FuelConsumptionChartWithAxes(
                segments = segments,
                unit = unit,
                selectedEntryId = selectedEntryId,
                onSegmentSelected = { entryId ->
                    selectedEntryId = if (selectedEntryId == entryId) null else entryId
                },
                onPageChanged = { selectedEntryId = null },
                overlayContent = {
                    if (selectedSegment != null && selectedEntry != null) {
                        FuelLogChartPointOverlay(
                            segment = selectedSegment,
                            entry = selectedEntry,
                            onEdit = { onEditEntry(selectedEntry) },
                            onDismiss = { selectedEntryId = null },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                        )
                    }
                },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FuelConsumptionChartWithAxes(
    segments: List<FuelConsumptionSegment>,
    unit: FuelCounterUnit,
    selectedEntryId: String?,
    onSegmentSelected: (String) -> Unit,
    onPageChanged: () -> Unit,
    overlayContent: @Composable BoxScope.() -> Unit,
) {
    val pages = remember(segments) { chunkFuelChartSegments(segments) }
    val pagerState = rememberPagerState(
        initialPage = (pages.size - 1).coerceAtLeast(0),
        pageCount = { pages.size },
    )

    var previousPage by rememberSaveable(segments.size) { mutableStateOf<Int?>(null) }
    LaunchedEffect(pagerState.settledPage) {
        if (previousPage != null && pagerState.settledPage != previousPage) {
            onPageChanged()
        }
        previousPage = pagerState.settledPage
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (pages.size > 1) {
            Text(
                text = stringResource(R.string.vehicles_fuel_log_chart_swipe_hint),
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.textSecondary(),
            )
            val currentPageSegments = pages[pagerState.currentPage]
            Text(
                text = stringResource(
                    R.string.vehicles_fuel_log_chart_page_range,
                    formatFuelChartDateLabel(currentPageSegments.first().dateLabel),
                    formatFuelChartDateLabel(currentPageSegments.last().dateLabel),
                ),
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.textSecondary(),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }

        if (pages.size == 1) {
            FuelConsumptionChartPage(
                segments = pages.first(),
                unit = unit,
                selectedEntryId = selectedEntryId,
                onSegmentSelected = onSegmentSelected,
                overlayContent = overlayContent,
            )
        } else {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth(),
            ) { page ->
                FuelConsumptionChartPage(
                    segments = pages[page],
                    unit = unit,
                    selectedEntryId = selectedEntryId,
                    onSegmentSelected = onSegmentSelected,
                    overlayContent = overlayContent,
                )
            }
        }
    }
}

@Composable
private fun FuelConsumptionChartPage(
    segments: List<FuelConsumptionSegment>,
    unit: FuelCounterUnit,
    selectedEntryId: String?,
    onSegmentSelected: (String) -> Unit,
    overlayContent: @Composable BoxScope.() -> Unit,
) {
    val yAxisTitle = consumptionUnitLabel(unit)
    val scale = remember(segments) { computeFuelChartScale(segments) }
    val yTicks = remember(scale) { buildFuelChartYAxisTicks(scale) }
    val axisLabelStyle = MaterialTheme.typography.labelSmall
    val axisLabelColor = AppColors.textSecondary()

    Column(modifier = Modifier.fillMaxWidth()) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val chartWidth = maxWidth - FuelChartLayout.yAxisWidth

            Text(
                text = yAxisTitle,
                style = axisLabelStyle,
                color = axisLabelColor,
                modifier = Modifier
                    .width(FuelChartLayout.yAxisWidth)
                    .padding(top = 2.dp),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Box(
                modifier = Modifier
                    .width(FuelChartLayout.yAxisWidth)
                    .height(FuelChartLayout.chartHeight)
                    .padding(top = FuelChartLayout.yAxisTitleHeight),
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
                                y = FuelChartLayout.chartHeight * tick.fraction - 7.dp,
                            ),
                    )
                }
            }

            Box(
                modifier = Modifier
                    .padding(
                        start = FuelChartLayout.yAxisWidth,
                        top = FuelChartLayout.yAxisTitleHeight,
                    )
                    .width(chartWidth)
                    .height(FuelChartLayout.chartHeight),
            ) {
                FuelConsumptionChart(
                    segments = segments,
                    scale = scale,
                    selectedEntryId = selectedEntryId,
                    onSegmentSelected = onSegmentSelected,
                    modifier = Modifier.fillMaxSize(),
                )
                if (selectedEntryId == null || segments.any { it.entryId == selectedEntryId }) {
                    overlayContent()
                }
            }
        }

        FuelConsumptionXAxis(
            segments = segments,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = FuelChartLayout.yAxisWidth, top = 2.dp),
        )
        Text(
            text = stringResource(R.string.vehicles_fuel_log_chart_axis_date),
            style = axisLabelStyle,
            color = axisLabelColor,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = FuelChartLayout.yAxisWidth),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun FuelLogChartPointOverlay(
    segment: FuelConsumptionSegment,
    entry: FuelLogEntry,
    onEdit: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(12.dp)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(AppColors.accentSurfaceModifier(shape)),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = AppColors.surface().copy(alpha = 0.97f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = fuelLogEntryTitle(entry),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.textPrimary(),
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.cancel),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
            CalculatorResultRow(
                label = stringResource(R.string.vehicles_fuel_log_consumption),
                value = formatConsumption(segment.consumption, segment.unit),
            )
            if (entry.date.isNotBlank()) {
                CalculatorResultRow(
                    label = stringResource(R.string.vehicles_fuel_log_date),
                    value = entry.date,
                )
            }
            if (entry.odometerKm.isNotBlank()) {
                CalculatorResultRow(
                    label = stringResource(R.string.vehicles_fuel_log_odometer),
                    value = "${entry.odometerKm} km",
                )
            }
            if (entry.operatingHours.isNotBlank()) {
                CalculatorResultRow(
                    label = stringResource(R.string.vehicles_fuel_log_operating_hours),
                    value = "${entry.operatingHours} h",
                )
            }
            if (entry.liters.isNotBlank()) {
                CalculatorResultRow(
                    label = stringResource(R.string.vehicles_fuel_log_liters),
                    value = "${entry.liters} L",
                )
            }
            if (entry.price.isNotBlank()) {
                CalculatorResultRow(
                    label = stringResource(R.string.vehicles_fuel_log_price),
                    value = "${entry.price} €",
                )
            }
            Button(
                onClick = onEdit,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.vehicles_fuel_log_edit))
            }
        }
    }
}

@Composable
private fun FuelConsumptionXAxis(
    segments: List<FuelConsumptionSegment>,
    modifier: Modifier = Modifier,
) {
    val axisLabelStyle = MaterialTheme.typography.labelSmall
    val axisLabelColor = AppColors.textSecondary()
    val tickIndices = remember(segments.size) { selectFuelChartXAxisIndices(segments.size) }
    val labelWidth = 44.dp
    val horizontalPadding = FuelChartLayout.horizontalPad

    BoxWithConstraints(
        modifier = modifier.height(36.dp),
    ) {
        val chartWidth = maxWidth - horizontalPadding * 2

        tickIndices.forEach { index ->
            val segment = segments[index]
            val fraction = if (segments.size == 1) {
                0.5f
            } else {
                index / (segments.size - 1).toFloat()
            }
            val centerX = horizontalPadding + chartWidth * fraction
            val labelOffset = (centerX - labelWidth / 2).coerceIn(0.dp, maxWidth - labelWidth)

            Text(
                text = formatFuelChartDateLabel(segment.dateLabel),
                style = axisLabelStyle,
                color = axisLabelColor,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .width(labelWidth)
                    .offset(x = labelOffset),
            )
        }
    }
}

private fun selectFuelChartXAxisIndices(segmentCount: Int): List<Int> = when {
    segmentCount <= 0 -> emptyList()
    segmentCount <= 5 -> (0 until segmentCount).toList()
    else -> {
        val maxTicks = 5
        (0 until maxTicks).map { step ->
            (step * (segmentCount - 1)) / (maxTicks - 1)
        }.distinct()
    }
}

private fun formatFuelChartDateLabel(dateLabel: String): String {
    val parsed = parseVehicleDateTime(dateLabel)
    return when {
        parsed != null -> parsed.format(DateTimeFormatter.ofPattern("dd.MM.yy", Locale.GERMANY))
        dateLabel.length > 8 -> dateLabel.take(8)
        else -> dateLabel
    }
}

private data class FuelChartScale(
    val minValue: Double,
    val maxValue: Double,
    val range: Double,
)

private data class FuelChartYAxisTick(
    val fraction: Float,
    val label: String,
)

private object FuelChartLayout {
    val yAxisWidth = 48.dp
    val yAxisTitleHeight = 18.dp
    val chartHeight = 150.dp
    val topPad = 12.dp
    val bottomPad = 4.dp
    val horizontalPad = 8.dp
    const val visibleSegmentCount = 5
}

private fun chunkFuelChartSegments(
    segments: List<FuelConsumptionSegment>,
): List<List<FuelConsumptionSegment>> {
    if (segments.size <= FuelChartLayout.visibleSegmentCount) {
        return listOf(segments)
    }
    return segments.chunked(FuelChartLayout.visibleSegmentCount)
}

private fun computeFuelChartScale(segments: List<FuelConsumptionSegment>): FuelChartScale {
    val values = segments.map { it.consumption }
    val minValue = min(values.min(), values.average() * 0.85)
    val maxValue = max(values.max(), values.average() * 1.15)
    val range = (maxValue - minValue).takeIf { it > 0.0 } ?: 1.0
    return FuelChartScale(minValue = minValue, maxValue = maxValue, range = range)
}

private fun buildFuelChartYAxisTicks(scale: FuelChartScale): List<FuelChartYAxisTick> {
    return (0..3).map { gridIndex ->
        val value = scale.minValue + scale.range * (1.0 - gridIndex / 3.0)
        FuelChartYAxisTick(
            fraction = gridIndex / 3f,
            label = formatFuelChartYAxisTick(value),
        )
    }
}

private fun formatFuelChartYAxisTick(value: Double): String =
    String.format(Locale.GERMAN, "%.1f", value)

private data class FuelChartPoint(
    val segment: FuelConsumptionSegment,
    val center: Offset,
)

private fun computeFuelChartPoints(
    segments: List<FuelConsumptionSegment>,
    scale: FuelChartScale,
    width: Float,
    height: Float,
    density: Density,
): List<FuelChartPoint> {
    if (segments.isEmpty() || width <= 0f || height <= 0f) return emptyList()

    val leftPad = with(density) { FuelChartLayout.horizontalPad.toPx() }
    val rightPad = with(density) { FuelChartLayout.horizontalPad.toPx() }
    val topPad = with(density) { FuelChartLayout.topPad.toPx() }
    val bottomPad = with(density) { FuelChartLayout.bottomPad.toPx() }
    val chartWidth = width - leftPad - rightPad
    val chartHeight = height - topPad - bottomPad

    return segments.mapIndexed { index, segment ->
        val x = if (segments.size == 1) {
            leftPad + chartWidth / 2f
        } else {
            leftPad + chartWidth * index / (segments.size - 1).toFloat()
        }
        val normalized = (segment.consumption - scale.minValue) / scale.range
        val y = topPad + chartHeight * (1f - normalized.toFloat())
        FuelChartPoint(segment, Offset(x, y))
    }
}

@Composable
private fun FuelConsumptionChart(
    segments: List<FuelConsumptionSegment>,
    scale: FuelChartScale,
    selectedEntryId: String?,
    onSegmentSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = AppColors.textSecondary().copy(alpha = 0.2f)
    val pointColor = MaterialTheme.colorScheme.secondary
    val selectedPointColor = MaterialTheme.colorScheme.primary
    val hitRadiusPx = with(LocalDensity.current) { 24.dp.toPx() }

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(segments, scale, selectedEntryId) {
                    detectTapGestures { tapOffset ->
                        val chartPoints = computeFuelChartPoints(
                            segments = segments,
                            scale = scale,
                            width = size.width.toFloat(),
                            height = size.height.toFloat(),
                            density = this,
                        )
                        val tappedPoint = chartPoints.firstOrNull { point ->
                            (tapOffset - point.center).getDistance() <= hitRadiusPx
                        }
                        tappedPoint?.let { onSegmentSelected(it.segment.entryId) }
                    }
                },
        ) {
            if (segments.isEmpty()) return@Canvas

            val chartPoints = computeFuelChartPoints(
                segments = segments,
                scale = scale,
                width = size.width,
                height = size.height,
                density = this,
            )
            val points = chartPoints.map { it.center }

            val leftPad = FuelChartLayout.horizontalPad.toPx()
            val rightPad = FuelChartLayout.horizontalPad.toPx()
            val topPad = FuelChartLayout.topPad.toPx()
            val chartWidth = size.width - leftPad - rightPad
            val chartHeight = size.height - topPad - FuelChartLayout.bottomPad.toPx()

            for (gridIndex in 0..3) {
                val y = topPad + chartHeight * gridIndex / 3f
                drawLine(
                    color = gridColor,
                    start = Offset(leftPad, y),
                    end = Offset(leftPad + chartWidth, y),
                    strokeWidth = 1.dp.toPx(),
                )
            }

            if (points.size > 1) {
                val path = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    points.drop(1).forEach { lineTo(it.x, it.y) }
                }
                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
                )
            }

            chartPoints.forEach { chartPoint ->
                val isSelected = chartPoint.segment.entryId == selectedEntryId
                val outerRadius = if (isSelected) 8.dp.toPx() else 5.dp.toPx()
                val innerRadius = if (isSelected) 5.dp.toPx() else 3.dp.toPx()
                drawCircle(
                    color = if (isSelected) selectedPointColor else pointColor,
                    radius = outerRadius,
                    center = chartPoint.center,
                )
                drawCircle(
                    color = if (isSelected) Color.White else lineColor,
                    radius = innerRadius,
                    center = chartPoint.center,
                )
            }
        }
    }
}

@Composable
private fun FuelLogEntryCard(
    entry: FuelLogEntry,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    CalculatorCollapsibleSection(
        title = fuelLogEntryTitle(entry),
        initiallyExpanded = false,
    ) {
        if (entry.date.isNotBlank()) {
            FuelLogInfoRow(stringResource(R.string.vehicles_fuel_log_date), entry.date)
        }
        if (entry.odometerKm.isNotBlank()) {
            FuelLogInfoRow(
                stringResource(R.string.vehicles_fuel_log_odometer),
                "${entry.odometerKm} km",
            )
        }
        if (entry.operatingHours.isNotBlank()) {
            FuelLogInfoRow(
                stringResource(R.string.vehicles_fuel_log_operating_hours),
                "${entry.operatingHours} h",
            )
        }
        if (entry.liters.isNotBlank()) {
            FuelLogInfoRow(stringResource(R.string.vehicles_fuel_log_liters), "${entry.liters} L")
        }
        if (entry.price.isNotBlank()) {
            FuelLogInfoRow(stringResource(R.string.vehicles_fuel_log_price), "${entry.price} €")
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onEdit) {
                Text(stringResource(R.string.vehicles_fuel_log_edit))
            }
            TextButton(onClick = onDelete) {
                Text(stringResource(R.string.vehicles_delete), color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun FuelLogInfoRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(bottom = 4.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = AppColors.textSecondary())
        Text(value, style = MaterialTheme.typography.bodyMedium, color = AppColors.textPrimary())
    }
}

@Composable
private fun FuelLogEntryDialog(
    entry: FuelLogEntry,
    isNewEntry: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (FuelLogEntry) -> Unit,
) {
    var dateTime by remember(entry.id) {
        mutableStateOf(parseVehicleDateTime(entry.date) ?: LocalDateTime.now())
    }
    var odometer by rememberSaveable(entry.id) { mutableStateOf(entry.odometerKm) }
    var operatingHours by rememberSaveable(entry.id) { mutableStateOf(entry.operatingHours) }
    var liters by rememberSaveable(entry.id) { mutableStateOf(entry.liters) }
    var price by rememberSaveable(entry.id) { mutableStateOf(entry.price) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (isNewEntry) R.string.vehicles_fuel_log_add else R.string.vehicles_fuel_log_edit,
                ),
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(FormFieldDefaults.fieldSpacing)) {
                VehicleDateTimePickerField(
                    value = dateTime,
                    onValueChange = { dateTime = it },
                    label = stringResource(R.string.vehicles_fuel_log_date),
                )
                CalculatorDecimalField(
                    value = odometer,
                    onValueChange = { odometer = it },
                    label = stringResource(R.string.vehicles_fuel_log_odometer),
                    suffix = "km",
                )
                CalculatorDecimalField(
                    value = operatingHours,
                    onValueChange = { operatingHours = it },
                    label = stringResource(R.string.vehicles_fuel_log_operating_hours),
                    suffix = "h",
                )
                Text(
                    text = stringResource(R.string.vehicles_fuel_log_counter_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.textSecondary(),
                )
                CalculatorDecimalField(
                    value = liters,
                    onValueChange = { liters = it },
                    label = stringResource(R.string.vehicles_fuel_log_liters),
                    suffix = "L",
                )
                CalculatorDecimalField(
                    value = price,
                    onValueChange = { price = it },
                    label = stringResource(R.string.vehicles_fuel_log_price),
                    suffix = "€",
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        entry.copy(
                            date = formatVehicleDateTime(dateTime),
                            odometerKm = odometer,
                            operatingHours = operatingHours,
                            liters = liters,
                            price = price,
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

@Composable
private fun fuelLogEntryTitle(entry: FuelLogEntry): String {
    return when {
        entry.date.isNotBlank() -> entry.date
        entry.odometerKm.isNotBlank() -> "${entry.odometerKm} km"
        entry.operatingHours.isNotBlank() -> "${entry.operatingHours} h"
        else -> stringResource(R.string.vehicles_fuel_log_entry_fallback)
    }
}

@Composable
private fun formatConsumption(value: Double, unit: FuelCounterUnit?): String {
    val formatted = String.format(Locale.GERMAN, "%.2f", value)
    return "$formatted ${consumptionUnitLabel(unit)}"
}

@Composable
private fun consumptionUnitLabel(unit: FuelCounterUnit?): String = when (unit) {
    FuelCounterUnit.KILOMETERS -> stringResource(R.string.vehicles_fuel_log_unit_km)
    FuelCounterUnit.OPERATING_HOURS -> stringResource(R.string.vehicles_fuel_log_unit_hours)
    null -> ""
}
