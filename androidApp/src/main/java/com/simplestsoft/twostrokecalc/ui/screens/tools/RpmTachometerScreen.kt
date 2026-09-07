package com.simplestsoft.twostrokecalc.ui.screens.tools

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Paint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.Modifier
import androidx.compose.foundation.Canvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.simplestsoft.twostrokecalc.R
import com.simplestsoft.twostrokecalc.data.sensors.AndroidAudioSampleSource
import com.simplestsoft.twostrokecalc.domain.model.ToolId
import com.simplestsoft.twostrokecalc.domain.model.ToolMeasurementSession
import com.simplestsoft.twostrokecalc.domain.tools.rpm.RpmAnalysisConfig
import com.simplestsoft.twostrokecalc.domain.tools.rpm.RpmEstimator
import com.simplestsoft.twostrokecalc.domain.tools.rpm.RpmLiveStats
import com.simplestsoft.twostrokecalc.ui.components.AppColors
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorChoiceField
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorDecimalField
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorDropdownOption
import com.simplestsoft.twostrokecalc.ui.components.calculator.filterDecimalInput
import com.simplestsoft.twostrokecalc.ui.components.calculator.parseDecimal
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val FREE_LIMIT_MS = 60_000L
private const val RPM_CHART_MAX_POINTS = 600
private const val RPM_CHART_SAMPLE_INTERVAL_MS = 100L
private const val RPM_UI_UPDATE_INTERVAL_MS = 50L
/** Keep the last lock on screen through brief YIN / RMS dropouts. */
private const val RPM_SIGNAL_HOLD_MS = 4_000L

private data class RpmChartPoint(
    val elapsedMs: Long,
    val rpm: Double,
)

