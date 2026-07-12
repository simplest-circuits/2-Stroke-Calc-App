package com.simplestsoft.twostrokecalc.ui.walkthrough

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.simplestsoft.twostrokecalc.R
import kotlin.math.roundToInt

data class WalkthroughStep(
    val titleRes: Int,
    val bodyRes: Int,
    val targetKey: String? = null,
)

@Composable
fun WalkthroughCoachMarksOverlay(
    steps: List<WalkthroughStep>,
    currentStepIndex: Int,
    targets: Map<String, Rect>,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    onFinish: () -> Unit,
) {
    if (steps.isEmpty()) return
    val step = steps[currentStepIndex.coerceIn(0, steps.lastIndex)]
    val targetRect = step.targetKey?.let(targets::get)
    var infoCardHeightPx by remember { mutableStateOf(0f) }
    var infoCardWidthPx by remember { mutableStateOf(0f) }
    var overlayBoundsInRoot by remember { mutableStateOf<Rect?>(null) }
    val localTargetRect = targetRect?.let { target ->
        overlayBoundsInRoot?.let { overlay ->
            Rect(
                left = target.left - overlay.left,
                top = target.top - overlay.top,
                right = target.right - overlay.left,
                bottom = target.bottom - overlay.top,
            )
        }
    }
    val density = LocalDensity.current
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
    ) {
        val horizontalPaddingPx = with(density) { 20.dp.toPx() }
        val verticalPaddingPx = with(density) { 24.dp.toPx() }
        val cardSpacingPx = with(density) { 20.dp.toPx() }
        val maxWidthPx = with(density) { maxWidth.toPx() }
        val maxHeightPx = with(density) { maxHeight.toPx() }
        val fallbackCardWidth = (maxWidthPx - horizontalPaddingPx * 2f).coerceAtLeast(0f)
        val resolvedCardWidth = if (infoCardWidthPx > 0f) infoCardWidthPx else fallbackCardWidth
        val resolvedCardHeight = if (infoCardHeightPx > 0f) infoCardHeightPx else with(density) { 220.dp.toPx() }
        val defaultY = (maxHeightPx * 0.5f) - (resolvedCardHeight * 0.5f)

        val preferredY = if (targetRect == null) {
            defaultY
        } else {
            val aboveY = targetRect.top - resolvedCardHeight - cardSpacingPx
            val belowY = targetRect.bottom + cardSpacingPx
            val canPlaceAbove = aboveY >= verticalPaddingPx
            val canPlaceBelow = belowY + resolvedCardHeight <= maxHeightPx - verticalPaddingPx
            when {
                targetRect.center.y < maxHeightPx * 0.55f && canPlaceBelow -> belowY
                canPlaceAbove -> aboveY
                canPlaceBelow -> belowY
                else -> defaultY
            }
        }

        var cardY = preferredY.coerceIn(
            verticalPaddingPx,
            (maxHeightPx - resolvedCardHeight - verticalPaddingPx).coerceAtLeast(verticalPaddingPx),
        )
        val cardX = horizontalPaddingPx

        if (targetRect != null) {
            val cardRect = Rect(
                left = cardX,
                top = cardY,
                right = cardX + resolvedCardWidth,
                bottom = cardY + resolvedCardHeight,
            )
            if (cardRect.overlaps(targetRect)) {
                val aboveY = (targetRect.top - resolvedCardHeight - cardSpacingPx)
                    .coerceAtLeast(verticalPaddingPx)
                val belowY = (targetRect.bottom + cardSpacingPx)
                    .coerceAtMost((maxHeightPx - resolvedCardHeight - verticalPaddingPx).coerceAtLeast(verticalPaddingPx))
                cardY = if (targetRect.center.y < maxHeightPx * 0.5f) belowY else aboveY
            }
        }

        Box(
            modifier = Modifier
                .matchParentSize()
                .onGloballyPositioned { coordinates ->
                    overlayBoundsInRoot = coordinates.boundsInRoot()
                }
                .pointerInput(localTargetRect, overlayBoundsInRoot) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        if (localTargetRect?.contains(down.position) == true) {
                            return@awaitEachGesture
                        }
                        val pointerId = down.id
                        do {
                            val event = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Main)
                            event.changes.forEach { change ->
                                if (change.id == pointerId) {
                                    change.consume()
                                }
                            }
                        } while (event.changes.any { it.pressed })
                    }
                },
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                if (localTargetRect != null) {
                    val spotlight = Path().apply {
                        fillType = PathFillType.EvenOdd
                        addRect(Rect(0f, 0f, size.width, size.height))
                        addRoundRect(
                            RoundRect(
                                rect = localTargetRect,
                                cornerRadius = CornerRadius(22f, 22f),
                            ),
                        )
                    }
                    drawPath(spotlight, color = Color.Black.copy(alpha = 0.68f))
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.95f),
                        topLeft = localTargetRect.topLeft,
                        size = localTargetRect.size,
                        cornerRadius = CornerRadius(22f, 22f),
                        style = Stroke(width = 4f),
                    )
                } else {
                    drawRect(color = Color.Black.copy(alpha = 0.68f))
                }
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .offset {
                    IntOffset(
                        x = 0,
                        y = cardY.roundToInt(),
                    )
                }
                .onGloballyPositioned { coordinates ->
                    infoCardHeightPx = coordinates.size.height.toFloat()
                    infoCardWidthPx = coordinates.size.width.toFloat()
                },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
            ) {
                Text(
                    text = stringResource(step.titleRes),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(step.bodyRes),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(
                    text = stringResource(
                        R.string.walkthrough_step_counter,
                        currentStepIndex + 1,
                        steps.size,
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 10.dp),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                ) {
                    TextButton(onClick = onSkip) {
                        Text(stringResource(R.string.walkthrough_skip))
                    }
                    if (currentStepIndex > 0) {
                        TextButton(onClick = onBack) {
                            Text(stringResource(R.string.walkthrough_back))
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    if (currentStepIndex < steps.lastIndex) {
                        Button(
                            onClick = onNext,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        ) {
                            Text(stringResource(R.string.walkthrough_next))
                        }
                    } else {
                        Button(
                            onClick = onFinish,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        ) {
                            Text(stringResource(R.string.walkthrough_finish))
                        }
                    }
                }
            }
        }
    }
}
