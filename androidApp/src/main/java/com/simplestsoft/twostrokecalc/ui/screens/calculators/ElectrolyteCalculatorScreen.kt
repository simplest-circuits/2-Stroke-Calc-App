package com.simplestsoft.twostrokecalc.ui.screens.calculators

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.simplestsoft.twostrokecalc.R
import com.simplestsoft.twostrokecalc.domain.calculation.ElectrolyteCalculator
import com.simplestsoft.twostrokecalc.domain.calculation.ElectrolyteCalculator.SliderDriver
import com.simplestsoft.twostrokecalc.domain.calculation.ElectrolyteCalculator.SliderState
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorInvalidResultText
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorResultCard
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorResultDivider
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorResultRow
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorSection
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorSliderField
import java.util.Locale
import kotlin.math.roundToInt

private const val ACID_PERCENT_STEPS = 50
private const val CURRENT_AMP_STEPS = 45

@Composable
fun ElectrolyteCalculatorContent() {
    val reference = ElectrolyteCalculator.referenceSliderState()
    var totalLiters by rememberSaveable { mutableFloatStateOf(reference.totalLiters.toFloat()) }
    var waterLiters by rememberSaveable { mutableFloatStateOf(reference.waterLiters.toFloat()) }
    var acidMl by rememberSaveable { mutableFloatStateOf(reference.aceticAcidMl.toFloat()) }
    var saltG by rememberSaveable { mutableFloatStateOf(reference.saltG.toFloat()) }
    var acidPercent by rememberSaveable { mutableFloatStateOf(reference.acidConcentrationPercent.toFloat()) }
    var currentAmps by rememberSaveable { mutableFloatStateOf(reference.electrificationCurrentAmps.toFloat()) }

    fun applySyncedState(driver: SliderDriver) {
        val synced = ElectrolyteCalculator.syncSliders(
            driver = driver,
            totalLiters = totalLiters.toDouble(),
            waterLiters = waterLiters.toDouble(),
            aceticAcidMl = acidMl.toDouble(),
            saltG = saltG.toDouble(),
            acidConcentrationPercent = acidPercent.toDouble(),
            electrificationCurrentAmps = currentAmps.toDouble(),
        ) ?: return

        totalLiters = synced.totalLiters.toFloat()
        waterLiters = synced.waterLiters.toFloat()
        acidMl = synced.aceticAcidMl.toFloat()
        saltG = synced.saltG.toFloat()
        acidPercent = synced.acidConcentrationPercent.toFloat()
    }

    val sliderState = SliderState(
        totalLiters = totalLiters.toDouble(),
        waterLiters = waterLiters.toDouble(),
        aceticAcidMl = acidMl.toDouble(),
        saltG = saltG.toDouble(),
        acidConcentrationPercent = acidPercent.toDouble(),
        electrificationCurrentAmps = currentAmps.toDouble().coerceIn(
            ElectrolyteCalculator.MIN_ELECTRIFICATION_CURRENT_AMPS,
            ElectrolyteCalculator.MAX_ELECTRIFICATION_CURRENT_AMPS,
        ),
    )
    val result = ElectrolyteCalculator.calculate(sliderState)
    val loadVoltage = ElectrolyteCalculator.voltageForLoad(
        currentAmps = currentAmps.toDouble(),
        totalLiters = totalLiters.toDouble(),
    )

    CalculatorSection(title = "") {
        CalculatorSliderField(
            label = stringResource(R.string.electrolyte_salt_label),
            value = saltG,
            onValueChange = {
                saltG = it
                applySyncedState(SliderDriver.SALT)
            },
            valueRange = ElectrolyteCalculator.MIN_SALT_G.toFloat()..
                ElectrolyteCalculator.MAX_SALT_G.toFloat(),
            steps = ElectrolyteCalculator.saltSliderSteps,
            valueDisplay = stringResource(
                R.string.electrolyte_salt_slider_value,
                formatGrams(saltG.toDouble()),
            ),
        )

        CalculatorSliderField(
            label = stringResource(R.string.electrolyte_total_volume_label),
            value = totalLiters,
            onValueChange = {
                totalLiters = it
                applySyncedState(SliderDriver.TOTAL)
            },
            valueRange = ElectrolyteCalculator.MIN_TOTAL_LITERS.toFloat()..
                ElectrolyteCalculator.MAX_TOTAL_LITERS.toFloat(),
            steps = ElectrolyteCalculator.totalVolumeSliderSteps,
            valueDisplay = stringResource(
                R.string.electrolyte_total_volume_value,
                formatLiters(totalLiters.toDouble()),
            ),
        )

        CalculatorSliderField(
            label = stringResource(R.string.electrolyte_acid_concentration_label),
            value = acidPercent,
            onValueChange = {
                acidPercent = it
                applySyncedState(SliderDriver.ACID_CONCENTRATION)
            },
            valueRange = ElectrolyteCalculator.MIN_ACID_CONCENTRATION_PERCENT.toFloat()..
                ElectrolyteCalculator.MAX_ACID_CONCENTRATION_PERCENT.toFloat(),
            steps = ACID_PERCENT_STEPS,
            valueDisplay = stringResource(
                R.string.electrolyte_acid_concentration_value,
                formatPercent(acidPercent.toDouble()),
            ),
        )

        CalculatorSliderField(
            label = stringResource(R.string.electrolyte_current_label),
            value = currentAmps,
            onValueChange = { currentAmps = it },
            valueRange = ElectrolyteCalculator.MIN_ELECTRIFICATION_CURRENT_AMPS.toFloat()..
                ElectrolyteCalculator.MAX_ELECTRIFICATION_CURRENT_AMPS.toFloat(),
            steps = CURRENT_AMP_STEPS,
            valueDisplay = stringResource(
                R.string.electrolyte_current_value,
                formatCurrent(currentAmps.toDouble()),
            ),
        )
    }

    CalculatorResultCard(title = stringResource(R.string.electrolyte_result_title)) {
        if (result == null) {
            CalculatorInvalidResultText(stringResource(R.string.electrolyte_result_invalid))
            return@CalculatorResultCard
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CalculatorResultRow(
                label = stringResource(R.string.electrolyte_result_acid_label),
                value = stringResource(
                    R.string.electrolyte_result_acid_value,
                    formatMilliliters(result.aceticAcidMl),
                    formatPercent(result.acidConcentrationPercent),
                ),
            )

            CalculatorResultRow(
                label = stringResource(R.string.electrolyte_result_water_label),
                value = stringResource(
                    R.string.electrolyte_result_water_value,
                    formatLiters(result.waterLiters),
                ),
            )

            CalculatorResultRow(
                label = stringResource(R.string.electrolyte_result_salt_label),
                value = stringResource(
                    R.string.electrolyte_result_salt_value,
                    formatGrams(result.saltG),
                ),
            )

            if (loadVoltage != null) {
                CalculatorResultRow(
                    label = stringResource(R.string.electrolyte_voltage_label),
                    value = stringResource(
                        R.string.electrolyte_voltage_value,
                        formatVoltage(loadVoltage),
                    ),
                )
            }

            val zincDissolution = result.zincDissolution
            if (zincDissolution != null) {
                CalculatorResultDivider()

                CalculatorResultRow(
                    label = stringResource(R.string.electrolyte_zinc_target_label),
                    value = stringResource(
                        R.string.electrolyte_zinc_target_value,
                        formatGrams(zincDissolution.targetDissolvedZincG),
                    ),
                )

                CalculatorResultRow(
                    label = stringResource(R.string.electrolyte_zinc_rate_label),
                    value = stringResource(
                        R.string.electrolyte_zinc_rate_value,
                        formatDecimal(zincDissolution.dissolutionRateGPerHour, 1),
                    ),
                )

                CalculatorResultRow(
                    label = stringResource(R.string.electrolyte_zinc_time_label),
                    value = formatElectrificationDuration(zincDissolution.electrificationHours),
                )

                CalculatorResultRow(
                    label = stringResource(R.string.electrolyte_zinc_power_label),
                    value = stringResource(
                        R.string.electrolyte_zinc_power_value,
                        formatDecimal(zincDissolution.powerWatts, 1),
                    ),
                )
            }
        }
    }

    Spacer(Modifier.height(8.dp))
}

