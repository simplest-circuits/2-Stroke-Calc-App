package com.simplestsoft.twostrokecalc.ui.porttiming

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.simplestsoft.twostrokecalc.R
import com.simplestsoft.twostrokecalc.domain.porttiming.IntakeSystem
import com.simplestsoft.twostrokecalc.domain.porttiming.PortTimingInput
import com.simplestsoft.twostrokecalc.domain.porttiming.PortTimingResult
import com.simplestsoft.twostrokecalc.ui.components.AppColors
import com.simplestsoft.twostrokecalc.ui.theme.ContainerCornerRadius
import java.util.Locale
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

private const val TDC_ANGLE = 270f
private const val BDC_ANGLE = 90f

private const val TRANSFER_INNER = 0.36f
private const val TRANSFER_OUTER = 0.52f
private const val EXHAUST_INNER = 0.58f
private const val EXHAUST_OUTER = 0.74f
private const val INTAKE_INNER = 0.78f
private const val INTAKE_OUTER = 0.98f

private data class DiagramPalette(
    val canvasBackground: Color,
    val ring: Color,
    val ringInner: Color,
    val crosshair: Color,
    val markerText: Color,
    val trackBackground: Color,
    val exhaustFill: Color,
    val exhaustStroke: Color,
    val transferFill: Color,
    val transferStroke: Color,
    val intakeFill: Color,
    val intakeStroke: Color,
    val overlapFill: Color,
    val overlapStroke: Color,
    val blowdownFill: Color,
    val blowdownStroke: Color,
)

private data class DiagramLayout(
    val cx: Float,
    val cy: Float,
    val maxRadius: Float,
)

private data class AngleSpan(val start: Float, val end: Float)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DiagramLegend(
    palette: DiagramPalette,
    showIntake: Boolean,
    showOverlap: Boolean,
    showBlowdown: Boolean,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        DiagramLegendItem(color = palette.exhaustStroke, label = stringResource(R.string.pt_legend_exhaust))
        DiagramLegendItem(color = palette.transferStroke, label = stringResource(R.string.pt_legend_transfer))
        if (showIntake) {
            DiagramLegendItem(color = palette.intakeStroke, label = stringResource(R.string.pt_legend_intake))
        }
        if (showBlowdown) {
            DiagramLegendItem(color = palette.blowdownStroke, label = stringResource(R.string.pt_legend_blowdown))
        }
        if (showOverlap) {
            DiagramLegendOverlapItem(color = palette.overlapStroke)
        }
    }
}

@Composable
private fun DiagramLegendOverlapItem(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(modifier = Modifier.size(10.dp)) {
            drawLine(color, Offset(0f, size.height / 2f), Offset(size.width, size.height / 2f), strokeWidth = 2.5f)
            val markerStroke = Stroke(width = 2.5f)
            drawCircle(color, radius = size.minDimension * 0.22f, center = Offset(size.width * 0.28f, size.height / 2f), style = markerStroke)
            drawCircle(color, radius = size.minDimension * 0.22f, center = Offset(size.width * 0.72f, size.height / 2f), style = markerStroke)
        }
        Spacer(Modifier.width(6.dp))
        Text(
            text = stringResource(R.string.pt_legend_overlap),
            style = MaterialTheme.typography.labelMedium,
            color = AppColors.textSecondary(),
        )
    }
}

@Composable
private fun DiagramLegendItem(
    color: Color,
    label: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, CircleShape),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = AppColors.textSecondary(),
        )
    }
}

