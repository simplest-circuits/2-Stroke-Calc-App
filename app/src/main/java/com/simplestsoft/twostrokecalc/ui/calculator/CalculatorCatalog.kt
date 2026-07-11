package com.simplestsoft.twostrokecalc.ui.calculator

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.ui.graphics.vector.ImageVector
import com.simplestsoft.twostrokecalc.R
import com.simplestsoft.twostrokecalc.domain.model.CalculatorId

val calculatorDisplayOrder: List<CalculatorId> = listOf(
    CalculatorId.TIMING,
    CalculatorId.PORT_AREA,
    CalculatorId.IGNITION,
    CalculatorId.DC_CABLE,
    CalculatorId.FLUID,
    CalculatorId.COMPRESSION,
    CalculatorId.SQUISH_BAND,
    CalculatorId.MEAN_PRESSURE,
    CalculatorId.GEAR,
    CalculatorId.EXHAUST,
    CalculatorId.COUNTERWEIGHT,
    CalculatorId.VARIATOR_WEIGHT,
    CalculatorId.FUEL_MIX,
    CalculatorId.CARB_JET,
    CalculatorId.DYNO_INERTIA,
    CalculatorId.VEHICLE_DYNAMICS,
)

fun CalculatorId.tabLabelRes(): Int = when (this) {
    CalculatorId.TIMING -> R.string.calculator_tab_timing
    CalculatorId.PORT_AREA -> R.string.calculator_tab_port_area
    CalculatorId.IGNITION -> R.string.calculator_tab_ignition
    CalculatorId.DC_CABLE -> R.string.calculator_tab_dc_cable
    CalculatorId.FLUID -> R.string.calculator_tab_fluid
    CalculatorId.FUEL_MIX -> R.string.calculator_tab_fuel_mix
    CalculatorId.CARB_JET -> R.string.calculator_tab_carb_jet
    CalculatorId.COUNTERWEIGHT -> R.string.calculator_tab_counterweight
    CalculatorId.VARIATOR_WEIGHT -> R.string.calculator_tab_variator_weight
    CalculatorId.COMPRESSION -> R.string.calculator_tab_compression
    CalculatorId.MEAN_PRESSURE -> R.string.calculator_tab_mean_pressure
    CalculatorId.SQUISH_BAND -> R.string.calculator_tab_squish_band
    CalculatorId.GEAR -> R.string.calculator_tab_gear
    CalculatorId.EXHAUST -> R.string.calculator_tab_exhaust
    CalculatorId.DYNO_INERTIA -> R.string.calculator_tab_dyno_inertia
    CalculatorId.VEHICLE_DYNAMICS -> R.string.calculator_tab_vehicle_dynamics
}

fun CalculatorId.overviewDescriptionRes(): Int = when (this) {
    CalculatorId.TIMING -> R.string.calculator_overview_timing_desc
    CalculatorId.PORT_AREA -> R.string.calculator_overview_port_area_desc
    CalculatorId.IGNITION -> R.string.calculator_overview_ignition_desc
    CalculatorId.DC_CABLE -> R.string.calculator_overview_dc_cable_desc
    CalculatorId.FLUID -> R.string.calculator_overview_fluid_desc
    CalculatorId.FUEL_MIX -> R.string.calculator_overview_fuel_mix_desc
    CalculatorId.CARB_JET -> R.string.calculator_overview_carb_jet_desc
    CalculatorId.COUNTERWEIGHT -> R.string.calculator_overview_counterweight_desc
    CalculatorId.VARIATOR_WEIGHT -> R.string.calculator_overview_variator_weight_desc
    CalculatorId.COMPRESSION -> R.string.calculator_overview_compression_desc
    CalculatorId.MEAN_PRESSURE -> R.string.calculator_overview_mean_pressure_desc
    CalculatorId.SQUISH_BAND -> R.string.calculator_overview_squish_band_desc
    CalculatorId.GEAR -> R.string.calculator_overview_gear_desc
    CalculatorId.EXHAUST -> R.string.calculator_overview_exhaust_desc
    CalculatorId.DYNO_INERTIA -> R.string.calculator_overview_dyno_inertia_desc
    CalculatorId.VEHICLE_DYNAMICS -> R.string.calculator_overview_vehicle_dynamics_desc
}

fun CalculatorId.icon(): ImageVector = when (this) {
    CalculatorId.TIMING -> Icons.AutoMirrored.Filled.ShowChart
    CalculatorId.PORT_AREA -> Icons.Default.ViewColumn
    CalculatorId.IGNITION -> Icons.Default.Bolt
    CalculatorId.DC_CABLE -> Icons.Default.Cable
    CalculatorId.FLUID -> Icons.Default.Science
    CalculatorId.FUEL_MIX -> Icons.Default.WaterDrop
    CalculatorId.CARB_JET -> Icons.Default.Air
    CalculatorId.COUNTERWEIGHT -> Icons.Default.Balance
    CalculatorId.VARIATOR_WEIGHT -> Icons.Default.Loop
    CalculatorId.COMPRESSION -> Icons.Default.Compress
    CalculatorId.MEAN_PRESSURE -> Icons.Default.Speed
    CalculatorId.SQUISH_BAND -> Icons.Default.PieChart
    CalculatorId.GEAR -> Icons.Default.Settings
    CalculatorId.EXHAUST -> Icons.Default.Tune
    CalculatorId.DYNO_INERTIA -> Icons.Default.Album
    CalculatorId.VEHICLE_DYNAMICS -> Icons.AutoMirrored.Filled.TrendingUp
}
