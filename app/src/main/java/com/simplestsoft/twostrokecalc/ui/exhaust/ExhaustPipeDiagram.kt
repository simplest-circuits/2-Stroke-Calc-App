package com.simplestsoft.twostrokecalc.ui.exhaust

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.simplestsoft.twostrokecalc.R
import com.simplestsoft.twostrokecalc.domain.calculation.ExpansionChamberResult
import com.simplestsoft.twostrokecalc.domain.calculation.ExpansionChamberSegment
import com.simplestsoft.twostrokecalc.domain.calculation.ExpansionChamberSegmentId
import com.simplestsoft.twostrokecalc.ui.components.AppColors
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorResultCard
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorResultRow
import com.simplestsoft.twostrokecalc.ui.theme.ContainerCornerRadius
import java.util.Locale
import kotlin.math.abs
import kotlin.math.min

enum class ExhaustPipeDisplayMode {
    EMBEDDED,
    FULLSCREEN,
}

private data class SegmentColors(
    val fill: Color,
    val stroke: Color,
)

private data class SegmentLayout(
    val index: Int,
    val left: Float,
    val right: Float,
    val centerY: Float,
    val startRadius: Float,
    val endRadius: Float,
)

private data class PipeDrawMetrics(
    val startX: Float,
    val centerY: Float,
    val scale: Float,
    val contentWidth: Float,
)

private const val PADDING_H_FRACTION = 0.04f
private const val PADDING_V_FRACTION = 0.12f
private const val MIN_PIPE_ASPECT_RATIO = 3f
private const val MAX_PIPE_ASPECT_RATIO = 12f
private const val FULLSCREEN_INFO_PANEL_WIDTH_DP = 152
private const val FULLSCREEN_CANVAS_H_PADDING_FRACTION = 0.08f
private const val FULLSCREEN_CANVAS_V_PADDING_FRACTION = 0.08f
private const val CANVAS_STROKE_INSET_PX = 3f

private fun pipeAspectRatio(result: ExpansionChamberResult): Float {
    val maxDiameter = result.segments.maxOf { maxOf(it.startDiameterMm, it.endDiameterMm) }
    return (result.totalLengthMm / maxDiameter)
        .toFloat()
        .coerceIn(MIN_PIPE_ASPECT_RATIO, MAX_PIPE_ASPECT_RATIO)
}

private fun computeDrawMetrics(
    width: Float,
    height: Float,
    result: ExpansionChamberResult,
    horizontalPaddingFraction: Float = PADDING_H_FRACTION,
    verticalPaddingFraction: Float = PADDING_V_FRACTION,
    contentInsetPx: Float = 0f,
): PipeDrawMetrics {
    val paddingH = width * horizontalPaddingFraction
    val paddingV = height * verticalPaddingFraction
    val drawWidth = (width - paddingH * 2 - contentInsetPx * 2).coerceAtLeast(1f)
    val drawHeight = (height - paddingV * 2 - contentInsetPx * 2).coerceAtLeast(1f)

    val totalLength = result.totalLengthMm.toFloat()
    val maxDiameter = result.segments.maxOf { maxOf(it.startDiameterMm, it.endDiameterMm) }.toFloat()
    val scale = min(drawWidth / totalLength, drawHeight / maxDiameter)

    val contentWidth = totalLength * scale
    val contentHeight = maxDiameter * scale
    val startX = paddingH + (drawWidth - contentWidth) / 2f
    val centerY = paddingV + (drawHeight - contentHeight) / 2f + contentHeight / 2f

    return PipeDrawMetrics(
        startX = startX,
        centerY = centerY,
        scale = scale,
        contentWidth = contentWidth,
    )
}

