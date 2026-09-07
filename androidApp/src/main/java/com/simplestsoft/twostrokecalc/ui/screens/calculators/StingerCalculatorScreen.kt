package com.simplestsoft.twostrokecalc.ui.screens.calculators

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.simplestsoft.twostrokecalc.R
import com.simplestsoft.twostrokecalc.domain.calculation.StingerCalculator
import com.simplestsoft.twostrokecalc.domain.calculation.StingerFlowRegime
import com.simplestsoft.twostrokecalc.domain.calculation.StingerInput
import com.simplestsoft.twostrokecalc.domain.calculation.StingerResult
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorDecimalField
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorEmptyResultText
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorHeader
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorInvalidResultText
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorPrimaryResultText
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorResultCard
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorResultDivider
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorResultRow
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorScaffold
import com.simplestsoft.twostrokecalc.ui.components.calculator.filterDecimalInput
import com.simplestsoft.twostrokecalc.ui.components.calculator.parseDecimal
import java.util.Locale

@Composable
fun StingerCalculatorScreen() {
    var displacementText by rememberSaveable { mutableStateOf("125") }
    var rpmText by rememberSaveable { mutableStateOf("10000") }
    var exhaustTempText by rememberSaveable { mutableStateOf("600") }
    var gaugePressureText by rememberSaveable {
        mutableStateOf(formatDecimal(StingerCalculator.DEFAULT_GAUGE_PRESSURE_BAR, 2))
    }
    var ambientPressureText by rememberSaveable {
        mutableStateOf(formatDecimal(StingerCalculator.DEFAULT_AMBIENT_PRESSURE_BAR_ABS, 5))
    }
    var gammaText by rememberSaveable {
        mutableStateOf(formatDecimal(StingerCalculator.DEFAULT_GAMMA, 2))
    }

    val displacement = parseDecimal(displacementText)
    val rpm = parseDecimal(rpmText)
    val exhaustTemp = parseDecimal(exhaustTempText)
    val gaugePressure = parseDecimal(gaugePressureText)
    val ambientPressure = parseDecimal(ambientPressureText)
    val gamma = parseDecimal(gammaText)

    val hasInput = displacement != null &&
        rpm != null &&
        exhaustTemp != null &&
        gaugePressure != null &&
        ambientPressure != null &&
        gamma != null

    val result = if (hasInput) {
        StingerCalculator.calculate(
            StingerInput(
                displacementCc = displacement!!,
                rpm = rpm!!,
                exhaustGasTempCelsius = exhaustTemp!!,
                gaugePressureBar = gaugePressure!!,
                ambientPressureBarAbs = ambientPressure!!,
                specificHeatRatio = gamma!!,
            ),
        )
    } else {
        null
    }

    CalculatorScaffold {
        CalculatorHeader(
            title = stringResource(R.string.stinger_calculator_title),
            subtitle = stringResource(R.string.stinger_calculator_subtitle),
        )

        CalculatorDecimalField(
            value = displacementText,
            onValueChange = { displacementText = it.filterDecimalInput() },
            label = stringResource(R.string.exhaust_displacement_label),
            suffix = stringResource(R.string.exhaust_unit_cc),
        )

        CalculatorDecimalField(
            value = rpmText,
            onValueChange = { rpmText = it.filterDecimalInput() },
            label = stringResource(R.string.mean_pressure_rpm_label),
            suffix = stringResource(R.string.exhaust_unit_rpm),
        )

        CalculatorDecimalField(
            value = exhaustTempText,
            onValueChange = { exhaustTempText = it.filterDecimalInput() },
            label = stringResource(R.string.exhaust_optional_exhaust_temp_label),
            suffix = stringResource(R.string.exhaust_unit_celsius),
        )

        CalculatorDecimalField(
            value = gaugePressureText,
            onValueChange = { gaugePressureText = it.filterDecimalInput() },
            label = stringResource(R.string.stinger_gauge_pressure_label),
            suffix = stringResource(R.string.stinger_unit_bar),
            supportingText = stringResource(R.string.stinger_gauge_pressure_hint),
        )

        CalculatorDecimalField(
            value = ambientPressureText,
            onValueChange = { ambientPressureText = it.filterDecimalInput() },
            label = stringResource(R.string.stinger_ambient_pressure_label),
            suffix = stringResource(R.string.stinger_unit_bar_abs),
        )

        CalculatorDecimalField(
            value = gammaText,
            onValueChange = { gammaText = it.filterDecimalInput() },
            label = stringResource(R.string.stinger_gamma_label),
            supportingText = stringResource(R.string.stinger_gamma_hint),
        )

        StingerResultCard(result = result, hasInput = hasInput)

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun StingerResultCard(
    result: StingerResult?,
    hasInput: Boolean,
) {
    CalculatorResultCard(title = stringResource(R.string.stinger_result_title)) {
        when {
            !hasInput -> CalculatorEmptyResultText(stringResource(R.string.stinger_result_empty))
            result == null -> CalculatorInvalidResultText(stringResource(R.string.stinger_result_invalid))
            else -> {
                CalculatorPrimaryResultText(
                    text = stringResource(
                        R.string.stinger_result_diameter,
                        formatDecimal(result.innerDiameterMm, 2),
                    ),
                )
                CalculatorResultDivider()
                CalculatorResultRow(
                    label = stringResource(R.string.stinger_result_mass_flow),
                    value = stringResource(
                        R.string.stinger_result_value_kg_s,
                        formatDecimal(result.massFlowKgPerS, 4),
                    ),
                )
                CalculatorResultRow(
                    label = stringResource(R.string.stinger_result_speed_of_sound),
                    value = stringResource(
                        R.string.stinger_result_value_m_s,
                        formatDecimal(result.speedOfSoundMs, 1),
                    ),
                )
                CalculatorResultRow(
                    label = stringResource(R.string.stinger_result_area),
                    value = stringResource(
                        R.string.stinger_result_value_mm2,
                        formatDecimal(result.areaMm2, 1),
                    ),
                )
                CalculatorResultRow(
                    label = stringResource(R.string.stinger_result_mach),
                    value = formatDecimal(result.machNumber, 3),
                )
                CalculatorResultRow(
                    label = stringResource(R.string.stinger_result_exit_velocity),
                    value = stringResource(
                        R.string.stinger_result_value_m_s,
                        formatDecimal(result.exitVelocityMs, 1),
                    ),
                )
                CalculatorResultRow(
                    label = stringResource(R.string.stinger_result_stagnation_pressure),
                    value = stringResource(
                        R.string.stinger_result_value_bar_abs,
                        formatDecimal(result.stagnationPressureBarAbs, 3),
                    ),
                )
                CalculatorResultRow(
                    label = stringResource(R.string.stinger_result_flow_regime),
                    value = stringResource(
                        when (result.flowRegime) {
                            StingerFlowRegime.SUBSONIC -> R.string.stinger_flow_regime_subsonic
                            StingerFlowRegime.CHOKED -> R.string.stinger_flow_regime_choked
                        },
                    ),
                )
                CalculatorResultDivider()
                CalculatorEmptyResultText(stringResource(R.string.stinger_result_model_note))
            }
        }
    }
}

private fun formatDecimal(value: Double, decimals: Int): String =
    String.format(Locale.getDefault(), "%.${decimals}f", value)
