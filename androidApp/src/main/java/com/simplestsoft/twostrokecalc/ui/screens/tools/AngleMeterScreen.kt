package com.simplestsoft.twostrokecalc.ui.screens.tools

import android.content.pm.ActivityInfo
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simplestsoft.twostrokecalc.R
import com.simplestsoft.twostrokecalc.data.sensors.AndroidAngleSampleSource
import com.simplestsoft.twostrokecalc.domain.model.ToolId
import com.simplestsoft.twostrokecalc.domain.model.ToolMeasurementSession
import com.simplestsoft.twostrokecalc.ui.components.AppColors
import com.simplestsoft.twostrokecalc.ui.util.findComponentActivity
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

@Composable
fun AngleMeterScreen(
    canSave: Boolean,
    onSaveSession: (ToolMeasurementSession) -> Unit,
    onRequestPro: () -> Unit,
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findComponentActivity() }
    val scope = rememberCoroutineScope()
    val source = remember { AndroidAngleSampleSource(context.applicationContext) }
    var smoothedAngle by remember { mutableFloatStateOf(0f) }
    var zeroOffset by remember { mutableFloatStateOf(0f) }
    var reliability by remember { mutableFloatStateOf(0f) }
    var hasSample by remember { mutableStateOf(false) }
    var sensorAvailable by remember { mutableStateOf(true) }
    var isHeld by remember { mutableStateOf(false) }
    var heldAngle by remember { mutableFloatStateOf(0f) }
    var sensorJob by remember { mutableStateOf<Job?>(null) }

    DisposableEffect(activity) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        onDispose {
            if (activity?.isChangingConfigurations == false) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
    }

    DisposableEffect(Unit) {
        sensorJob = scope.launch {
            source.start()
                .catch { sensorAvailable = false }
                .collect { sample ->
                    reliability = sample.reliability
                    smoothedAngle = if (!hasSample) {
                        sample.degrees
                    } else {
                        smoothCircularDegrees(smoothedAngle, sample.degrees)
                    }
                    hasSample = true
                }
        }
        onDispose { sensorJob?.cancel() }
    }

    val liveAngle = normalizeDegrees(smoothedAngle - zeroOffset)
    val displayedAngle = if (isHeld) heldAngle else liveAngle
    val readableTextRotation = -signedDegrees(smoothedAngle)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.tool_angle_hint),
            color = AppColors.textSecondary(),
            modifier = Modifier.fillMaxWidth(),
        )

        AngleDial(
            angle = displayedAngle,
            valueText = String.format(Locale.getDefault(), "%.1f°", displayedAngle),
            valueRotation = readableTextRotation,
            modifier = Modifier.size(300.dp),
        )

        when {
            !sensorAvailable -> Text(
                text = stringResource(R.string.tool_angle_sensor_unavailable),
                color = MaterialTheme.colorScheme.error,
            )
            reliability < 0.25f -> Text(
                text = stringResource(R.string.tool_angle_flat_warning),
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
            else -> Text(
                text = stringResource(R.string.tool_angle_sensor_ready),
                color = AppColors.textSecondary(),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = {
                    if (isHeld) {
                        isHeld = false
                    } else {
                        heldAngle = liveAngle
                        isHeld = true
                    }
                },
                enabled = hasSample,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(if (isHeld) R.string.tool_angle_release else R.string.tool_angle_hold))
            }
            Button(
                onClick = {
                    zeroOffset = smoothedAngle
                    isHeld = false
                },
                enabled = hasSample,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.tool_angle_zero))
            }
        }

        OutlinedButton(
            onClick = {
                zeroOffset = 0f
                isHeld = false
            },
            enabled = hasSample,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.tool_angle_reset))
        }

        OutlinedButton(
            onClick = {
                if (!canSave) {
                    onRequestPro()
                    return@OutlinedButton
                }
                onSaveSession(
                    ToolMeasurementSession(
                        toolId = ToolId.ANGLE_METER,
                        title = "${context.getString(R.string.tool_tab_angle)} " +
                            String.format(Locale.getDefault(), "%.1f°", displayedAngle),
                        resultJson = "{\"degrees\":${displayedAngle}}",
                    ),
                )
            },
            enabled = hasSample && sensorAvailable,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.tool_angle_save))
        }
    }
}