@Composable
fun RpmTachometerScreen(
    canSave: Boolean,
    initialTargetRpm: Double? = null,
    onSaveSession: (ToolMeasurementSession) -> Unit,
    onRequestPro: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val audioSource = remember { AndroidAudioSampleSource() }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> hasPermission = granted }

    var cylinders by remember { mutableIntStateOf(1) }
    var targetRpmText by remember {
        mutableStateOf(initialTargetRpm?.let { String.format(Locale.GERMAN, "%.0f", it) } ?: "")
    }
    var stats by remember { mutableStateOf(RpmLiveStats()) }
    var measuring by remember { mutableStateOf(false) }
    var elapsedMs by remember { mutableLongStateOf(0L) }
    var collectJob by remember { mutableStateOf<Job?>(null) }
    var timerJob by remember { mutableStateOf<Job?>(null) }
    var chartPoints by remember { mutableStateOf<List<RpmChartPoint>>(emptyList()) }

    DisposableEffect(Unit) {
        onDispose {
            collectJob?.cancel()
            timerJob?.cancel()
            audioSource.stop()
            RpmEstimator.reset()
        }
    }

    fun stopMeasurement() {
        measuring = false
        collectJob?.cancel()
        timerJob?.cancel()
        audioSource.stop()
    }

    fun startMeasurement() {
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }
        RpmEstimator.reset()
        stats = RpmLiveStats()
        elapsedMs = 0L
        chartPoints = emptyList()
        measuring = true
        // 2-stroke: one exhaust pulse per revolution per cylinder firing once per rev.
        val config = RpmAnalysisConfig(
            cylinderCount = cylinders,
            pulsesPerRevolution = 1.0,
            hintRpm = parseDecimal(targetRpmText),
        )
        // Quarter-window hops keep the live value updating about 43 times per second.
        val hopSize = config.fftSize / 4
        val acc = FloatArray(config.fftSize * 2)
        val frame = FloatArray(config.fftSize)
        val startedAtMs = System.currentTimeMillis()
        collectJob = scope.launch(Dispatchers.Default) {
            var accSize = 0
            var lastFreshAtMs = 0L
            var lastUiAtMs = 0L
            var lastChartAtMs = 0L
            var signalLostPublished = false
            audioSource.start(config.sampleRateHz, hopSize)
                .catch { withContext(Dispatchers.Main) { stopMeasurement() } }
                .collect { chunk ->
                    if (accSize + chunk.size > acc.size) {
                        val keep = minOf(accSize, config.fftSize)
                        if (keep > 0) acc.copyInto(acc, 0, accSize - keep, accSize)
                        accSize = keep
                        if (accSize + chunk.size > acc.size) accSize = 0
                    }
                    chunk.copyInto(acc, accSize)
                    accSize += chunk.size
                    while (accSize >= config.fftSize) {
                        acc.copyInto(frame, 0, 0, config.fftSize)
                        acc.copyInto(acc, 0, hopSize, accSize)
                        accSize -= hopSize
                        val result = RpmEstimator.analyze(frame, config)
                        val now = System.currentTimeMillis()
                        if (result != null && (result.held || result.confidence >= RpmEstimator.MIN_ACCEPT_CONFIDENCE)) {
                            lastFreshAtMs = now
                            signalLostPublished = false
                        }
                        if (result != null && !result.held && result.confidence >= RpmEstimator.MIN_ACCEPT_CONFIDENCE) {
                            val nowElapsed = now - startedAtMs
                            val shouldChart = nowElapsed - lastChartAtMs >= RPM_CHART_SAMPLE_INTERVAL_MS
                            if (shouldChart) lastChartAtMs = nowElapsed
                            if (now - lastUiAtMs >= RPM_UI_UPDATE_INTERVAL_MS || shouldChart) {
                                lastUiAtMs = now
                                withContext(Dispatchers.Main) {
                                    stats = stats.update(result)
                                    if (shouldChart) {
                                        chartPoints = (chartPoints + RpmChartPoint(nowElapsed, result.rpm))
                                            .takeLast(RPM_CHART_MAX_POINTS)
                                    }
                                }
                            }
                        } else if (
                            lastFreshAtMs > 0L &&
                            now - lastFreshAtMs >= RPM_SIGNAL_HOLD_MS &&
                            !signalLostPublished
                        ) {
                            signalLostPublished = true
                            withContext(Dispatchers.Main) {
                                stats = stats.withoutSignal()
                            }
                        }
                    }
                }
        }
        timerJob = scope.launch {
            val started = System.currentTimeMillis()
            while (measuring) {
                delay(200)
                elapsedMs = System.currentTimeMillis() - started
                if (!canSave && elapsedMs >= FREE_LIMIT_MS) {
                    stopMeasurement()
                    break
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.tool_rpm_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.textSecondary(),
        )
        if (!hasPermission) {
            Text(stringResource(R.string.tool_rpm_permission), color = AppColors.textSecondary())
            Button(onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }) {
                Text(stringResource(R.string.tool_rpm_grant))
            }
        }
        CalculatorChoiceField(
            label = stringResource(R.string.tool_rpm_cylinders),
            options = listOf(
                CalculatorDropdownOption("1", "1"),
                CalculatorDropdownOption("2", "2"),
            ),
            selectedKey = cylinders.toString(),
            onOptionSelected = { cylinders = it.toIntOrNull() ?: 1 },
        )
        CalculatorDecimalField(
            value = targetRpmText,
            onValueChange = { targetRpmText = it.filterDecimalInput() },
            label = stringResource(R.string.tool_rpm_target),
        )
        Text(
            text = if (stats.hasSignal) {
                String.format(Locale.getDefault(), "%.0f", stats.currentRpm)
            } else {
                "—"
            },
            style = MaterialTheme.typography.displayLarge.copy(fontSize = 64.sp),
            fontWeight = FontWeight.Bold,
            color = if (stats.hasSignal) AppColors.textPrimary() else AppColors.textSecondary(),
        )
        Text(
            text = "RPM",
            style = MaterialTheme.typography.titleMedium,
            color = AppColors.textSecondary(),
        )
        if (measuring && !stats.hasSignal) {
            Text(
                text = stringResource(R.string.tool_rpm_no_signal),
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.textSecondary(),
            )
        }
        LinearProgressIndicator(
            progress = { stats.confidence.toFloat().coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = "${stringResource(R.string.tool_rpm_confidence)}: ${(stats.confidence * 100).toInt()}%",
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.textSecondary(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatChip(stringResource(R.string.tool_rpm_min), stats.minRpm)
            StatChip(stringResource(R.string.tool_rpm_max), stats.maxRpm)
            StatChip(stringResource(R.string.tool_rpm_peak), stats.peakHoldRpm)
        }
        if (chartPoints.isNotEmpty()) {
            Text(
                text = stringResource(R.string.tool_rpm_chart),
                style = MaterialTheme.typography.titleSmall,
                color = AppColors.textPrimary(),
            )
            RpmHistoryChart(
                points = chartPoints,
                timeAxisLabel = stringResource(R.string.tool_rpm_chart_time_axis),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
            )
        }
        parseDecimal(targetRpmText)?.let { target ->
            Text(
                text = "${stringResource(R.string.tool_rpm_target)}: ${target.toInt()}",
                color = if (stats.currentRpm >= target * 0.98) AppColors.primaryBlue() else AppColors.textSecondary(),
            )
        }
        if (!canSave) {
            Text(stringResource(R.string.tool_rpm_free_limit), color = AppColors.textSecondary())
        }
        if (measuring) {
            Button(onClick = { stopMeasurement() }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.tool_rpm_stop))
            }
        } else {
            Button(onClick = { startMeasurement() }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.tool_rpm_start))
            }
        }
        OutlinedButton(
            onClick = {
                if (!canSave) {
                    onRequestPro()
                    return@OutlinedButton
                }
                onSaveSession(
                    ToolMeasurementSession(
                        toolId = ToolId.RPM_TACHOMETER,
                        title = "${context.getString(R.string.tool_tab_rpm)} ${stats.currentRpm.toInt()} RPM",
                        resultJson = "{\"rpm\":${stats.currentRpm},\"min\":${stats.minRpm},\"max\":${stats.maxRpm},\"peak\":${stats.peakHoldRpm}}",
                    ),
                )
            },
            enabled = stats.currentRpm > 0,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.tool_rpm_save))
        }
    }
}

