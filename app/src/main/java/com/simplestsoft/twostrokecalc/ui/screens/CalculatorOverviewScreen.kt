package com.simplestsoft.twostrokecalc.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.simplestsoft.twostrokecalc.R
import com.simplestsoft.twostrokecalc.domain.model.CalculatorId
import com.simplestsoft.twostrokecalc.ui.calculator.icon
import com.simplestsoft.twostrokecalc.ui.calculator.overviewDescriptionRes
import com.simplestsoft.twostrokecalc.ui.calculator.tabLabelRes
import com.simplestsoft.twostrokecalc.ui.components.AppColors
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorIcon
import com.simplestsoft.twostrokecalc.ui.theme.ContainerCornerRadius

private enum class CalculatorOverviewViewMode {
    LIST,
    GRID,
}

@Composable
fun CalculatorOverviewScreen(
    visibleCalculators: List<CalculatorId>,
    readOnlyCalculators: Set<CalculatorId> = emptySet(),
    onSelectCalculator: (CalculatorId) -> Unit,
    modifier: Modifier = Modifier,
    calculatorHighlightModifiers: Map<CalculatorId, Modifier> = emptyMap(),
) {
    var viewModeName by rememberSaveable { mutableStateOf(CalculatorOverviewViewMode.LIST.name) }
    val viewMode = CalculatorOverviewViewMode.valueOf(viewModeName)
    val cardShape = RoundedCornerShape(ContainerCornerRadius)

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CalculatorOverviewHeader(
            viewMode = viewMode,
            onViewModeChange = { viewModeName = it.name },
        )

        when (viewMode) {
            CalculatorOverviewViewMode.LIST ->             CalculatorOverviewList(
                visibleCalculators = visibleCalculators,
                readOnlyCalculators = readOnlyCalculators,
                cardShape = cardShape,
                onSelectCalculator = onSelectCalculator,
                calculatorHighlightModifiers = calculatorHighlightModifiers,
                modifier = Modifier.weight(1f),
            )
            CalculatorOverviewViewMode.GRID -> CalculatorOverviewGrid(
                visibleCalculators = visibleCalculators,
                readOnlyCalculators = readOnlyCalculators,
                cardShape = cardShape,
                onSelectCalculator = onSelectCalculator,
                calculatorHighlightModifiers = calculatorHighlightModifiers,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun CalculatorOverviewHeader(
    viewMode: CalculatorOverviewViewMode,
    onViewModeChange: (CalculatorOverviewViewMode) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.calculator_overview_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = AppColors.textPrimary(),
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = {
                    onViewModeChange(
                        if (viewMode == CalculatorOverviewViewMode.LIST) {
                            CalculatorOverviewViewMode.GRID
                        } else {
                            CalculatorOverviewViewMode.LIST
                        },
                    )
                },
            ) {
                Icon(
                    imageVector = when (viewMode) {
                        CalculatorOverviewViewMode.LIST -> Icons.Default.GridView
                        CalculatorOverviewViewMode.GRID -> Icons.AutoMirrored.Filled.ViewList
                    },
                    contentDescription = when (viewMode) {
                        CalculatorOverviewViewMode.LIST -> stringResource(R.string.cd_calculator_overview_grid)
                        CalculatorOverviewViewMode.GRID -> stringResource(R.string.cd_calculator_overview_list)
                    },
                    tint = AppColors.textSecondary(),
                )
            }
        }
        Text(
            text = stringResource(R.string.calculator_overview_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.textSecondary(),
        )
    }
}

@Composable
private fun CalculatorOverviewList(
    visibleCalculators: List<CalculatorId>,
    readOnlyCalculators: Set<CalculatorId>,
    cardShape: RoundedCornerShape,
    onSelectCalculator: (CalculatorId) -> Unit,
    calculatorHighlightModifiers: Map<CalculatorId, Modifier> = emptyMap(),
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        visibleCalculators.forEach { calculatorId ->
            CalculatorOverviewListCard(
                calculatorId = calculatorId,
                isReadOnly = calculatorId in readOnlyCalculators,
                shape = cardShape,
                onClick = { onSelectCalculator(calculatorId) },
                modifier = calculatorHighlightModifiers[calculatorId] ?: Modifier,
            )
        }
    }
}

@Composable
private fun CalculatorOverviewGrid(
    visibleCalculators: List<CalculatorId>,
    readOnlyCalculators: Set<CalculatorId>,
    cardShape: RoundedCornerShape,
    onSelectCalculator: (CalculatorId) -> Unit,
    calculatorHighlightModifiers: Map<CalculatorId, Modifier> = emptyMap(),
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        modifier = modifier.fillMaxWidth(),
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(visibleCalculators, key = { it.name }) { calculatorId ->
            CalculatorOverviewGridCard(
                calculatorId = calculatorId,
                isReadOnly = calculatorId in readOnlyCalculators,
                shape = cardShape,
                onClick = { onSelectCalculator(calculatorId) },
                modifier = calculatorHighlightModifiers[calculatorId] ?: Modifier,
            )
        }
    }
}

@Composable
private fun CalculatorOverviewListCard(
    calculatorId: CalculatorId,
    isReadOnly: Boolean,
    shape: RoundedCornerShape,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .then(AppColors.cardBorderModifier(shape)),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = AppColors.surface()),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CalculatorIcon(
                imageVector = calculatorId.icon(),
                contentDescription = null,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(calculatorId.tabLabelRes()),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = AppColors.textPrimary(),
                        modifier = Modifier.weight(1f),
                    )
                    if (isReadOnly) {
                        ProBadge()
                    }
                }
                Text(
                    text = stringResource(calculatorId.overviewDescriptionRes()),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.textSecondary(),
                )
            }
        }
    }
}

@Composable
private fun CalculatorOverviewGridCard(
    calculatorId: CalculatorId,
    isReadOnly: Boolean,
    shape: RoundedCornerShape,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .then(AppColors.cardBorderModifier(shape)),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = AppColors.surface()),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CalculatorIcon(
                imageVector = calculatorId.icon(),
                contentDescription = null,
            )
            if (isReadOnly) {
                ProBadge()
            }
            Text(
                text = stringResource(calculatorId.tabLabelRes()),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.textPrimary(),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ProBadge() {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = AppColors.primaryBlue().copy(alpha = 0.12f),
    ) {
        Text(
            text = stringResource(R.string.pro_badge_label),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = AppColors.primaryBlue(),
        )
    }
}