@Composable
private fun rememberDiagramPalette(): DiagramPalette {
    val scheme = MaterialTheme.colorScheme
    val isDark = AppColors.isDarkTheme()
    val canvasBackground = AppColors.accentSurfaceBackground()
    return remember(isDark, canvasBackground, scheme) {
        val overlapStroke = if (isDark) Color(0xFFFF7043) else Color(0xFFE64A19)
        DiagramPalette(
            canvasBackground = canvasBackground,
            ring = scheme.outline.copy(alpha = if (isDark) 0.7f else 0.55f),
            ringInner = scheme.outline.copy(alpha = if (isDark) 0.35f else 0.25f),
            crosshair = scheme.outline.copy(alpha = if (isDark) 0.55f else 0.4f),
            markerText = scheme.onSurfaceVariant,
            trackBackground = scheme.onSurface.copy(alpha = if (isDark) 0.06f else 0.04f),
            exhaustFill = scheme.onSurfaceVariant.copy(alpha = if (isDark) 0.55f else 0.42f),
            exhaustStroke = scheme.onSurfaceVariant,
            transferFill = scheme.primary.copy(alpha = if (isDark) 0.42f else 0.32f),
            transferStroke = scheme.primary,
            intakeFill = scheme.secondary.copy(alpha = if (isDark) 0.38f else 0.28f),
            intakeStroke = scheme.secondary,
            overlapFill = overlapStroke.copy(alpha = if (isDark) 0.9f else 0.82f),
            overlapStroke = overlapStroke,
            blowdownFill = scheme.onSurfaceVariant.copy(alpha = if (isDark) 0.72f else 0.58f),
            blowdownStroke = scheme.onSurface.copy(alpha = if (isDark) 0.85f else 0.7f),
        )
    }
}

@Composable
fun PortTimingDiagram(
    input: PortTimingInput,
    result: PortTimingResult,
    modifier: Modifier = Modifier,
    locale: Locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault(),
) {
    val palette = rememberDiagramPalette()
    val shape = RoundedCornerShape(ContainerCornerRadius)
    val overlapSpans = remember(result, input) { computeOverlapSpans(result, input) }
    val blowdownSpans = remember(result) { computeBlowdownSpans(result) }
    val showIntake = input.intakeSystem == IntakeSystem.ROTARY_VALVE && result.intake != null
    val otLabel = stringResource(R.string.port_area_exhaust_diagram_ot)
    val utLabel = stringResource(R.string.port_area_exhaust_diagram_ut)

    Column(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(shape)
                .background(palette.canvasBackground)
                .then(AppColors.accentSurfaceModifier(shape))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = otLabel,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = palette.markerText,
                textAlign = TextAlign.Center,
            )
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                val layout = computeDiagramLayout(size.width, size.height)
                val cx = layout.cx
                val cy = layout.cy
                val maxRadius = layout.maxRadius

                drawRingTracks(cx, cy, maxRadius, palette, showIntake)
                drawCircleGuide(cx, cy, maxRadius, palette, showIntake)
                drawCrosshairs(cx, cy, maxRadius, palette.crosshair)
                drawAngleTicks(cx, cy, maxRadius, palette.crosshair)
                drawTdcBdcMarkers(cx, cy, maxRadius, palette.markerText)

                result.transfer?.let { transfer ->
                    drawRingSector(
                        cx = cx,
                        cy = cy,
                        innerRadius = maxRadius * TRANSFER_INNER,
                        outerRadius = maxRadius * TRANSFER_OUTER,
                        startAngle = BDC_ANGLE - transfer.openBeforeBdc.toFloat(),
                        sweepAngle = transfer.duration.toFloat(),
                        fillColor = palette.transferFill,
                        strokeColor = palette.transferStroke,
                        strokeWidth = 2.5f,
                    )
                }

                result.exhaust?.let { exhaust ->
                    drawRingSector(
                        cx = cx,
                        cy = cy,
                        innerRadius = maxRadius * EXHAUST_INNER,
                        outerRadius = maxRadius * EXHAUST_OUTER,
                        startAngle = BDC_ANGLE - exhaust.openBeforeBdc.toFloat(),
                        sweepAngle = exhaust.duration.toFloat(),
                        fillColor = palette.exhaustFill,
                        strokeColor = palette.exhaustStroke,
                        strokeWidth = 2.5f,
                    )
                }

                if (showIntake) {
                    result.intake?.let { intake ->
                        drawRingSector(
                            cx = cx,
                            cy = cy,
                            innerRadius = maxRadius * INTAKE_INNER,
                            outerRadius = maxRadius * INTAKE_OUTER,
                            startAngle = TDC_ANGLE - intake.openBeforeTdc.toFloat(),
                            sweepAngle = intake.duration.toFloat(),
                            fillColor = palette.intakeFill,
                            strokeColor = palette.intakeStroke,
                            strokeWidth = 2.5f,
                        )
                    }
                }

                blowdownSpans.forEach { blowdown ->
                    drawRingSector(
                        cx = cx,
                        cy = cy,
                        innerRadius = maxRadius * blowdown.innerRadiusFactor,
                        outerRadius = maxRadius * blowdown.outerRadiusFactor,
                        startAngle = blowdown.startAngle,
                        sweepAngle = blowdown.sweepAngle,
                        fillColor = palette.blowdownFill,
                        strokeColor = palette.blowdownStroke,
                        strokeWidth = 2.5f,
                        strokePathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)),
                    )
                }

                overlapSpans.forEach { overlap ->
                    drawRingSector(
                        cx = cx,
                        cy = cy,
                        innerRadius = maxRadius * overlap.innerRadiusFactor,
                        outerRadius = maxRadius * overlap.outerRadiusFactor,
                        startAngle = overlap.startAngle,
                        sweepAngle = overlap.sweepAngle,
                        fillColor = palette.overlapFill,
                        strokeColor = palette.overlapStroke,
                        strokeWidth = 3f,
                    )
                    drawOverlapBoundaryMarkers(
                        cx = cx,
                        cy = cy,
                        innerRadius = maxRadius * overlap.markerInnerFactor,
                        outerRadius = maxRadius * overlap.markerOuterFactor,
                        startAngle = overlap.startAngle,
                        endAngle = overlap.startAngle + overlap.sweepAngle,
                        color = palette.overlapStroke,
                    )
                }

                drawRingChannelLabels(
                    cx = cx,
                    cy = cy,
                    maxRadius = maxRadius,
                    palette = palette,
                    showIntake = showIntake,
                )
            }
            Text(
                text = utLabel,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = palette.markerText,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(Modifier.height(10.dp))
        DiagramLegend(
            palette = palette,
            showIntake = showIntake,
            showOverlap = overlapSpans.isNotEmpty(),
            showBlowdown = blowdownSpans.isNotEmpty(),
        )
    }
}

