package com.simplestsoft.twostrokecalc.ui.portarea

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.simplestsoft.twostrokecalc.R
import com.simplestsoft.twostrokecalc.domain.portarea.ExhaustPortShape
import com.simplestsoft.twostrokecalc.domain.portarea.ExhaustPortType
import com.simplestsoft.twostrokecalc.domain.portarea.PortGeometryLayout
import com.simplestsoft.twostrokecalc.domain.portarea.PortWindowRadii
import com.simplestsoft.twostrokecalc.domain.portarea.PortWindowRole
import com.simplestsoft.twostrokecalc.domain.portarea.bridgeAfterWindow
import com.simplestsoft.twostrokecalc.domain.portarea.coerced
import com.simplestsoft.twostrokecalc.ui.components.AppColors
import com.simplestsoft.twostrokecalc.ui.theme.ContainerCornerRadius
import java.util.Locale

@Composable
fun ExhaustPortGeometryDiagram(
    layout: PortGeometryLayout,
    locale: Locale,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        val cardShape = RoundedCornerShape(ContainerCornerRadius)
        val wallColor = AppColors.textSecondary().copy(alpha = 0.35f)
        val portFill = AppColors.primaryBlue().copy(alpha = 0.55f)
        val portStroke = AppColors.primaryBlue()
        val boosterFill = AppColors.primaryBlue().copy(alpha = 0.35f)
        val bridgeFill = AppColors.textSecondary().copy(alpha = 0.25f)
        val bridgeStroke = AppColors.textSecondary().copy(alpha = 0.6f)
        val dimensionColor = AppColors.textSecondary().copy(alpha = 0.85f)
        val otLabel = stringResource(R.string.port_area_exhaust_diagram_ot)
        val utLabel = stringResource(R.string.port_area_exhaust_diagram_ut)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2.2f)
                .clip(cardShape)
                .background(AppColors.surface()),
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
            ) {
                val padding = size.width * 0.06f
                val topLabelSpace = size.height * 0.14f
                val bottomLabelSpace = size.height * 0.12f
                val drawWidth = size.width - padding * 2
                val drawHeight = size.height - padding * 2 - topLabelSpace - bottomLabelSpace

                val scale = minOf(
                    drawWidth / layout.totalTopSpanMm.toFloat(),
                    drawHeight / layout.portHeightMm.toFloat(),
                )
                val contentWidth = layout.totalTopSpanMm.toFloat() * scale
                val contentHeight = layout.portHeightMm.toFloat() * scale
                val originX = padding + (drawWidth - contentWidth) / 2f
                val originY = padding + topLabelSpace

                drawRect(
                    color = wallColor,
                    topLeft = Offset(originX, originY),
                    size = Size(contentWidth, contentHeight),
                    style = Stroke(width = 2f),
                )

                var xTop = originX
                layout.windows.forEachIndexed { index, window ->
                    val topWidth = window.topWidthMm.toFloat() * scale
                    val bottomWidth = window.bottomWidthMm.toFloat() * scale
                    val windowHeight = window.heightMm.toFloat() * scale
                    val fill = if (window.roleLabelKey == PortWindowRole.BOOSTER) boosterFill else portFill

                    drawPortShape(
                        shape = window.shape,
                        topLeftX = xTop,
                        topWidth = topWidth,
                        bottomWidth = bottomWidth,
                        topY = originY,
                        height = windowHeight,
                        radii = window.radii,
                        vShapeApexAtEnd = window.vShapeApexAtEnd,
                        scale = scale,
                        fillColor = fill,
                        strokeColor = portStroke,
                    )

                    xTop += topWidth
                    if (index < layout.windows.lastIndex) {
                        val bridgeWidth = layout.bridgeAfterWindow(index).toFloat() * scale
                        drawRect(
                            color = bridgeFill,
                            topLeft = Offset(xTop, originY),
                            size = Size(bridgeWidth, contentHeight),
                        )
                        drawRect(
                            color = bridgeStroke,
                            topLeft = Offset(xTop, originY),
                            size = Size(bridgeWidth, contentHeight),
                            style = Stroke(width = 1.5f),
                        )
                        xTop += bridgeWidth
                    }
                }

                layout.boosterHeightMm?.let { boosterHeight ->
                    if (boosterHeight < layout.portHeightMm) {
                        val boosterBottomY = originY + boosterHeight.toFloat() * scale
                        drawLine(
                            color = wallColor.copy(alpha = 0.7f),
                            start = Offset(originX, boosterBottomY),
                            end = Offset(originX + contentWidth, boosterBottomY),
                            strokeWidth = 1.5f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)),
                        )
                    }
                }

                drawChordDimension(
                    left = originX,
                    right = originX + contentWidth,
                    y = originY - 6f,
                    color = dimensionColor,
                )

                drawBottomOpeningsDimension(
                    layout = layout,
                    originX = originX,
                    originY = originY,
                    contentHeight = contentHeight,
                    scale = scale,
                    color = dimensionColor,
                )

                drawOrientationMarkers(
                    originX = originX,
                    originY = originY,
                    contentWidth = contentWidth,
                    contentHeight = contentHeight,
                    color = dimensionColor,
                )
            }

            Text(
                text = otLabel,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.textSecondary(),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 14.dp, end = 20.dp),
            )
            Text(
                text = utLabel,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.textSecondary(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 14.dp, end = 20.dp),
            )
        }

        Text(
            text = geometrySummary(layout, locale),
            style = MaterialTheme.typography.bodySmall,
            color = AppColors.textSecondary(),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        )
    }
}

