package com.simplestsoft.twostrokecalc.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.simplestsoft.twostrokecalc.domain.model.CalculatorId
import com.simplestsoft.twostrokecalc.domain.model.ProModuleId
import com.simplestsoft.twostrokecalc.ui.calculator.CalculatorViewModel
import com.simplestsoft.twostrokecalc.ui.calculator.tabLabelRes
import com.simplestsoft.twostrokecalc.ui.components.AppColors
import com.simplestsoft.twostrokecalc.ui.screens.calculators.CarbJetCorrectionCalculatorScreen
import com.simplestsoft.twostrokecalc.ui.screens.calculators.DcCableCrossSectionCalculatorScreen
import com.simplestsoft.twostrokecalc.ui.screens.calculators.FluidCalculatorScreen
import com.simplestsoft.twostrokecalc.ui.screens.calculators.CompressionCalculatorScreen
import com.simplestsoft.twostrokecalc.ui.screens.calculators.CounterweightFactorCalculatorScreen
import com.simplestsoft.twostrokecalc.ui.screens.calculators.FuelMixCalculatorScreen
import com.simplestsoft.twostrokecalc.ui.screens.calculators.GearCalculatorScreen
import com.simplestsoft.twostrokecalc.ui.screens.calculators.IgnitionTimingCalculatorScreen
import com.simplestsoft.twostrokecalc.ui.screens.calculators.MeanPressureCalculatorScreen
import com.simplestsoft.twostrokecalc.ui.screens.calculators.SquishBandCalculatorScreen
import com.simplestsoft.twostrokecalc.ui.screens.calculators.ExhaustCalculatorScreen
import com.simplestsoft.twostrokecalc.ui.screens.calculators.FlywheelInertiaCalculatorScreen
import com.simplestsoft.twostrokecalc.ui.screens.calculators.PortAreaCalculatorScreen
import com.simplestsoft.twostrokecalc.ui.screens.calculators.TimingCalculatorScreen
import com.simplestsoft.twostrokecalc.ui.screens.calculators.VariatorWeightCalculatorScreen
import com.simplestsoft.twostrokecalc.ui.screens.calculators.VehicleDynamicsCalculatorScreen
import com.simplestsoft.twostrokecalc.ui.components.ProReadOnlyBanner
import com.simplestsoft.twostrokecalc.ui.components.ProUpsellDialog
import com.simplestsoft.twostrokecalc.ui.pro.LocalCalculatorEditingEnabled
import com.simplestsoft.twostrokecalc.ui.util.screenSystemBarPadding

@Composable
fun CalculatorScreen(
    resetToOverview: Boolean = false,
    calculatorHighlightModifiers: Map<CalculatorId, Modifier> = emptyMap(),
    isAuthenticated: Boolean = false,
    onSignInRequired: () -> Unit = {},
    viewModel: CalculatorViewModel = hiltViewModel(),
) {
    var selectedCalculatorId by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedId = selectedCalculatorId?.let { name ->
        CalculatorId.entries.firstOrNull { it.name == name }
    }
    val visibleCalculators by viewModel.visibleDisplayOrder.collectAsStateWithLifecycle()
    val readOnlyCalculators by viewModel.readOnlyCalculators.collectAsStateWithLifecycle()
    var proUpsellModuleName by rememberSaveable { mutableStateOf<String?>(null) }
    val proUpsellModule = proUpsellModuleName?.let { ProModuleId.fromName(it) }

    LaunchedEffect(resetToOverview) {
        if (resetToOverview) {
            selectedCalculatorId = null
        }
    }

    BackHandler(enabled = selectedId != null) {
        selectedCalculatorId = null
    }

    if (selectedId != null) {
        val editingEnabled = selectedId !in readOnlyCalculators
        Column(
            modifier = Modifier
                .fillMaxSize()
                .screenSystemBarPadding()
                .background(AppColors.background()),
        ) {
            CalculatorDetailTopBar(
                calculatorId = selectedId,
                onBack = { selectedCalculatorId = null },
            )
            if (!editingEnabled) {
                ProReadOnlyBanner(
                    onBuyPro = {
                        proUpsellModuleName = ProModuleId.fromCalculator(selectedId).name
                    },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }
            CompositionLocalProvider(LocalCalculatorEditingEnabled provides editingEnabled) {
                CalculatorDetailContent(
                    calculatorId = selectedId,
                    onNavigateToCalculator = { target ->
                        if (viewModel.isEnabled(target)) {
                            selectedCalculatorId = target.name
                        }
                    },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .screenSystemBarPadding()
                .background(AppColors.background())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CalculatorOverviewScreen(
                visibleCalculators = visibleCalculators,
                readOnlyCalculators = readOnlyCalculators,
                onSelectCalculator = { id ->
                    if (viewModel.canOpen(id)) {
                        selectedCalculatorId = id.name
                    }
                },
                calculatorHighlightModifiers = calculatorHighlightModifiers,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    if (proUpsellModule != null) {
        ProUpsellDialog(
            module = proUpsellModule,
            isAuthenticated = isAuthenticated,
            onDismiss = { proUpsellModuleName = null },
            onSignInRequired = {
                proUpsellModuleName = null
                onSignInRequired()
            },
        )
    }
}

@Composable
private fun CalculatorDetailTopBar(
    calculatorId: CalculatorId,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(com.simplestsoft.twostrokecalc.R.string.calculator_back),
                tint = AppColors.textPrimary(),
            )
        }
        Text(
            text = stringResource(calculatorId.tabLabelRes()),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.textPrimary(),
        )
    }
}

@Composable
private fun CalculatorDetailContent(
    calculatorId: CalculatorId,
    onNavigateToCalculator: (CalculatorId) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (calculatorId) {
            CalculatorId.TIMING -> TimingCalculatorScreen()
            CalculatorId.PORT_AREA -> PortAreaCalculatorScreen(
                onNavigateToCalculator = onNavigateToCalculator,
            )
            CalculatorId.IGNITION -> IgnitionTimingCalculatorScreen()
            CalculatorId.DC_CABLE -> DcCableCrossSectionCalculatorScreen()
            CalculatorId.FLUID -> FluidCalculatorScreen()
            CalculatorId.FUEL_MIX -> FuelMixCalculatorScreen()
            CalculatorId.CARB_JET -> CarbJetCorrectionCalculatorScreen()
            CalculatorId.COUNTERWEIGHT -> CounterweightFactorCalculatorScreen()
            CalculatorId.VARIATOR_WEIGHT -> VariatorWeightCalculatorScreen()
            CalculatorId.COMPRESSION -> CompressionCalculatorScreen(
                onNavigateToCalculator = onNavigateToCalculator,
            )
            CalculatorId.MEAN_PRESSURE -> MeanPressureCalculatorScreen()
            CalculatorId.GEAR -> GearCalculatorScreen()
            CalculatorId.EXHAUST -> ExhaustCalculatorScreen()
            CalculatorId.SQUISH_BAND -> SquishBandCalculatorScreen(
                onNavigateToCalculator = onNavigateToCalculator,
            )
            CalculatorId.DYNO_INERTIA -> FlywheelInertiaCalculatorScreen()
            CalculatorId.VEHICLE_DYNAMICS -> VehicleDynamicsCalculatorScreen()
        }
    }
}