private data class OverlapSpan(
    val startAngle: Float,
    val sweepAngle: Float,
    val innerRadiusFactor: Float,
    val outerRadiusFactor: Float,
    val markerInnerFactor: Float,
    val markerOuterFactor: Float,
)

private data class BlowdownSpan(
    val startAngle: Float,
    val sweepAngle: Float,
    val innerRadiusFactor: Float,
    val outerRadiusFactor: Float,
)

private fun computeBlowdownSpans(result: PortTimingResult): List<BlowdownSpan> {
    val exhaust = result.exhaust ?: return emptyList()
    val transfer = result.transfer ?: return emptyList()
    val blowdown = result.blowdown ?: return emptyList()
    if (blowdown <= 0.05) return emptyList()

    val exhaustHalf = exhaust.openBeforeBdc.toFloat()
    val transferHalf = transfer.openBeforeBdc.toFloat()
    return listOf(
        angleInterval(BDC_ANGLE - exhaustHalf, BDC_ANGLE - transferHalf),
        angleInterval(BDC_ANGLE + transferHalf, BDC_ANGLE + exhaustHalf),
    ).map { (startAngle, sweepAngle) ->
        BlowdownSpan(
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            innerRadiusFactor = EXHAUST_INNER,
            outerRadiusFactor = EXHAUST_OUTER,
        )
    }
}