/**
 * Top chord (Sehnenmaß): ports are laid out with flush top edges so [PortGeometryLayout.totalTopSpanMm]
 * equals the drawn width at OT.
 */
private fun DrawScope.drawChordDimension(
    left: Float,
    right: Float,
    y: Float,
    color: Color,
) {
    drawLine(color = color, start = Offset(left, y), end = Offset(right, y), strokeWidth = 1.5f)
    drawLine(color = color, start = Offset(left, y - 4f), end = Offset(left, y + 4f), strokeWidth = 1.5f)
    drawLine(color = color, start = Offset(right, y - 4f), end = Offset(right, y + 4f), strokeWidth = 1.5f)
}

/** Marks each port opening at UT (bottom edge), excluding bridge material. */
private fun DrawScope.drawBottomOpeningsDimension(
    layout: PortGeometryLayout,
    originX: Float,
    originY: Float,
    contentHeight: Float,
    scale: Float,
    color: Color,
) {
    val y = originY + contentHeight + 6f
    var xTop = originX
    layout.windows.forEachIndexed { index, window ->
        val topWidth = window.topWidthMm.toFloat() * scale
        val bottomWidth = window.bottomWidthMm.toFloat() * scale
        if (bottomWidth > 0f) {
            val bottomLeft = PortFaceGeometry.bottomLeftX(xTop, topWidth, bottomWidth)
            val bottomRight = bottomLeft + bottomWidth
            drawLine(color = color, start = Offset(bottomLeft, y), end = Offset(bottomRight, y), strokeWidth = 1.5f)
            drawLine(
                color = color,
                start = Offset(bottomLeft, y - 3f),
                end = Offset(bottomLeft, y + 3f),
                strokeWidth = 1.5f,
            )
            drawLine(
                color = color,
                start = Offset(bottomRight, y - 3f),
                end = Offset(bottomRight, y + 3f),
                strokeWidth = 1.5f,
            )
        }
        xTop += topWidth
        if (index < layout.windows.lastIndex) {
            xTop += layout.bridgeAfterWindow(index).toFloat() * scale
        }
    }
}

private fun DrawScope.drawOrientationMarkers(
    originX: Float,
    originY: Float,
    contentWidth: Float,
    contentHeight: Float,
    color: Color,
) {
    val markerX = originX + contentWidth + 6f
    drawLine(
        color = color,
        start = Offset(markerX, originY),
        end = Offset(markerX, originY + contentHeight),
        strokeWidth = 1.5f,
    )
    drawLine(
        color = color,
        start = Offset(markerX - 4f, originY + 2f),
        end = Offset(markerX, originY),
        strokeWidth = 1.5f,
    )
    drawLine(
        color = color,
        start = Offset(markerX - 4f, originY + contentHeight - 2f),
        end = Offset(markerX, originY + contentHeight),
        strokeWidth = 1.5f,
    )
}

