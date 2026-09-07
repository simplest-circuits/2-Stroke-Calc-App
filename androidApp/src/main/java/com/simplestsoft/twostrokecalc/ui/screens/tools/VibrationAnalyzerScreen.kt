package com.simplestsoft.twostrokecalc.ui.screens.tools

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.simplestsoft.twostrokecalc.R
import com.simplestsoft.twostrokecalc.data.sensors.AndroidMotionSampleSource
import com.simplestsoft.twostrokecalc.domain.model.ToolId
import com.simplestsoft.twostrokecalc.domain.model.ToolMeasurementSession
import com.simplestsoft.twostrokecalc.domain.tools.vibration.VibrationAnalysisResult
import com.simplestsoft.twostrokecalc.domain.tools.vibration.VibrationAnalyzer
import com.simplestsoft.twostrokecalc.domain.tools.vibration.VibrationKalmanDenoiser
import com.simplestsoft.twostrokecalc.domain.tools.vibration.VibrationSample
import com.simplestsoft.twostrokecalc.ui.components.AppColors
import java.util.Locale
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

private const val VIBRATION_CHART_MAX_POINTS = 600
private const val VIBRATION_CHART_SAMPLE_INTERVAL_MS = 200L
private const val VIBRATION_LIVE_FFT_SIZE = 256
private const val VIBRATION_LIVE_BUFFER_SIZE = 512
private const val VIBRATION_SESSION_MAX_SAMPLES = 60_000