private fun computeOverlapSpans(
    result: PortTimingResult,
    input: PortTimingInput,
): List<OverlapSpan> {
    val overlaps = mutableListOf<OverlapSpan>()

    val exhaust = result.exhaust
    val transfer = result.transfer
    if (exhaust != null && transfer != null) {
        val exhaustSpan = symmetricBdcSpan(exhaust.openBeforeBdc)
        val transferSpan = symmetricBdcSpan(transfer.openBeforeBdc)
        intersectSpans(exhaustSpan, transferSpan).forEach { interval ->
            overlaps.add(
                overlapBridgeSpan(
                    interval = interval,
                    bridgeInner = TRANSFER_OUTER,
                    bridgeOuter = EXHAUST_INNER,
                    markerInner = TRANSFER_INNER,
                    markerOuter = EXHAUST_OUTER,
                ),
            )
        }
    }

    val intake = result.intake
    if (input.intakeSystem == IntakeSystem.ROTARY_VALVE && exhaust != null && intake != null) {
        val exhaustSpan = symmetricBdcSpan(exhaust.openBeforeBdc)
        val intakeSpan = intakeSpan(intake.openBeforeTdc, intake.closeAfterTdc)
        intersectSpans(exhaustSpan, intakeSpan).forEach { interval ->
            overlaps.add(
                overlapBridgeSpan(
                    interval = interval,
                    bridgeInner = EXHAUST_OUTER,
                    bridgeOuter = INTAKE_INNER,
                    markerInner = EXHAUST_INNER,
                    markerOuter = INTAKE_OUTER,
                ),
            )
        }
    }

    return overlaps
}

private fun overlapBridgeSpan(
    interval: Pair<Float, Float>,
    bridgeInner: Float,
    bridgeOuter: Float,
    markerInner: Float,
    markerOuter: Float,
): OverlapSpan {
    return OverlapSpan(
        startAngle = interval.first,
        sweepAngle = interval.second - interval.first,
        innerRadiusFactor = bridgeInner,
        outerRadiusFactor = bridgeOuter,
        markerInnerFactor = markerInner,
        markerOuterFactor = markerOuter,
    )
}

private fun angleInterval(startAngle: Float, endAngle: Float): Pair<Float, Float> {
    val start = normalizeAngle(startAngle)
    val end = normalizeAngle(endAngle)
    return if (start <= end) {
        start to (end - start)
    } else {
        start to (360f - start + end)
    }
}

private fun symmetricBdcSpan(openBeforeBdc: Double): AngleSpan {
    val half = openBeforeBdc.toFloat()
    return AngleSpan(
        start = normalizeAngle(BDC_ANGLE - half),
        end = normalizeAngle(BDC_ANGLE + half),
    )
}

private fun intakeSpan(openBeforeTdc: Double, closeAfterTdc: Double): AngleSpan {
    return AngleSpan(
        start = normalizeAngle(TDC_ANGLE - openBeforeTdc.toFloat()),
        end = normalizeAngle(TDC_ANGLE + closeAfterTdc.toFloat()),
    )
}

private fun normalizeAngle(angle: Float): Float = ((angle % 360f) + 360f) % 360f

private fun spanToIntervals(span: AngleSpan): List<Pair<Float, Float>> {
    val start = normalizeAngle(span.start)
    val end = normalizeAngle(span.end)
    return if (start <= end) {
        listOf(start to end)
    } else {
        listOf(start to 360f, 0f to end)
    }
}

private fun intersectSpans(first: AngleSpan, second: AngleSpan): List<Pair<Float, Float>> {
    val intervals = mutableListOf<Pair<Float, Float>>()
    for ((aStart, aEnd) in spanToIntervals(first)) {
        for ((bStart, bEnd) in spanToIntervals(second)) {
            val overlapStart = max(aStart, bStart)
            val overlapEnd = min(aEnd, bEnd)
            if (overlapEnd - overlapStart > 0.05f) {
                intervals.add(overlapStart to overlapEnd)
            }
        }
    }
    return intervals
}