@Composable
fun ExhaustPipeDiagram(
    result: ExpansionChamberResult,
    modifier: Modifier = Modifier,
    displayMode: ExhaustPipeDisplayMode = ExhaustPipeDisplayMode.EMBEDDED,
    locale: Locale = Locale.getDefault(),
    selectedSegmentIndex: Int? = null,
    onSegmentSelected: ((Int?) -> Unit)? = null,
) {
    val segmentColors = remember {
        mapOf(
            ExpansionChamberSegmentId.HEADER to SegmentColors(Color(0xFF78909C), Color(0xFF546E7A)),
            ExpansionChamberSegmentId.DIFFUSER_1 to SegmentColors(Color(0xFF42A5F5), Color(0xFF1565C0)),
            ExpansionChamberSegmentId.DIFFUSER_2 to SegmentColors(Color(0xFF29B6F6), Color(0xFF0277BD)),
            ExpansionChamberSegmentId.DIFFUSER_3 to SegmentColors(Color(0xFF26C6DA), Color(0xFF00838F)),
            ExpansionChamberSegmentId.BELLY to SegmentColors(Color(0xFF66BB6A), Color(0xFF2E7D32)),
            ExpansionChamberSegmentId.BAFFLE to SegmentColors(Color(0xFFFFA726), Color(0xFFEF6C00)),
            ExpansionChamberSegmentId.STINGER to SegmentColors(Color(0xFFEF5350), Color(0xFFC62828)),
        )
    }

    val shape = RoundedCornerShape(ContainerCornerRadius)

    if (displayMode == ExhaustPipeDisplayMode.FULLSCREEN) {
        ExhaustPipeFullscreenLayout(
            result = result,
            modifier = modifier,
            locale = locale,
            shape = shape,
            segmentColors = segmentColors,
            selectedSegmentIndex = selectedSegmentIndex,
            onSegmentSelected = onSegmentSelected,
        )
        return
    }

    val embeddedAspectRatio = remember(result) { pipeAspectRatio(result) }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(embeddedAspectRatio)
                .clip(shape)
                .background(AppColors.surface()),
        ) {
            ExhaustPipeCanvas(
                result = result,
                segmentColors = segmentColors,
                selectedSegmentIndex = selectedSegmentIndex,
                interactive = false,
                onSegmentSelected = null,
            )
        }

        ExhaustPipeLegend(
            segmentColors = segmentColors,
            segments = result.segments,
        )
    }
}

@Composable
private fun ExhaustPipeFullscreenLayout(
    result: ExpansionChamberResult,
    modifier: Modifier,
    locale: Locale,
    shape: RoundedCornerShape,
    segmentColors: Map<ExpansionChamberSegmentId, SegmentColors>,
    selectedSegmentIndex: Int?,
    onSegmentSelected: ((Int?) -> Unit)?,
) {
    Row(
        modifier = modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(FULLSCREEN_INFO_PANEL_WIDTH_DP.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (selectedSegmentIndex != null) {
                result.segments.getOrNull(selectedSegmentIndex)?.let { segment ->
                    ExhaustLandscapeSegmentPanel(
                        segment = segment,
                        locale = locale,
                        accentColor = segmentColors[segment.id]?.fill ?: AppColors.primaryBlue(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            } else {
                ExhaustLandscapeSummaryPanel(
                    result = result,
                    locale = locale,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .fillMaxHeight()
                .clip(shape)
                .background(AppColors.surface())
                .padding(end = 4.dp),
        ) {
            ExhaustPipeCanvas(
                result = result,
                segmentColors = segmentColors,
                selectedSegmentIndex = selectedSegmentIndex,
                interactive = onSegmentSelected != null,
                onSegmentSelected = onSegmentSelected,
                horizontalPaddingFraction = FULLSCREEN_CANVAS_H_PADDING_FRACTION,
                verticalPaddingFraction = FULLSCREEN_CANVAS_V_PADDING_FRACTION,
                contentInsetPx = CANVAS_STROKE_INSET_PX,
            )
        }
    }
}

@Composable
private fun ExhaustLandscapePanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(AppColors.cardBorderModifier(shape)),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = AppColors.surface()),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            content = content,
        )
    }
}

@Composable
private fun ExhaustLandscapeSegmentPanel(
    segment: ExpansionChamberSegment,
    locale: Locale,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    ExhaustLandscapePanel(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .padding(end = 6.dp)
                    .size(width = 10.dp, height = 6.dp)
                    .background(accentColor, RoundedCornerShape(2.dp)),
            )
            Text(
                text = exhaustSegmentLabel(segment.id),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.textPrimary(),
                maxLines = 2,
            )
        }
        ExhaustLandscapeMetricLine(
            label = stringResource(R.string.exhaust_segment_detail_length),
            value = "${formatExhaustDecimal(segment.lengthMm, 0, locale)} mm",
        )
        ExhaustLandscapeMetricLine(
            label = stringResource(R.string.exhaust_segment_detail_start_diameter),
            value = "${formatExhaustDecimal(segment.startDiameterMm, 1, locale)} mm",
        )
        ExhaustLandscapeMetricLine(
            label = stringResource(R.string.exhaust_segment_detail_end_diameter),
            value = "${formatExhaustDecimal(segment.endDiameterMm, 1, locale)} mm",
        )
    }
}

