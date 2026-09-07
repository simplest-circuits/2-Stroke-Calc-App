package com.simplestsoft.twostrokecalc.ui.screens.tools

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simplestsoft.twostrokecalc.domain.tools.dyno.DynoPowerPoint
import com.simplestsoft.twostrokecalc.domain.tools.dyno.DynoSpeedChartPoint
import com.simplestsoft.twostrokecalc.domain.tools.vibration.VibrationAnalysisResult
import com.simplestsoft.twostrokecalc.ui.components.AppColors
import java.util.Locale

@Composable
fun DynoSpeedChart(
    points: List<DynoSpeedChartPoint>,
    timeAxisLabel: String,
    speedAxisLabel: String,
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
        val minSpeed = (points.minOf { it.speedKmh } * 0.9).coerceAtLeast(0.0)
        val maxSpeed = (points.maxOf { it.speedKmh } * 1.1).coerceAtLeast(minSpeed + 10.0)
        val speedRange = maxSpeed - minSpeed
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
            val y = plotHeight - ((point.speedKmh - minSpeed) / speedRange * plotHeight).toFloat()
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 3f, cap = StrokeCap.Round),
        )
        drawContext.canvas.nativeCanvas.apply {
            drawText(speedAxisLabel, 0f, 11.dp.toPx(), labelPaint)
            drawText(minSpeed.toInt().toString(), 0f, plotHeight, labelPaint)
            drawText(maxSpeed.toInt().toString(), 0f, 11.dp.toPx() + labelPaint.textSize, labelPaint)
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
fun DynoPowerChart(
    points: List<DynoPowerPoint>,
    speedAxisLabel: String,
    powerAxisLabel: String,
    modifier: Modifier = Modifier,
) {
    val lineColor = AppColors.primaryBlue()
    val gridColor = AppColors.textSecondary().copy(alpha = 0.22f)
    val labelColor = AppColors.textSecondary()
    Canvas(modifier = modifier) {
        if (points.size < 2) return@Canvas
        val minSpeed = points.minOf { it.speedKmh }.coerceAtLeast(0.0)
        val maxSpeed = (points.maxOf { it.speedKmh } * 1.05).coerceAtLeast(minSpeed + 10.0)
        val speedRange = maxSpeed - minSpeed
        val minPower = 0.0
        val maxPower = (points.maxOf { it.powerPs } * 1.1).coerceAtLeast(10.0)
        val powerRange = maxPower - minPower
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
            val x = leftPadding + ((point.speedKmh - minSpeed) / speedRange * plotWidth).toFloat()
            val y = plotHeight - ((point.powerPs - minPower) / powerRange * plotHeight).toFloat()
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 3f, cap = StrokeCap.Round),
        )
        drawContext.canvas.nativeCanvas.apply {
            drawText(powerAxisLabel, 0f, 11.dp.toPx(), labelPaint)
            drawText("0", 0f, plotHeight, labelPaint)
            drawText(maxPower.toInt().toString(), 0f, 11.dp.toPx() + labelPaint.textSize, labelPaint)
            drawText(minSpeed.toInt().toString(), leftPadding, size.height - 4.dp.toPx(), labelPaint)
            val maxSpeedLabel = maxSpeed.toInt().toString()
            val maxSpeedWidth = labelPaint.measureText(maxSpeedLabel)
            drawText(maxSpeedLabel, size.width - maxSpeedWidth, size.height - 4.dp.toPx(), labelPaint)
            val axisWidth = labelPaint.measureText(speedAxisLabel)
            drawText(
                speedAxisLabel,
                leftPadding + (plotWidth - axisWidth) / 2f,
                size.height - 4.dp.toPx(),
                labelPaint,
            )
        }
    }
}

@Composable
fun VibrationRmsChart(
    points: List<VibrationRmsChartPoint>,
    timeAxisLabel: String,
    valueAxisLabel: String,
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
        val minRms = (points.minOf { it.rms } * 0.9).coerceAtLeast(0.0)
        val maxRms = (points.maxOf { it.rms } * 1.1).coerceAtLeast(minRms + 0.01)
        val rmsRange = maxRms - minRms
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
            val y = plotHeight - ((point.rms - minRms) / rmsRange * plotHeight).toFloat()
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 3f, cap = StrokeCap.Round),
        )
        drawContext.canvas.nativeCanvas.apply {
            drawText(valueAxisLabel, 0f, 11.dp.toPx(), labelPaint)
            drawText(
                String.format(Locale.getDefault(), "%.2f", minRms),
                0f,
                plotHeight,
                labelPaint,
            )
            drawText(
                String.format(Locale.getDefault(), "%.2f", maxRms),
                0f,
                11.dp.toPx() + labelPaint.textSize,
                labelPaint,
            )
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
fun VibrationSpectrumChart(
    primary: VibrationAnalysisResult,
    secondary: VibrationAnalysisResult? = null,
    modifier: Modifier = Modifier,
) {
    val primaryColor = AppColors.primaryBlue()
    val secondaryColor = AppColors.textSecondary()
    Canvas(modifier = modifier) {
        fun drawSpectrum(result: VibrationAnalysisResult, color: Color) {
            if (result.spectrum.size < 2) return
            val maxMag = result.spectrum.maxOf { it.magnitude }.coerceAtLeast(1e-6)
            val path = Path()
            result.spectrum.forEachIndexed { index, bin ->
                val x = size.width * index / (result.spectrum.size - 1)
                val y = size.height - (bin.magnitude / maxMag * size.height).toFloat()
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, color = color, style = Stroke(width = 3f, cap = StrokeCap.Round))
        }
        secondary?.let { drawSpectrum(it, secondaryColor) }
        drawSpectrum(primary, primaryColor)
        drawLine(
            color = secondaryColor.copy(alpha = 0.3f),
            start = Offset(0f, size.height),
            end = Offset(size.width, size.height),
            strokeWidth = 2f,
        )
    }
}

@Composable
fun ToolMeasurementStatChip(
    label: String,
    value: Double,
    suffix: String = "",
) {
    val display = if (value.isInfinite() || value.isNaN()) {
        "–"
    } else {
        String.format(Locale.getDefault(), "%.2f", value) + suffix
    }
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = AppColors.textSecondary())
        Text(display, style = MaterialTheme.typography.titleMedium, color = AppColors.textPrimary())
    }
}