@Composable
private fun AngleDial(
    angle: Float,
    valueText: String,
    valueRotation: Float,
    modifier: Modifier = Modifier,
) {
    val primary = AppColors.primaryBlue()
    val tickColor = AppColors.textSecondary()
    val surfaceColor = AppColors.surface()
    val innerColor = AppColors.background()
    val textColor = AppColors.textPrimary()

    Canvas(modifier = modifier) {
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        val innerRadius = radius * 0.34f
        drawCircle(color = surfaceColor, radius = radius)
        drawCircle(color = tickColor.copy(alpha = 0.5f), radius = radius - 2f, style = Stroke(width = 3f))

        for (degrees in 0 until 360 step 10) {
            val radians = Math.toRadians((degrees - 90).toDouble())
            val isMajor = degrees % 30 == 0
            val outerRadius = radius - 10f
            val innerRadius = radius - if (isMajor) 34f else 22f
            drawLine(
                color = tickColor,
                start = Offset(
                    center.x + cos(radians).toFloat() * innerRadius,
                    center.y + sin(radians).toFloat() * innerRadius,
                ),
                end = Offset(
                    center.x + cos(radians).toFloat() * outerRadius,
                    center.y + sin(radians).toFloat() * outerRadius,
                ),
                strokeWidth = if (isMajor) 4f else 2f,
                cap = StrokeCap.Round,
            )
        }

        val labelPaint = android.graphics.Paint().apply {
            isAntiAlias = true
            color = tickColor.toArgb()
            textAlign = android.graphics.Paint.Align.CENTER
            textSize = 10.sp.toPx()
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        }
        drawContext.canvas.nativeCanvas.apply {
            for (degrees in 0 until 360 step 30) {
                val radians = Math.toRadians((degrees - 90).toDouble())
                val labelRadius = radius - 48f
                val label = if (degrees == 0) "0/360" else degrees.toString()
                val x = center.x + cos(radians).toFloat() * labelRadius
                val y = center.y + sin(radians).toFloat() * labelRadius -
                    (labelPaint.ascent() + labelPaint.descent()) / 2f
                drawText(label, x, y, labelPaint)
            }
        }

        val needleRadians = Math.toRadians((angle - 90f).toDouble())
        drawLine(
            color = primary,
            start = Offset(
                center.x + cos(needleRadians).toFloat() * (innerRadius + 8f),
                center.y + sin(needleRadians).toFloat() * (innerRadius + 8f),
            ),
            end = Offset(
                center.x + cos(needleRadians).toFloat() * (radius - 68f),
                center.y + sin(needleRadians).toFloat() * (radius - 68f),
            ),
            strokeWidth = 8f,
            cap = StrokeCap.Round,
        )

        drawCircle(color = innerColor, radius = innerRadius, center = center)
        drawCircle(
            color = primary,
            radius = innerRadius,
            center = center,
            style = Stroke(width = 5f),
        )

        val valuePaint = android.graphics.Paint().apply {
            isAntiAlias = true
            color = textColor.toArgb()
            textAlign = android.graphics.Paint.Align.CENTER
            textSize = 30.sp.toPx()
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        }
        drawContext.canvas.nativeCanvas.apply {
            save()
            rotate(valueRotation, center.x, center.y)
            drawText(
                valueText,
                center.x,
                center.y - (valuePaint.ascent() + valuePaint.descent()) / 2f,
                valuePaint,
            )
            restore()
        }
    }
}

private fun smoothCircularDegrees(current: Float, next: Float): Float {
    val shortestDelta = (next - current + 540f) % 360f - 180f
    return normalizeDegrees(current + shortestDelta * 0.18f)
}

private fun normalizeDegrees(value: Float): Float = (value % 360f + 360f) % 360f

private fun signedDegrees(value: Float): Float = (value + 540f) % 360f - 180f