@Composable
private fun ExhaustLandscapeSummaryPanel(
    result: ExpansionChamberResult,
    locale: Locale,
    modifier: Modifier = Modifier,
) {
    ExhaustLandscapePanel(modifier = modifier) {
        Text(
            text = stringResource(R.string.exhaust_fullscreen_summary_title),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.textPrimary(),
        )
        ExhaustLandscapeMetricLine(
            label = stringResource(R.string.exhaust_result_total_length),
            value = "${formatExhaustDecimal(result.totalLengthMm, 0, locale)} mm",
        )
        ExhaustLandscapeMetricLine(
            label = stringResource(R.string.exhaust_result_tuned_length),
            value = "${formatExhaustDecimal(result.tunedLengthMm, 0, locale)} mm",
        )
        if (result.diffuserStageCount > 1) {
            ExhaustLandscapeMetricLine(
                label = stringResource(R.string.exhaust_result_horn_coeff),
                value = formatExhaustDecimal(result.hornCoefficient, 2, locale),
            )
        }
        result.diametersMm.forEach { (label, value) ->
            ExhaustLandscapeMetricLine(
                label = label,
                value = "${formatExhaustDecimal(value, 1, locale)} mm",
            )
        }
    }
}

@Composable
private fun ExhaustLandscapeMetricLine(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.textSecondary(),
            maxLines = 1,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = AppColors.textPrimary(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ExhaustPipeCanvas(
    result: ExpansionChamberResult,
    segmentColors: Map<ExpansionChamberSegmentId, SegmentColors>,
    selectedSegmentIndex: Int?,
    interactive: Boolean,
    onSegmentSelected: ((Int?) -> Unit)?,
    horizontalPaddingFraction: Float = PADDING_H_FRACTION,
    verticalPaddingFraction: Float = PADDING_V_FRACTION,
    contentInsetPx: Float = 0f,
) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (interactive && onSegmentSelected != null) {
                    Modifier.pointerInput(
                        result,
                        selectedSegmentIndex,
                        horizontalPaddingFraction,
                        verticalPaddingFraction,
                        contentInsetPx,
                    ) {
                        detectTapGestures { tapOffset ->
                            val layouts = computeSegmentLayouts(
                                width = size.width.toFloat(),
                                height = size.height.toFloat(),
                                result = result,
                                horizontalPaddingFraction = horizontalPaddingFraction,
                                verticalPaddingFraction = verticalPaddingFraction,
                                contentInsetPx = contentInsetPx,
                            )
                            val hitIndex = hitTestSegment(tapOffset, layouts)
                            onSegmentSelected(
                                when {
                                    hitIndex == null -> null
                                    selectedSegmentIndex == hitIndex -> null
                                    else -> hitIndex
                                },
                            )
                        }
                    }
                } else {
                    Modifier
                },
            ),
    ) {
        val metrics = computeDrawMetrics(
            width = size.width,
            height = size.height,
            result = result,
            horizontalPaddingFraction = horizontalPaddingFraction,
            verticalPaddingFraction = verticalPaddingFraction,
            contentInsetPx = contentInsetPx,
        )

        var x = metrics.startX
        result.segments.forEachIndexed { index, segment ->
            val segmentWidth = segment.lengthMm.toFloat() * metrics.scale
            val startRadius = (segment.startDiameterMm / 2.0).toFloat() * metrics.scale
            val endRadius = (segment.endDiameterMm / 2.0).toFloat() * metrics.scale
            val colors = segmentColors[segment.id] ?: SegmentColors(Color.Gray, Color.DarkGray)
            val selected = index == selectedSegmentIndex

            val segmentPath = Path().apply {
                moveTo(x, metrics.centerY - startRadius)
                lineTo(x + segmentWidth, metrics.centerY - endRadius)
                lineTo(x + segmentWidth, metrics.centerY + endRadius)
                lineTo(x, metrics.centerY + startRadius)
                close()
            }
            drawPath(segmentPath, colors.fill)
            drawPath(
                segmentPath,
                if (selected) Color.White else colors.stroke,
                style = Stroke(width = if (selected) 3f else 1.5f),
            )
            x += segmentWidth
        }

        drawLine(
            color = Color.Gray.copy(alpha = 0.35f),
            start = Offset(metrics.startX, metrics.centerY),
            end = Offset(metrics.startX + metrics.contentWidth, metrics.centerY),
            strokeWidth = 1f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)),
        )
    }
}

