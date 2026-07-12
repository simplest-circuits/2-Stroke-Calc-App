package com.simplestsoft.twostrokecalc.ui.porttiming

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.simplestsoft.twostrokecalc.R
import com.simplestsoft.twostrokecalc.domain.porttiming.IntakeSystem
import com.simplestsoft.twostrokecalc.domain.porttiming.PortTimingInput
import com.simplestsoft.twostrokecalc.domain.porttiming.PortTimingResult
import com.simplestsoft.twostrokecalc.ui.components.AppColors
import com.simplestsoft.twostrokecalc.ui.pro.LocalCalculatorEditingEnabled
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorEmptyResultText
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorInvalidResultText
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorResultCard
import java.util.Locale

private enum class PortTimingChannel {
    ROUND,
    EXHAUST,
    BLOWDOWN,
    TRANSFER,
    INTAKE,
}

private data class PortTimingChannelColors(
    val round: Color,
    val exhaust: Color,
    val blowdown: Color,
    val transfer: Color,
    val intake: Color,
) {
    fun colorFor(channel: PortTimingChannel): Color = when (channel) {
        PortTimingChannel.ROUND -> round
        PortTimingChannel.EXHAUST -> exhaust
        PortTimingChannel.BLOWDOWN -> blowdown
        PortTimingChannel.TRANSFER -> transfer
        PortTimingChannel.INTAKE -> intake
    }
}

private data class PortTimingValueItem(
    val label: String,
    val value: String,
    val channel: PortTimingChannel,
)

private data class PortTimingIntakeRow(
    val label: String,
    val value: String,
    val emphasized: Boolean = false,
)

private sealed interface PortTimingResultRow {
    data class Value(val item: PortTimingValueItem) : PortTimingResultRow

    data class IntakeGroup(val rows: List<PortTimingIntakeRow>) : PortTimingResultRow
}

@Composable
fun PortTimingResultsCard(
    result: PortTimingResult?,
    input: PortTimingInput?,
    locale: Locale,
    errorMessage: String?,
    roundResults: Boolean,
    onRoundResultsChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = rememberPortTimingChannelColors()

    CalculatorResultCard(
        title = stringResource(R.string.pt_result_title),
        modifier = modifier,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PortTimingRoundTile(
                checked = roundResults,
                onCheckedChange = onRoundResultsChange,
                colors = colors,
            )
            when {
                errorMessage != null -> {
                    CalculatorInvalidResultText(
                        message = when (errorMessage) {
                            "invalid_input" -> stringResource(R.string.pt_error_invalid)
                            "out_of_range" -> stringResource(R.string.pt_error_out_of_range)
                            else -> errorMessage
                        },
                    )
                }
                result == null || input == null -> {
                    CalculatorEmptyResultText(stringResource(R.string.pt_not_calculated))
                }
                else -> {
                    PortTimingResultsList(
                        rows = buildPortTimingResultRows(
                            result = result,
                            input = input,
                            locale = locale,
                        ),
                        colors = colors,
                    )
                }
            }
        }
    }
}

@Composable
private fun buildPortTimingResultRows(
    result: PortTimingResult,
    input: PortTimingInput,
    locale: Locale,
): List<PortTimingResultRow> {
    return buildList {
        result.exhaust?.let {
            add(
                PortTimingResultRow.Value(
                    PortTimingValueItem(
                        label = stringResource(R.string.pt_exhaust_duration),
                        value = formatPortTimingDegrees(it.duration, input.roundResults, locale),
                        channel = PortTimingChannel.EXHAUST,
                    ),
                ),
            )
        }
        result.blowdown?.takeIf { it > 0.05 }?.let {
            add(
                PortTimingResultRow.Value(
                    PortTimingValueItem(
                        label = stringResource(R.string.pt_blowdown),
                        value = formatPortTimingDegrees(it, input.roundResults, locale),
                        channel = PortTimingChannel.BLOWDOWN,
                    ),
                ),
            )
        }
        result.transfer?.let {
            add(
                PortTimingResultRow.Value(
                    PortTimingValueItem(
                        label = stringResource(R.string.pt_transfer_duration),
                        value = formatPortTimingDegrees(it.duration, input.roundResults, locale),
                        channel = PortTimingChannel.TRANSFER,
                    ),
                ),
            )
        }
        if (input.intakeSystem == IntakeSystem.ROTARY_VALVE) {
            result.intake?.let { intake ->
                add(
                    PortTimingResultRow.IntakeGroup(
                        rows = listOf(
                            PortTimingIntakeRow(
                                label = stringResource(R.string.pt_intake_before_tdc),
                                value = formatPortTimingDegrees(intake.openBeforeTdc, input.roundResults, locale),
                            ),
                            PortTimingIntakeRow(
                                label = stringResource(R.string.pt_intake_after_tdc),
                                value = formatPortTimingDegrees(intake.closeAfterTdc, input.roundResults, locale),
                            ),
                            PortTimingIntakeRow(
                                label = stringResource(R.string.pt_intake_duration),
                                value = formatPortTimingDegrees(intake.duration, input.roundResults, locale),
                                emphasized = true,
                            ),
                        ),
                    ),
                )
            }
        }
    }
}