private fun computeDiagramLayout(width: Float, height: Float): DiagramLayout {
    val inset = min(width, height) * 0.035f
    val maxRadius = min(width - inset * 2f, height - inset * 2f) / 2f
    return DiagramLayout(
        cx = width / 2f,
        cy = height / 2f,
        maxRadius = maxRadius,
    )
}

private fun DrawScope.drawRingTracks(
    cx: Float,
    cy: Float,
    maxRadius: Float,
    palette: DiagramPalette,
    showIntake: Boolean,
) {
    val tracks = buildList {
        add(TRANSFER_INNER to TRANSFER_OUTER)
        add(EXHAUST_INNER to EXHAUST_OUTER)
        if (showIntake) {
            add(INTAKE_INNER to INTAKE_OUTER)
        }
    }
    tracks.forEach { (innerFactor, outerFactor) ->
        drawRingSector(
            cx = cx,
            cy = cy,
            innerRadius = maxRadius * innerFactor,
            outerRadius = maxRadius * outerFactor,
            startAngle = 0f,
            sweepAngle = 360f,
            fillColor = palette.trackBackground,
            strokeColor = Color.Transparent,
            strokeWidth = 0f,
        )
    }
}

private fun DrawScope.drawAngleTicks(
    cx: Float,
    cy: Float,
    radius: Float,
    color: Color,
) {
    for (angle in 0 until 360 step 30) {
        val isMajor = angle % 90 == 0
        val radians = Math.toRadians(angle.toDouble())
        val cosValue = cos(radians).toFloat()
        val sinValue = sin(radians).toFloat()
        val innerFactor = if (isMajor) 0.965f else 0.985f
        val outerFactor = if (isMajor) 1.035f else 1.012f
        drawLine(
            color = color,
            start = Offset(cx + radius * innerFactor * cosValue, cy + radius * innerFactor * sinValue),
            end = Offset(cx + radius * outerFactor * cosValue, cy + radius * outerFactor * sinValue),
            strokeWidth = if (isMajor) 1.75f else 1f,
        )
    }
}

private fun DrawScope.drawTdcBdcMarkers(
    cx: Float,
    cy: Float,
    radius: Float,
    color: Color,
) {
    val markerLength = radius * 0.08f
    drawLine(
        color = color.copy(alpha = 0.85f),
        start = Offset(cx, cy - radius - markerLength),
        end = Offset(cx, cy - radius + markerLength * 0.35f),
        strokeWidth = 2.5f,
    )
    drawLine(
        color = color.copy(alpha = 0.85f),
        start = Offset(cx, cy + radius - markerLength * 0.35f),
        end = Offset(cx, cy + radius + markerLength),
        strokeWidth = 2.5f,
    )
}

private fun DrawScope.drawRingChannelLabels(
    cx: Float,
    cy: Float,
    maxRadius: Float,
    palette: DiagramPalette,
    showIntake: Boolean,
) {
    val labelAngle = 200f
    val radians = Math.toRadians(labelAngle.toDouble())
    val cosValue = cos(radians).toFloat()
    val sinValue = sin(radians).toFloat()
    val markers = buildList {
        add(Triple((TRANSFER_INNER + TRANSFER_OUTER) / 2f, palette.transferStroke, 3.5f))
        add(Triple((EXHAUST_INNER + EXHAUST_OUTER) / 2f, palette.exhaustStroke, 3.5f))
        if (showIntake) {
            add(Triple((INTAKE_INNER + INTAKE_OUTER) / 2f, palette.intakeStroke, 3.5f))
        }
    }
    markers.forEach { (midFactor, color, strokeWidth) ->
        val midRadius = maxRadius * midFactor
        val tickHalf = maxRadius * 0.035f
        drawLine(
            color = color,
            start = Offset(
                cx + (midRadius - tickHalf) * cosValue,
                cy + (midRadius - tickHalf) * sinValue,
            ),
            end = Offset(
                cx + (midRadius + tickHalf) * cosValue,
                cy + (midRadius + tickHalf) * sinValue,
            ),
            strokeWidth = strokeWidth,
        )
    }
}