@Composable
fun ExhaustSegmentDetailCard(
    segment: ExpansionChamberSegment,
    locale: Locale,
    accentColor: Color,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    CalculatorResultCard(
        title = exhaustSegmentLabel(segment.id),
        modifier = modifier.fillMaxWidth(),
    ) {
        if (!compact) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .padding(end = 10.dp)
                        .size(width = 18.dp, height = 10.dp)
                        .background(accentColor, RoundedCornerShape(2.dp)),
                )
                Text(
                    text = stringResource(
                        R.string.exhaust_segment_value,
                        formatExhaustDecimal(segment.lengthMm, 0, locale),
                        formatExhaustDecimal(segment.startDiameterMm, 1, locale),
                        formatExhaustDecimal(segment.endDiameterMm, 1, locale),
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.textPrimary(),
                )
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(width = 14.dp, height = 8.dp)
                        .background(accentColor, RoundedCornerShape(2.dp)),
                )
                Text(
                    text = stringResource(
                        R.string.exhaust_segment_value,
                        formatExhaustDecimal(segment.lengthMm, 0, locale),
                        formatExhaustDecimal(segment.startDiameterMm, 1, locale),
                        formatExhaustDecimal(segment.endDiameterMm, 1, locale),
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.textPrimary(),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        CalculatorResultRow(
            label = stringResource(R.string.exhaust_segment_detail_length),
            value = "${formatExhaustDecimal(segment.lengthMm, 0, locale)} mm",
        )
        CalculatorResultRow(
            label = stringResource(R.string.exhaust_segment_detail_start_diameter),
            value = "${formatExhaustDecimal(segment.startDiameterMm, 1, locale)} mm",
        )
        CalculatorResultRow(
            label = stringResource(R.string.exhaust_segment_detail_end_diameter),
            value = "${formatExhaustDecimal(segment.endDiameterMm, 1, locale)} mm",
        )
    }
}

@Composable
fun exhaustSegmentLabel(id: ExpansionChamberSegmentId): String = when (id) {
    ExpansionChamberSegmentId.HEADER -> stringResource(R.string.exhaust_segment_header)
    ExpansionChamberSegmentId.DIFFUSER_1 -> stringResource(R.string.exhaust_segment_diffuser_1)
    ExpansionChamberSegmentId.DIFFUSER_2 -> stringResource(R.string.exhaust_segment_diffuser_2)
    ExpansionChamberSegmentId.DIFFUSER_3 -> stringResource(R.string.exhaust_segment_diffuser_3)
    ExpansionChamberSegmentId.BELLY -> stringResource(R.string.exhaust_segment_belly)
    ExpansionChamberSegmentId.BAFFLE -> stringResource(R.string.exhaust_segment_baffle)
    ExpansionChamberSegmentId.STINGER -> stringResource(R.string.exhaust_segment_stinger)
}

private fun computeSegmentLayouts(
    width: Float,
    height: Float,
    result: ExpansionChamberResult,
    horizontalPaddingFraction: Float = PADDING_H_FRACTION,
    verticalPaddingFraction: Float = PADDING_V_FRACTION,
    contentInsetPx: Float = 0f,
): List<SegmentLayout> {
    val metrics = computeDrawMetrics(
        width = width,
        height = height,
        result = result,
        horizontalPaddingFraction = horizontalPaddingFraction,
        verticalPaddingFraction = verticalPaddingFraction,
        contentInsetPx = contentInsetPx,
    )

    var x = metrics.startX
    return result.segments.mapIndexed { index, segment ->
        val segmentWidth = segment.lengthMm.toFloat() * metrics.scale
        val layout = SegmentLayout(
            index = index,
            left = x,
            right = x + segmentWidth,
            centerY = metrics.centerY,
            startRadius = (segment.startDiameterMm / 2.0).toFloat() * metrics.scale,
            endRadius = (segment.endDiameterMm / 2.0).toFloat() * metrics.scale,
        )
        x += segmentWidth
        layout
    }
}

private fun hitTestSegment(tap: Offset, layouts: List<SegmentLayout>): Int? {
    layouts.forEach { layout ->
        if (tap.x < layout.left || tap.x > layout.right) return@forEach
        val segmentWidth = layout.right - layout.left
        if (segmentWidth <= 0f) return@forEach
        val t = (tap.x - layout.left) / segmentWidth
        val radiusAtTap = layout.startRadius + (layout.endRadius - layout.startRadius) * t
        if (abs(tap.y - layout.centerY) <= radiusAtTap) {
            return layout.index
        }
    }
    return null
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExhaustPipeLegend(
    segmentColors: Map<ExpansionChamberSegmentId, SegmentColors>,
    segments: List<ExpansionChamberSegment>,
) {
    val legendRes = mapOf(
        ExpansionChamberSegmentId.HEADER to R.string.exhaust_legend_header,
        ExpansionChamberSegmentId.DIFFUSER_1 to R.string.exhaust_legend_diffuser,
        ExpansionChamberSegmentId.DIFFUSER_2 to R.string.exhaust_legend_diffuser,
        ExpansionChamberSegmentId.DIFFUSER_3 to R.string.exhaust_legend_diffuser,
        ExpansionChamberSegmentId.BELLY to R.string.exhaust_legend_belly,
        ExpansionChamberSegmentId.BAFFLE to R.string.exhaust_legend_baffle,
        ExpansionChamberSegmentId.STINGER to R.string.exhaust_legend_stinger,
    )
    val items = segments
        .mapNotNull { segment -> legendRes[segment.id]?.let { segment.id to it } }
        .distinctBy { it.second }
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.forEach { (id, labelRes) ->
            val colors = segmentColors[id] ?: return@forEach
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .size(width = 14.dp, height = 8.dp)
                        .background(colors.fill, RoundedCornerShape(2.dp)),
                )
                Text(
                    text = stringResource(labelRes),
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.textSecondary(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExhaustPipeFullscreenLegend(
    result: ExpansionChamberResult,
    locale: Locale,
    compact: Boolean = false,
) {
    ExhaustLandscapeSummaryPanel(result = result, locale = locale)
}

fun formatExhaustDecimal(value: Double, decimals: Int, locale: Locale): String =
    String.format(locale, "%.${decimals}f", value)
