package com.simplestsoft.twostrokecalc.ui.screens.tools

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.simplestsoft.twostrokecalc.R
import com.simplestsoft.twostrokecalc.domain.model.ToolId
import com.simplestsoft.twostrokecalc.domain.model.ToolMeasurementSession
import com.simplestsoft.twostrokecalc.ui.components.AppColors
import com.simplestsoft.twostrokecalc.ui.tools.tabLabelRes
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ToolSessionHistoryContent(
    toolId: ToolId,
    sessions: List<ToolMeasurementSession>,
    canDelete: Boolean,
    onDeleteSession: (String) -> Unit,
    detailBackTicket: Int = 0,
    onDetailVisibleChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var pendingDelete by remember { mutableStateOf<ToolMeasurementSession?>(null) }
    var selectedSession by remember { mutableStateOf<ToolMeasurementSession?>(null) }
    val toolSessions = remember(sessions, toolId) {
        sessions.filter { it.toolId == toolId }.sortedByDescending { it.createdAt }
    }

    LaunchedEffect(detailBackTicket) {
        if (detailBackTicket > 0) {
            selectedSession = null
        }
    }
    LaunchedEffect(selectedSession) {
        onDetailVisibleChange(selectedSession != null)
    }

    selectedSession?.let { session ->
        ToolSessionHistoryDetail(
            session = session,
            modifier = modifier,
        )
        return
    }

    if (toolSessions.isEmpty()) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.tool_session_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = AppColors.textSecondary(),
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(toolSessions, key = { it.id }) { session ->
            ToolSessionHistoryCard(
                session = session,
                canDelete = canDelete,
                onOpen = { selectedSession = session },
                onDelete = { pendingDelete = session },
            )
        }
    }

    pendingDelete?.let { session ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.tool_session_delete)) },
            text = { Text(stringResource(R.string.tool_session_delete_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteSession(session.id)
                        pendingDelete = null
                    },
                ) {
                    Text(stringResource(R.string.tool_session_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun ToolSessionHistoryDetail(
    session: ToolMeasurementSession,
    modifier: Modifier = Modifier,
) {
    val metrics = toolSessionMetrics(session)
    val dateText = DateFormat.getDateTimeInstance(
        DateFormat.MEDIUM,
        DateFormat.SHORT,
        Locale.getDefault(),
    ).format(Date(session.createdAt))
    val dynoCharts = remember(session.id, session.resultJson) {
        ToolSessionChartJson.parseDynoCharts(session.resultJson)
    }
    val vibrationCharts = remember(session.id, session.resultJson) {
        ToolSessionChartJson.parseVibrationCharts(session.resultJson)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = session.title.ifBlank { stringResource(session.toolId.tabLabelRes()) },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.textPrimary(),
        )
        Text(
            text = dateText,
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.textSecondary(),
        )
        if (metrics.isNotBlank()) {
            Text(
                text = metrics,
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.primaryBlue(),
            )
        }
        if (session.notes.isNotBlank()) {
            Text(
                text = session.notes,
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.textSecondary(),
            )
        }

        when (session.toolId) {
            ToolId.GPS_DYNO -> {
                dynoCharts?.speedChart?.takeIf { it.size >= 2 }?.let { points ->
                    Text(
                        text = stringResource(R.string.tool_dyno_chart),
                        style = MaterialTheme.typography.titleSmall,
                        color = AppColors.textPrimary(),
                    )
                    DynoSpeedChart(
                        points = points,
                        timeAxisLabel = stringResource(R.string.tool_rpm_chart_time_axis),
                        speedAxisLabel = stringResource(R.string.tool_dyno_chart_speed_axis),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                    )
                }
                dynoCharts?.powerChart?.takeIf { it.size >= 2 }?.let { points ->
                    Text(
                        text = stringResource(R.string.tool_dyno_power_chart),
                        style = MaterialTheme.typography.titleSmall,
                        color = AppColors.textPrimary(),
                    )
                    DynoPowerChart(
                        points = points,
                        speedAxisLabel = stringResource(R.string.tool_dyno_chart_speed_axis),
                        powerAxisLabel = stringResource(R.string.tool_dyno_chart_power_axis),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                    )
                }
            }
            ToolId.VIBRATION_ANALYZER -> {
                vibrationCharts?.let { charts ->
                    val result = charts.result
                    Text(
                        "${stringResource(R.string.tool_vibration_noise_floor)}: " +
                            String.format(Locale.getDefault(), "%.4f", result.noiseFloor),
                        color = AppColors.textSecondary(),
                    )
                    Text(
                        "${stringResource(R.string.tool_vibration_rms)}: " +
                            String.format(Locale.getDefault(), "%.3f", result.rms),
                        color = AppColors.textPrimary(),
                    )
                    Text(
                        "${stringResource(R.string.tool_vibration_dominant)}: " +
                            if (result.hasSignificantVibration) {
                                String.format(Locale.getDefault(), "%.1f Hz", result.dominantFrequencyHz)
                            } else {
                                "—"
                            },
                        color = AppColors.textPrimary(),
                    )
                    if (charts.spectrum.size >= 2) {
                        VibrationSpectrumChart(
                            primary = result,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                        )
                    }
                    if (charts.rmsChart.size >= 2) {
                        Text(
                            text = stringResource(R.string.tool_vibration_chart),
                            style = MaterialTheme.typography.titleSmall,
                            color = AppColors.textPrimary(),
                        )
                        VibrationRmsChart(
                            points = charts.rmsChart,
                            timeAxisLabel = stringResource(R.string.tool_rpm_chart_time_axis),
                            valueAxisLabel = stringResource(R.string.tool_vibration_rms),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                        )
                    }
                }
            }
            else -> Unit
        }

        if (
            (session.toolId == ToolId.GPS_DYNO && dynoCharts == null) ||
            (session.toolId == ToolId.VIBRATION_ANALYZER && vibrationCharts == null)
        ) {
            Text(
                text = stringResource(R.string.tool_session_no_chart),
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.textSecondary(),
            )
        }
    }
}

@Composable
private fun ToolSessionHistoryCard(
    session: ToolMeasurementSession,
    canDelete: Boolean,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    val metrics = toolSessionMetrics(session)
    val dateText = DateFormat.getDateTimeInstance(
        DateFormat.MEDIUM,
        DateFormat.SHORT,
        Locale.getDefault(),
    ).format(Date(session.createdAt))
    val hasChartData = session.toolId == ToolId.GPS_DYNO ||
        session.toolId == ToolId.VIBRATION_ANALYZER

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppColors.surface()),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 14.dp, bottom = 14.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(enabled = hasChartData, onClick = onOpen),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = session.title.ifBlank { stringResource(session.toolId.tabLabelRes()) },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.textPrimary(),
                )
                if (metrics.isNotBlank()) {
                    Text(
                        text = metrics,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.primaryBlue(),
                    )
                }
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.textSecondary(),
                )
                if (hasChartData) {
                    Text(
                        text = stringResource(R.string.tool_session_open_detail),
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.primaryBlue(),
                    )
                }
            }
            if (canDelete) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.tool_session_delete),
                        tint = AppColors.textSecondary(),
                    )
                }
            }
        }
    }
}
