package com.simplestsoft.twostrokecalc.ui.screens.calculators

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.simplestsoft.twostrokecalc.ui.pro.LocalCalculatorEditingEnabled
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.simplestsoft.twostrokecalc.R
import com.simplestsoft.twostrokecalc.domain.porttiming.IntakeSystem
import com.simplestsoft.twostrokecalc.ui.components.FormFieldDefaults
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorDecimalField
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorHeader
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorScaffold
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorSection
import com.simplestsoft.twostrokecalc.ui.porttiming.PortTimingResultsCard
import com.simplestsoft.twostrokecalc.ui.porttiming.PortTimingViewModel
import java.util.Locale

@Composable
fun TimingCalculatorScreen(
    onOpenPortTimingTool: () -> Unit = {},
    viewModel: PortTimingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()

    CalculatorScaffold {
        CalculatorHeader(
            title = stringResource(R.string.timing_calculator_title),
            subtitle = stringResource(R.string.timing_calculator_subtitle),
        )

        PortTimingIntakeSystemSection(
            intakeSystem = uiState.intakeSystem,
            onIntakeSystemChange = viewModel::onIntakeSystemChange,
        )

        CalculatorSection(title = stringResource(R.string.pt_section_input)) {
            val mm = stringResource(R.string.pt_mm)
            val isRotaryValve = uiState.intakeSystem == IntakeSystem.ROTARY_VALVE

            Column(verticalArrangement = Arrangement.spacedBy(FormFieldDefaults.fieldSpacing)) {
                TimingFieldRow {
                    CalculatorDecimalField(
                        value = uiState.stroke,
                        onValueChange = viewModel::onStrokeChange,
                        label = stringResource(R.string.pt_stroke),
                        suffix = mm,
                        modifier = Modifier.weight(1f),
                    )
                    CalculatorDecimalField(
                        value = uiState.connectingRod,
                        onValueChange = viewModel::onConnectingRodChange,
                        label = stringResource(R.string.pt_connecting_rod),
                        suffix = mm,
                        modifier = Modifier.weight(1f),
                    )
                }
                TimingFieldRow {
                    CalculatorDecimalField(
                        value = uiState.exhaustPort,
                        onValueChange = viewModel::onExhaustPortChange,
                        label = stringResource(R.string.pt_exhaust),
                        suffix = mm,
                        modifier = Modifier.weight(1f),
                    )
                    CalculatorDecimalField(
                        value = uiState.transferPort,
                        onValueChange = viewModel::onTransferPortChange,
                        label = stringResource(R.string.pt_transfer),
                        suffix = mm,
                        modifier = Modifier.weight(1f),
                    )
                }
                TimingFieldRow {
                    CalculatorDecimalField(
                        value = uiState.intakeOpen,
                        onValueChange = viewModel::onIntakeOpenChange,
                        label = stringResource(R.string.pt_intake_open),
                        suffix = mm,
                        enabled = isRotaryValve,
                        modifier = Modifier.weight(1f),
                    )
                    CalculatorDecimalField(
                        value = uiState.intakeClose,
                        onValueChange = viewModel::onIntakeCloseChange,
                        label = stringResource(R.string.pt_intake_close),
                        suffix = mm,
                        enabled = isRotaryValve,
                        modifier = Modifier.weight(1f),
                    )
                }
                CalculatorDecimalField(
                    value = uiState.pistonDeck,
                    onValueChange = viewModel::onPistonDeckChange,
                    label = stringResource(R.string.pt_piston_deck),
                    suffix = mm,
                    supportingText = stringResource(R.string.pt_piston_deck_hint),
                )
            }
        }

        PortTimingResultsCard(
            result = uiState.result,
            input = uiState.lastInput,
            locale = locale,
            errorMessage = uiState.errorMessage,
            roundResults = uiState.roundResults,
            onRoundResultsChange = viewModel::onRoundResultsChange,
        )

        OutlinedButton(
            onClick = onOpenPortTimingTool,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.tool_port_from_calculator))
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun PortTimingIntakeSystemSection(
    intakeSystem: IntakeSystem,
    onIntakeSystemChange: (IntakeSystem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val editingEnabled = LocalCalculatorEditingEnabled.current
    CalculatorSection(
        title = stringResource(R.string.pt_intake_system),
        modifier = modifier,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = intakeSystem == IntakeSystem.ROTARY_VALVE,
                onClick = { onIntakeSystemChange(IntakeSystem.ROTARY_VALVE) },
                enabled = editingEnabled,
            )
            Text(
                text = stringResource(R.string.pt_rotary_valve),
                modifier = Modifier.padding(end = 12.dp),
            )
            RadioButton(
                selected = intakeSystem == IntakeSystem.REED_VALVE,
                onClick = { onIntakeSystemChange(IntakeSystem.REED_VALVE) },
                enabled = editingEnabled,
            )
            Text(stringResource(R.string.pt_reed_valve))
        }
    }
}

@Composable
private fun TimingFieldRow(
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}