private fun DrawScope.drawCircleGuide(
    cx: Float,
    cy: Float,
    radius: Float,
    palette: DiagramPalette,
    showIntake: Boolean,
) {
    val ringFactors = buildList {
        add(TRANSFER_INNER)
        add(TRANSFER_OUTER)
        add(EXHAUST_INNER)
        add(EXHAUST_OUTER)
        if (showIntake) {
            add(INTAKE_INNER)
            add(INTAKE_OUTER)
        }
    }
    ringFactors.forEach { factor ->
        drawCircle(
            color = palette.ringInner,
            radius = radius * factor,
            center = Offset(cx, cy),
            style = Stroke(width = 1f),
        )
    }
    drawCircle(
        color = palette.ring,
        radius = radius,
        center = Offset(cx, cy),
        style = Stroke(width = 1.5f),
    )
}

private fun DrawScope.drawCrosshairs(cx: Float, cy: Float, radius: Float, color: Color) {
    drawLine(color, Offset(cx, cy - radius), Offset(cx, cy + radius), strokeWidth = 1f)
    drawLine(color, Offset(cx - radius, cy), Offset(cx + radius, cy), strokeWidth = 1f)
}

private fun DrawScope.drawRingSector(
    cx: Float,
    cy: Float,
    innerRadius: Float,
    outerRadius: Float,
    startAngle: Float,
    sweepAngle: Float,
    fillColor: Color,
    strokeColor: Color,
    strokeWidth: Float = 2f,
    strokePathEffect: PathEffect? = null,
) {
    if (sweepAngle <= 0f) return

    val path = ringSectorPath(
        cx = cx,
        cy = cy,
        innerRadius = innerRadius,
        outerRadius = outerRadius,
        startAngle = startAngle,
        sweepAngle = sweepAngle,
    )

    drawPath(path, fillColor)
    if (strokeWidth > 0f && strokeColor.alpha > 0f) {
        drawPath(
            path,
            strokeColor,
            style = Stroke(width = strokeWidth, pathEffect = strokePathEffect),
        )
    }
}

private fun ringSectorPath(
    cx: Float,
    cy: Float,
    innerRadius: Float,
    outerRadius: Float,
    startAngle: Float,
    sweepAngle: Float,
): Path {
    val outerRect = Rect(
        left = cx - outerRadius,
        top = cy - outerRadius,
        right = cx + outerRadius,
        bottom = cy + outerRadius,
    )
    val innerRect = Rect(
        left = cx - innerRadius,
        top = cy - innerRadius,
        right = cx + innerRadius,
        bottom = cy + innerRadius,
    )
    val startRadians = Math.toRadians(startAngle.toDouble())
    val endAngle = startAngle + sweepAngle
    val endRadians = Math.toRadians(endAngle.toDouble())

    return Path().apply {
        moveTo(
            cx + outerRadius * cos(startRadians).toFloat(),
            cy + outerRadius * sin(startRadians).toFloat(),
        )
        arcTo(outerRect, startAngle, sweepAngle, false)
        lineTo(
            cx + innerRadius * cos(endRadians).toFloat(),
            cy + innerRadius * sin(endRadians).toFloat(),
        )
        arcTo(innerRect, endAngle, -sweepAngle, false)
        close()
    }
}

private fun DrawScope.drawOverlapBoundaryMarkers(
    cx: Float,
    cy: Float,
    innerRadius: Float,
    outerRadius: Float,
    startAngle: Float,
    endAngle: Float,
    color: Color,
) {
    listOf(startAngle, endAngle).forEach { angle ->
        val radians = Math.toRadians(angle.toDouble())
        val cosValue = cos(radians).toFloat()
        val sinValue = sin(radians).toFloat()
        drawLine(
            color = color,
            start = Offset(cx + innerRadius * cosValue, cy + innerRadius * sinValue),
            end = Offset(cx + outerRadius * cosValue, cy + outerRadius * sinValue),
            strokeWidth = 2.5f,
        )
    }
}
