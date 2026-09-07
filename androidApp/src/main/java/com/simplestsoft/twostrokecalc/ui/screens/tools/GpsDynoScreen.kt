package com.simplestsoft.twostrokecalc.ui.screens.tools

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.simplestsoft.twostrokecalc.R
import com.simplestsoft.twostrokecalc.data.sensors.AndroidLocationSampleSource
import com.simplestsoft.twostrokecalc.domain.model.ToolId
import com.simplestsoft.twostrokecalc.domain.model.ToolMeasurementSession
import com.simplestsoft.twostrokecalc.domain.tools.dyno.DynoRunAnalyzer
import com.simplestsoft.twostrokecalc.domain.tools.dyno.DynoRunResult
import com.simplestsoft.twostrokecalc.domain.tools.dyno.DynoRunSample
import com.simplestsoft.twostrokecalc.domain.tools.dyno.DynoSpeedChartPoint
import com.simplestsoft.twostrokecalc.domain.tools.dyno.DynoVehicleContext
import com.simplestsoft.twostrokecalc.ui.components.AppColors
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorDecimalField
import com.simplestsoft.twostrokecalc.ui.components.calculator.filterDecimalInput
import com.simplestsoft.twostrokecalc.ui.components.calculator.parseDecimal
import java.util.Locale
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

private const val STANDSTILL_SPEED_KMH = 1.0
private const val MOVING_SPEED_KMH = 8.0
private const val STANDSTILL_AUTO_STOP_MS = 5_000L

