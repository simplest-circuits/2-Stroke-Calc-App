package com.simplestsoft.twostrokecalc.ui.screens.calculators

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.simplestsoft.twostrokecalc.R
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorHeader
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorScaffold
import com.simplestsoft.twostrokecalc.ui.theme.ContainerCornerRadius

private enum class FluidCalculatorTab {
    ELECTROLYTE,
    CLEANING_AGENT,
}

@Composable
fun FluidCalculatorScreen() {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    val selectedTab = FluidCalculatorTab.entries[selectedTabIndex]

    CalculatorScaffold {
        CalculatorHeader(
            title = stringResource(R.string.calculator_tab_fluid),
            infoTooltip = stringResource(R.string.fluid_calculator_info),
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(ContainerCornerRadius),
        ) {
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                edgePadding = 8.dp,
                divider = {},
            ) {
                Tab(
                    selected = selectedTab == FluidCalculatorTab.ELECTROLYTE,
                    onClick = { selectedTabIndex = FluidCalculatorTab.ELECTROLYTE.ordinal },
                    modifier = Modifier.height(44.dp),
                    text = { Text(stringResource(R.string.fluid_calculator_subtab_electrolyte)) },
                )
                Tab(
                    selected = selectedTab == FluidCalculatorTab.CLEANING_AGENT,
                    onClick = { selectedTabIndex = FluidCalculatorTab.CLEANING_AGENT.ordinal },
                    modifier = Modifier.height(44.dp),
                    text = { Text(stringResource(R.string.fluid_calculator_subtab_cleaning_agent)) },
                )
            }
        }

        Column {
            when (selectedTab) {
                FluidCalculatorTab.ELECTROLYTE -> ElectrolyteCalculatorContent()
                FluidCalculatorTab.CLEANING_AGENT -> CleaningAgentCalculatorContent()
            }
        }
    }
}