@Composable
fun VibrationAnalyzerScreen(
    canSave: Boolean,
    onSaveSession: (ToolMeasurementSession) -> Unit,
    onRequestPro: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val motionSource = remember { AndroidMotionSampleSource(context.applicationContext) }
    val kalman = remember { VibrationKalmanDenoiser() }
    var measuring by remember { mutableStateOf(false) }
    var warmingUp by remember { mutableStateOf(false) }
    var magnitudes by remember { mutableStateOf(listOf<Double>()) }
    var timestamps by remember { mutableStateOf(listOf<Long>()) }
    var sessionMagnitudes by remember { mutableStateOf(listOf<Double>()) }
    var sessionTimestamps by remember { mutableStateOf(listOf<Long>()) }
    var noiseFloor by remember { mutableStateOf(0.0) }
    var result by remember { mutableStateOf<VibrationAnalysisResult?>(null) }
    var previousResult by remember { mutableStateOf<VibrationAnalysisResult?>(null) }
    var chartPoints by remember { mutableStateOf<List<VibrationRmsChartPoint>>(emptyList()) }
    var measurementStartedAtMs by remember { mutableLongStateOf(0L) }
    var lastChartPointAtMs by remember { mutableLongStateOf(0L) }
    var job by remember { mutableStateOf<Job?>(null) }

    DisposableEffect(Unit) {
        onDispose { job?.cancel() }
    }

    fun appendChartPoint(analysis: VibrationAnalysisResult, force: Boolean = false) {
        if (measurementStartedAtMs == 0L) return
        val elapsed = System.currentTimeMillis() - measurementStartedAtMs
        if (force || elapsed - lastChartPointAtMs >= VIBRATION_CHART_SAMPLE_INTERVAL_MS) {
            lastChartPointAtMs = elapsed
            chartPoints = (chartPoints + VibrationRmsChartPoint(elapsedMs = elapsed, rms = analysis.rms))
                .takeLast(VIBRATION_CHART_MAX_POINTS)
        }
    }

    fun stop() {
        measuring = false
        warmingUp = false
        job?.cancel()
        val analysisMagnitudes = sessionMagnitudes.ifEmpty { magnitudes }
        val analysisTimestamps = sessionTimestamps.ifEmpty { timestamps }
        result = VibrationAnalyzer.analyzeMagnitudes(
            magnitudes = analysisMagnitudes,
            timestampsMs = analysisTimestamps,
            noiseFloor = noiseFloor,
        ) ?: result
        result?.let { appendChartPoint(it, force = true) }
    }

    fun start() {
        kalman.reset()
        magnitudes = emptyList()
        timestamps = emptyList()
        sessionMagnitudes = emptyList()
        sessionTimestamps = emptyList()
        noiseFloor = 0.0
        result = null
        chartPoints = emptyList()
        measurementStartedAtMs = System.currentTimeMillis()
        lastChartPointAtMs = 0L
        measuring = true
        warmingUp = true
        job = scope.launch {
            motionSource.start(100)
                .catch { stop() }
                .collect { sample ->
                    val denoised = kalman.push(
                        VibrationSample(
                            timestampMs = sample.timestampMs,
                            x = sample.x,
                            y = sample.y,
                            z = sample.z,
                        ),
                    )
                    warmingUp = kalman.isWarmingUp()
                    noiseFloor = denoised.noiseFloor
                    if (!warmingUp) {
                        sessionMagnitudes = (sessionMagnitudes + denoised.magnitude)
                            .takeLast(VIBRATION_SESSION_MAX_SAMPLES)
                        sessionTimestamps = (sessionTimestamps + denoised.timestampMs)
                            .takeLast(VIBRATION_SESSION_MAX_SAMPLES)
                        magnitudes = sessionMagnitudes.takeLast(VIBRATION_LIVE_BUFFER_SIZE)
                        timestamps = sessionTimestamps.takeLast(VIBRATION_LIVE_BUFFER_SIZE)
                        if (magnitudes.size >= VIBRATION_LIVE_FFT_SIZE) {
                            val analysis = VibrationAnalyzer.analyzeMagnitudes(
                                magnitudes = magnitudes,
                                timestampsMs = timestamps,
                                noiseFloor = noiseFloor,
                            )
                            if (analysis != null) {
                                result = analysis
                                appendChartPoint(analysis)
                            }
                        }
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
            stringResource(R.string.tool_vibration_hint),
            color = AppColors.textSecondary(),
        )
        Text(
            stringResource(R.string.tool_vibration_kalman_hint),
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.textSecondary(),
        )
        if (warmingUp) {
            Text(
                stringResource(R.string.tool_vibration_calibrating),
                color = AppColors.primaryBlue(),
            )
        }
        result?.let { r ->
            Text(
                "${stringResource(R.string.tool_vibration_noise_floor)}: ${String.format(Locale.getDefault(), "%.4f", r.noiseFloor)}",
                color = AppColors.textSecondary(),
            )
            if (!r.hasSignificantVibration) {
                Text(
                    stringResource(R.string.tool_vibration_no_signal),
                    color = AppColors.textSecondary(),
                )
            }
            Text(
                "${stringResource(R.string.tool_vibration_rms)}: ${String.format(Locale.getDefault(), "%.3f", r.rms)}",
                color = AppColors.textPrimary(),
            )
            Text(
                "${stringResource(R.string.tool_vibration_dominant)}: " +
                    if (r.hasSignificantVibration) {
                        String.format(Locale.getDefault(), "%.1f Hz", r.dominantFrequencyHz)
                    } else {
                        "—"
                    },
                color = AppColors.textPrimary(),
            )
            VibrationSpectrumChart(
                primary = r,
                secondary = previousResult,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
            )
        }
        if (chartPoints.isNotEmpty()) {
            Text(
                text = stringResource(R.string.tool_vibration_chart),
                style = MaterialTheme.typography.titleSmall,
                color = AppColors.textPrimary(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ToolMeasurementStatChip(
                    label = stringResource(R.string.tool_rpm_min),
                    value = chartPoints.minOf { it.rms },
                )
                ToolMeasurementStatChip(
                    label = stringResource(R.string.tool_rpm_max),
                    value = chartPoints.maxOf { it.rms },
                )
                val durationSec = (chartPoints.last().elapsedMs - chartPoints.first().elapsedMs) / 1000.0
                ToolMeasurementStatChip(
                    label = stringResource(R.string.tool_vibration_duration),
                    value = durationSec,
                    suffix = " s",
                )
            }
            VibrationRmsChart(
                points = chartPoints,
                timeAxisLabel = stringResource(R.string.tool_rpm_chart_time_axis),
                valueAxisLabel = stringResource(R.string.tool_vibration_rms),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
            )
        }
        if (measuring) {
            Button(onClick = { stop() }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.tool_vibration_stop))
            }
        } else {
            Button(onClick = { start() }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.tool_vibration_start))
            }
        }
        OutlinedButton(
            onClick = {
                if (!canSave) {
                    onRequestPro()
                    return@OutlinedButton
                }
                previousResult = result
            },
            enabled = result != null,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.tool_vibration_compare))
        }
        OutlinedButton(
            onClick = {
                if (!canSave) {
                    onRequestPro()
                    return@OutlinedButton
                }
                val current = result ?: return@OutlinedButton
                onSaveSession(
                    ToolMeasurementSession(
                        toolId = ToolId.VIBRATION_ANALYZER,
                        title = "${context.getString(R.string.tool_tab_vibration)} " +
                            "${String.format(Locale.getDefault(), "%.1f", current.dominantFrequencyHz)} Hz",
                        resultJson = ToolSessionChartJson.encodeVibrationResult(
                            result = current,
                            rmsChart = chartPoints,
                        ),
                    ),
                )
            },
            enabled = result != null && result?.hasSignificantVibration == true,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.tool_vibration_save))
        }
    }
}