@Composable
fun GpsDynoScreen(
    canSave: Boolean,
    onSaveSession: (ToolMeasurementSession) -> Unit,
    onRequestPro: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val locationSource = remember { AndroidLocationSampleSource(context.applicationContext) }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> hasPermission = granted }

    var disclaimerAccepted by remember { mutableStateOf(false) }
    var massText by remember { mutableStateOf("180") }
    var driverText by remember { mutableStateOf("75") }
    var countdown by remember { mutableIntStateOf(0) }
    var measuring by remember { mutableStateOf(false) }
    var liveSpeed by remember { mutableStateOf(0.0) }
    var livePower by remember { mutableStateOf(0.0) }
    var samples by remember { mutableStateOf(ArrayList<DynoRunSample>()) }
    var result by remember { mutableStateOf<DynoRunResult?>(null) }
    var speedChartPoints by remember { mutableStateOf<List<DynoSpeedChartPoint>>(emptyList()) }
    var autoStopped by remember { mutableStateOf(false) }
    var job by remember { mutableStateOf<Job?>(null) }

    fun vehicleContext(): DynoVehicleContext {
        val mass = parseDecimal(massText) ?: 180.0
        val driver = parseDecimal(driverText) ?: 75.0
        return DynoVehicleContext(massKg = mass, driverMassKg = driver)
    }

    DisposableEffect(Unit) {
        onDispose { job?.cancel() }
    }

    fun stopRun(fromStandstill: Boolean = false) {
        measuring = false
        job?.cancel()
        autoStopped = fromStandstill
        val snapshot = ArrayList(samples)
        speedChartPoints = DynoRunAnalyzer.speedChartPoints(snapshot)
        result = DynoRunAnalyzer.analyze(
            samples = snapshot,
            context = vehicleContext(),
        )
    }

    fun startRun() {
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            return
        }
        samples = ArrayList()
        result = null
        speedChartPoints = emptyList()
        autoStopped = false
        liveSpeed = 0.0
        livePower = 0.0
        countdown = 3
        job = scope.launch {
            var hasMoved = false
            var standstillStartMs = 0L
            while (countdown > 0) {
                delay(1000)
                countdown -= 1
            }
            measuring = true
            val dynoContext = vehicleContext()
            locationSource.start(dynoMode = true)
                .catch { stopRun() }
                .collect { sample ->
                    val dynoSample = DynoRunSample(
                        timestampMs = sample.timestampMs,
                        speedKmh = sample.speedKmh,
                        gpsAccuracyM = sample.accuracyM,
                    )
                    val previous = samples.lastOrNull()
                    samples.add(dynoSample)
                    liveSpeed = sample.speedKmh
                    if (previous != null) {
                        livePower = DynoRunAnalyzer.instantPowerPs(previous, dynoSample, dynoContext) ?: 0.0
                    }
                    if (dynoSample.speedKmh >= MOVING_SPEED_KMH) {
                        hasMoved = true
                        standstillStartMs = 0L
                    } else if (hasMoved && dynoSample.speedKmh < STANDSTILL_SPEED_KMH) {
                        if (standstillStartMs == 0L) {
                            standstillStartMs = dynoSample.timestampMs
                        } else if (dynoSample.timestampMs - standstillStartMs >= STANDSTILL_AUTO_STOP_MS) {
                            stopRun(fromStandstill = true)
                            return@collect
                        }
                    }
                    if (!canSave && samples.size > 40) {
                        stopRun()
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
        if (!disclaimerAccepted) {
            Text(stringResource(R.string.tool_dyno_disclaimer), color = AppColors.textSecondary())
            Button(onClick = { disclaimerAccepted = true }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.tool_dyno_accept))
            }
            return@Column
        }
        if (!hasPermission) {
            Text(stringResource(R.string.tool_dyno_permission), color = AppColors.textSecondary())
            Button(onClick = { permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }) {
                Text(stringResource(R.string.tool_rpm_grant))
            }
        }
        CalculatorDecimalField(
            value = massText,
            onValueChange = { massText = it.filterDecimalInput() },
            label = stringResource(R.string.tool_dyno_mass),
        )
        CalculatorDecimalField(
            value = driverText,
            onValueChange = { driverText = it.filterDecimalInput() },
            label = stringResource(R.string.tool_dyno_driver),
        )
        if (countdown > 0) {
            Text(
                text = countdown.toString(),
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 72.sp),
                fontWeight = FontWeight.Bold,
                color = AppColors.primaryBlue(),
            )
        } else if (measuring) {
            Text(
                text = String.format(Locale.getDefault(), "%.1f km/h", liveSpeed),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = AppColors.textPrimary(),
            )
            if (livePower > 0.0) {
                Text(
                    text = String.format(Locale.getDefault(), "%.1f PS", livePower),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.primaryBlue(),
                )
            }
            Button(onClick = { stopRun() }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.tool_dyno_stop))
            }
        } else {
            Button(onClick = { startRun() }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.tool_dyno_countdown))
            }
        }
        result?.let { r ->
            if (autoStopped) {
                Text(
                    text = stringResource(R.string.tool_dyno_auto_stopped),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.textSecondary(),
                )
            }
            if (speedChartPoints.size >= 2) {
                Text(
                    text = stringResource(R.string.tool_dyno_chart),
                    style = MaterialTheme.typography.titleSmall,
                    color = AppColors.textPrimary(),
                )
                DynoSpeedChart(
                    points = speedChartPoints,
                    timeAxisLabel = stringResource(R.string.tool_rpm_chart_time_axis),
                    speedAxisLabel = stringResource(R.string.tool_dyno_chart_speed_axis),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                )
            }
            if (r.powerCurve.size >= 2) {
                Text(
                    text = stringResource(R.string.tool_dyno_power_chart),
                    style = MaterialTheme.typography.titleSmall,
                    color = AppColors.textPrimary(),
                )
                DynoPowerChart(
                    points = r.powerCurve,
                    speedAxisLabel = stringResource(R.string.tool_dyno_chart_speed_axis),
                    powerAxisLabel = stringResource(R.string.tool_dyno_chart_power_axis),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                )
            }
            Text(
                "${stringResource(R.string.tool_dyno_peak_power)}: ${String.format(Locale.getDefault(), "%.1f PS", r.peakPowerPs)}",
                color = AppColors.textPrimary(),
            )
            Text(
                "${stringResource(R.string.tool_dyno_max_speed)}: ${String.format(Locale.getDefault(), "%.1f km/h", r.maxSpeedKmh)}",
                color = AppColors.textPrimary(),
            )
            r.time0to60Ms?.let { t60 ->
                Text(
                    "0–60: ${String.format(Locale.getDefault(), "%.1f s", t60 / 1000.0)}",
                    color = AppColors.textSecondary(),
                )
            }
            r.time0to100Ms?.let { t100 ->
                Text(
                    "0–100: ${String.format(Locale.getDefault(), "%.1f s", t100 / 1000.0)}",
                    color = AppColors.textSecondary(),
                )
            }
            Text(
                "${stringResource(R.string.tool_dyno_confidence)}: ${(r.confidence * 100).toInt()}%",
                color = AppColors.textSecondary(),
            )
            OutlinedButton(
                onClick = {
                    if (!canSave) {
                        onRequestPro()
                        return@OutlinedButton
                    }
                    onSaveSession(
                        ToolMeasurementSession(
                            toolId = ToolId.GPS_DYNO,
                            title = "${context.getString(R.string.tool_tab_gps_dyno)} " +
                                "${String.format(Locale.getDefault(), "%.1f", r.peakPowerPs)} PS",
                            resultJson = ToolSessionChartJson.encodeDynoResult(
                                peakPs = r.peakPowerPs,
                                maxKmh = r.maxSpeedKmh,
                                confidence = r.confidence,
                                speedChart = speedChartPoints,
                                powerCurve = r.powerCurve,
                            ),
                        ),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.tool_dyno_save))
            }
        }
    }
}