@Composable
private fun PortTimingResultsList(
    rows: List<PortTimingResultRow>,
    colors: PortTimingChannelColors,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rows.forEach { row ->
            when (row) {
                is PortTimingResultRow.Value -> PortTimingResultTile(
                    item = row.item,
                    colors = colors,
                )
                is PortTimingResultRow.IntakeGroup -> PortTimingIntakeGroupTile(
                    rows = row.rows,
                    colors = colors,
                )
            }
        }
    }
}

@Composable
private fun PortTimingRoundTile(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    colors: PortTimingChannelColors,
) {
    val editingEnabled = LocalCalculatorEditingEnabled.current
    val accent = colors.round
    val shape = RoundedCornerShape(12.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(shape)
            .background(accent.copy(alpha = if (AppColors.isDarkTheme()) 0.16f else 0.10f))
            .padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(2.dp))
                .background(accent),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = stringResource(R.string.pt_round),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.textSecondary(),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = editingEnabled,
        )
    }
}

@Composable
private fun PortTimingResultTile(
    item: PortTimingValueItem,
    colors: PortTimingChannelColors,
) {
    val accent = colors.colorFor(item.channel)
    val shape = RoundedCornerShape(12.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(shape)
            .background(accent.copy(alpha = if (AppColors.isDarkTheme()) 0.16f else 0.10f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(2.dp))
                .background(accent),
        )
        Spacer(Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = item.label,
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.textSecondary(),
            )
            Text(
                text = item.value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = AppColors.primaryBlue(),
            )
        }
    }
}

@Composable
private fun PortTimingIntakeGroupTile(
    rows: List<PortTimingIntakeRow>,
    colors: PortTimingChannelColors,
) {
    val accent = colors.intake
    val shape = RoundedCornerShape(12.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(shape)
            .background(accent.copy(alpha = if (AppColors.isDarkTheme()) 0.16f else 0.10f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(2.dp))
                .background(accent),
        )
        Spacer(Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.pt_legend_intake),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = accent,
            )
            rows.forEachIndexed { index, row ->
                if (index > 0) {
                    HorizontalDivider(color = accent.copy(alpha = 0.22f))
                }
                PortTimingIntakeGroupRow(row = row)
            }
        }
    }
}

@Composable
private fun PortTimingIntakeGroupRow(row: PortTimingIntakeRow) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = row.label,
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.textSecondary(),
        )
        Text(
            text = row.value,
            style = if (row.emphasized) {
                MaterialTheme.typography.headlineSmall
            } else {
                MaterialTheme.typography.titleMedium
            },
            fontWeight = if (row.emphasized) FontWeight.Bold else FontWeight.SemiBold,
            color = if (row.emphasized) AppColors.primaryBlue() else AppColors.textPrimary(),
        )
    }
}

@Composable
private fun rememberPortTimingChannelColors(): PortTimingChannelColors {
    val scheme = MaterialTheme.colorScheme
    val isDark = AppColors.isDarkTheme()
    return remember(isDark, scheme) {
        PortTimingChannelColors(
            round = scheme.outline,
            exhaust = scheme.onSurfaceVariant,
            blowdown = scheme.onSurface.copy(alpha = if (isDark) 0.85f else 0.7f),
            transfer = scheme.primary,
            intake = scheme.secondary,
        )
    }
}