private fun DrawScope.drawPortShape(
    shape: ExhaustPortShape,
    topLeftX: Float,
    topWidth: Float,
    bottomWidth: Float,
    topY: Float,
    height: Float,
    radii: PortWindowRadii,
    vShapeApexAtEnd: Boolean,
    scale: Float,
    fillColor: Color,
    strokeColor: Color,
) {
    val bottomLeftX = PortFaceGeometry.bottomLeftX(topLeftX, topWidth, bottomWidth)
    val coerced = radii.coerced(
        bottomWidthMm = (bottomWidth / scale).toDouble(),
        topWidthMm = (topWidth / scale).toDouble(),
        heightMm = (height / scale).toDouble(),
    )
    val utCornerR = (coerced.utCornerRadiusMm * scale).toFloat()
    val otCornerR = (coerced.otCornerRadiusMm * scale).toFloat()
    val topEdgeR = (coerced.topEdgeRadiusMm * scale).toFloat()

    when (shape) {
        ExhaustPortShape.OVAL -> {
            val ovalWidth = if (bottomWidth > 0f) bottomWidth else topWidth
            val ovalLeft = topLeftX + (topWidth - ovalWidth) / 2f
            drawOval(color = fillColor, topLeft = Offset(ovalLeft, topY), size = Size(ovalWidth, height))
            drawOval(
                color = strokeColor,
                topLeft = Offset(ovalLeft, topY),
                size = Size(ovalWidth, height),
                style = Stroke(2f),
            )
        }

        ExhaustPortShape.ROUNDED_RECT -> {
            if (topEdgeR <= 0f && topWidth == bottomWidth && topLeftX == bottomLeftX) {
                val maxRadius = minOf(bottomWidth, height) / 2f
                val radius = utCornerR.coerceIn(0f, maxRadius)
                drawRoundRect(
                    color = fillColor,
                    topLeft = Offset(bottomLeftX, topY),
                    size = Size(bottomWidth, height),
                    cornerRadius = CornerRadius(radius, radius),
                )
                drawRoundRect(
                    color = strokeColor,
                    topLeft = Offset(bottomLeftX, topY),
                    size = Size(bottomWidth, height),
                    cornerRadius = CornerRadius(radius, radius),
                    style = Stroke(
                        width = 2f,
                        join = StrokeJoin.Round,
                        cap = StrokeCap.Round,
                    ),
                )
            } else {
                drawPathOutline(
                    path = PortFaceGeometry.trapezoidPath(
                        topLeftX = topLeftX,
                        topWidth = topWidth,
                        bottomWidth = bottomWidth,
                        topY = topY,
                        height = height,
                        utCornerRadius = utCornerR,
                        otCornerRadius = otCornerR,
                        topEdgeSagitta = topEdgeR,
                    ),
                    fillColor = fillColor,
                    strokeColor = strokeColor,
                )
            }
        }

        ExhaustPortShape.TRIANGULAR -> {
            val path = PortFaceGeometry.trianglePath(
                topLeftX = topLeftX,
                topWidth = topWidth,
                topY = topY,
                height = height,
                utCornerRadius = utCornerR,
                otCornerRadius = otCornerR,
                topEdgeSagitta = topEdgeR,
            )
            drawPathOutline(path, fillColor, strokeColor)
        }

        ExhaustPortShape.V_SHAPE -> {
            val path = PortFaceGeometry.vShapePath(
                topLeftX = topLeftX,
                topWidth = topWidth,
                topY = topY,
                height = height,
                apexAtEnd = vShapeApexAtEnd,
                otCornerRadius = otCornerR,
                topEdgeSagitta = topEdgeR,
            )
            drawPathOutline(path, fillColor, strokeColor)
        }

        ExhaustPortShape.RECTANGULAR,
        ExhaustPortShape.TRAPEZOID,
        -> {
            val path = PortFaceGeometry.trapezoidPath(
                topLeftX = topLeftX,
                topWidth = topWidth,
                bottomWidth = bottomWidth,
                topY = topY,
                height = height,
                utCornerRadius = utCornerR,
                otCornerRadius = otCornerR,
                topEdgeSagitta = topEdgeR,
            )
            drawPathOutline(path, fillColor, strokeColor)
        }
    }
}

private fun DrawScope.drawPathOutline(path: Path, fillColor: Color, strokeColor: Color) {
    drawPath(path, fillColor)
    drawPath(
        path,
        strokeColor,
        style = Stroke(
            width = 2f,
            join = StrokeJoin.Round,
            cap = StrokeCap.Round,
        ),
    )
}