@Composable
private fun formatElectrificationDuration(hours: Double): String {
    val totalMinutes = (hours * 60.0).roundToInt()
    val displayHours = totalMinutes / 60
    val displayMinutes = totalMinutes % 60
    return if (displayHours > 0) {
        stringResource(
            R.string.electrolyte_zinc_time_value_hours_minutes,
            displayHours,
            displayMinutes,
        )
    } else {
        stringResource(R.string.electrolyte_zinc_time_value_minutes, displayMinutes)
    }
}

private fun formatLiters(value: Double): String =
    String.format(Locale.getDefault(), "%.1f", value)

private fun formatMilliliters(value: Double): String =
    if (value >= 100.0) {
        String.format(Locale.getDefault(), "%.0f", value)
    } else {
        String.format(Locale.getDefault(), "%.1f", value)
    }

private fun formatGrams(value: Double): String =
    if (value >= 100.0) {
        String.format(Locale.getDefault(), "%.0f", value)
    } else {
        String.format(Locale.getDefault(), "%.1f", value)
    }

private fun formatPercent(value: Double): String =
    String.format(Locale.getDefault(), "%.0f", value.roundToInt().toDouble())

private fun formatCurrent(value: Double): String =
    String.format(Locale.getDefault(), "%.1f", value)

private fun formatVoltage(value: Double): String =
    String.format(Locale.getDefault(), "%.1f", value)

private fun formatDecimal(value: Double, decimals: Int): String =
    String.format(Locale.getDefault(), "%.${decimals}f", value)