@Composable
private fun RpmHistoryChart(
    points: List<RpmChartPoint>,
    timeAxisLabel: String,
    modifier: Modifier = Modifier,
) {
    val lineColor = AppColors.primaryBlue()
    val gridColor = AppColors.textSecondary().copy(alpha = 0.22f)
    val labelColor = AppColors.textSecondary()
    Canvas(modifier = modifier) {
        if (points.size < 2) return@Canvas
        val firstTime = points.first().elapsedMs
        val lastTime = points.last().elapsedMs
        val duration = (lastTime - firstTime).coerceAtLeast(1L)
        val minRpm = (points.minOf { it.rpm } * 0.9).coerceAtLeast(0.0)
        val maxRpm = (points.maxOf { it.rpm } * 1.1).coerceAtLeast(minRpm + 500.0)
        val rpmRange = maxRpm - minRpm
        val leftPadding = 42.dp.toPx()
        val bottomPadding = 24.dp.toPx()
        val plotWidth = (size.width - leftPadding).coerceAtLeast(1f)
        val plotHeight = (size.height - bottomPadding).coerceAtLeast(1f)
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = labelColor.toArgb()
            textSize = 11.sp.toPx()
        }

        repeat(4) { index ->
            val y = plotHeight * index / 3f
            drawLine(
                color = gridColor,
                start = Offset(leftPadding, y),
                end = Offset(size.width, y),
                strokeWidth = 1f,
            )
        }
        repeat(5) { index ->
            val x = leftPadding + plotWidth * index / 4f
            drawLine(
                color = gridColor,
                start = Offset(x, 0f),
                end = Offset(x, plotHeight),
                strokeWidth = 1f,
            )
        }

        val path = Path()
        points.forEachIndexed { index, point ->
            val x = leftPadding + plotWidth * (point.elapsedMs - firstTime).toFloat() / duration
            val y = plotHeight - ((point.rpm - minRpm) / rpmRange * plotHeight).toFloat()
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 3f, cap = StrokeCap.Round),
        )
        drawContext.canvas.nativeCanvas.apply {
            drawText("RPM", 0f, 11.dp.toPx(), labelPaint)
            drawText(minRpm.toInt().toString(), 0f, plotHeight, labelPaint)
            drawText(maxRpm.toInt().toString(), 0f, 11.dp.toPx() + labelPaint.textSize, labelPaint)
            drawText("0 s", leftPadding, size.height - 4.dp.toPx(), labelPaint)
            val durationLabel = String.format(Locale.getDefault(), "%.1f s", duration / 1000.0)
            val durationWidth = labelPaint.measureText(durationLabel)
            drawText(durationLabel, size.width - durationWidth, size.height - 4.dp.toPx(), labelPaint)
            val axisWidth = labelPaint.measureText(timeAxisLabel)
            drawText(
                timeAxisLabel,
                leftPadding + (plotWidth - axisWidth) / 2f,
                size.height - 4.dp.toPx(),
                labelPaint,
            )
        }
    }
}

@Composable
private fun StatChip(label: String, value: Double) {
    val display = if (value.isInfinite() || value.isNaN()) "–" else String.format(Locale.getDefault(), "%.0f", value)
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = AppColors.textSecondary())
        Text(display, style = MaterialTheme.typography.titleMedium, color = AppColors.textPrimary())
    }
}