@Composable
private fun geometrySummary(layout: PortGeometryLayout, locale: Locale): String {
    val typeLabel = when (layout.type) {
        ExhaustPortType.SINGLE -> stringResource(R.string.port_area_exhaust_type_single)
        ExhaustPortType.TWIN -> stringResource(R.string.port_area_exhaust_type_twin)
        ExhaustPortType.TRIPLE -> stringResource(R.string.port_area_exhaust_type_triple)
    }

    if (layout.type == ExhaustPortType.TRIPLE) {
        val main = layout.windows.firstOrNull { it.roleLabelKey == PortWindowRole.MAIN }
        val leftBooster = layout.windows.firstOrNull()
        val rightBooster = layout.windows.lastOrNull()
        if (main != null && leftBooster != null && rightBooster != null) {
            val boosterShapeLabel = if (leftBooster.shape == rightBooster.shape) {
                stringResource(leftBooster.shape.labelRes())
            } else {
                "${stringResource(leftBooster.shape.labelRes())} / ${stringResource(rightBooster.shape.labelRes())}"
            }
            return stringResource(
                R.string.port_area_exhaust_geometry_compound_summary,
                typeLabel,
                formatPortAreaDecimal(main.topWidthMm, 1, locale),
                formatPortAreaDecimal(main.bottomWidthMm, 1, locale),
                formatPortAreaDecimal(main.heightMm, 1, locale),
                boosterShapeLabel,
                formatPortAreaDecimal(leftBooster.topWidthMm, 1, locale),
                formatPortAreaDecimal(leftBooster.heightMm, 1, locale),
                formatPortAreaDecimal(layout.totalAreaMm2, 0, locale),
            )
        }
    }

    val window = layout.windows.firstOrNull() ?: return ""
    val shapeLabel = stringResource(window.shape.labelRes())
    return when (window.shape) {
        ExhaustPortShape.TRAPEZOID -> {
            if (layout.windows.size > 1) {
                stringResource(
                    R.string.port_area_exhaust_geometry_trapezoid_multi_summary,
                    typeLabel,
                    shapeLabel,
                    formatPortAreaDecimal(layout.totalTopSpanMm, 1, locale),
                    formatPortAreaDecimal(layout.totalBottomSpanMm, 1, locale),
                    formatPortAreaDecimal(window.topWidthMm, 1, locale),
                    formatPortAreaDecimal(window.bottomWidthMm, 1, locale),
                    formatPortAreaDecimal(window.heightMm, 1, locale),
                    formatPortAreaDecimal(layout.totalAreaMm2, 0, locale),
                )
            } else {
                stringResource(
                    R.string.port_area_exhaust_geometry_trapezoid_summary,
                    typeLabel,
                    shapeLabel,
                    formatPortAreaDecimal(window.topWidthMm, 1, locale),
                    formatPortAreaDecimal(window.bottomWidthMm, 1, locale),
                    formatPortAreaDecimal(window.heightMm, 1, locale),
                    formatPortAreaDecimal(layout.totalAreaMm2, 0, locale),
                )
            }
        }
        ExhaustPortShape.TRIANGULAR -> stringResource(
            R.string.port_area_exhaust_geometry_triangle_summary,
            typeLabel,
            shapeLabel,
            formatPortAreaDecimal(window.topWidthMm, 1, locale),
            formatPortAreaDecimal(window.heightMm, 1, locale),
            formatPortAreaDecimal(layout.totalAreaMm2, 0, locale),
        )
        ExhaustPortShape.V_SHAPE -> stringResource(
            R.string.port_area_exhaust_geometry_vshape_summary,
            typeLabel,
            shapeLabel,
            formatPortAreaDecimal(window.topWidthMm, 1, locale),
            formatPortAreaDecimal(window.heightMm, 1, locale),
            formatPortAreaDecimal(layout.totalAreaMm2, 0, locale),
        )
        ExhaustPortShape.ROUNDED_RECT -> stringResource(
            R.string.port_area_exhaust_geometry_rounded_summary,
            typeLabel,
            shapeLabel,
            formatPortAreaDecimal(window.bottomWidthMm, 1, locale),
            formatPortAreaDecimal(window.heightMm, 1, locale),
            formatPortAreaDecimal(window.radii.cornerRadiusMm, 1, locale),
            formatPortAreaDecimal(layout.totalAreaMm2, 0, locale),
        )
        else -> stringResource(
            R.string.port_area_exhaust_geometry_summary,
            typeLabel,
            shapeLabel,
            formatPortAreaDecimal(window.bottomWidthMm, 1, locale),
            formatPortAreaDecimal(window.heightMm, 1, locale),
            formatPortAreaDecimal(layout.totalAreaMm2, 0, locale),
        )
    }
}

private fun ExhaustPortShape.labelRes(): Int = when (this) {
    ExhaustPortShape.RECTANGULAR -> R.string.port_area_exhaust_shape_rectangular
    ExhaustPortShape.ROUNDED_RECT -> R.string.port_area_exhaust_shape_rounded
    ExhaustPortShape.OVAL -> R.string.port_area_exhaust_shape_oval
    ExhaustPortShape.TRAPEZOID -> R.string.port_area_exhaust_shape_trapezoid
    ExhaustPortShape.TRIANGULAR -> R.string.port_area_exhaust_shape_triangular
    ExhaustPortShape.V_SHAPE -> R.string.port_area_exhaust_shape_vshape
}

fun formatPortAreaDecimal(value: Double, decimals: Int, locale: Locale): String =
    String.format(locale, "%.${decimals}f", value)
