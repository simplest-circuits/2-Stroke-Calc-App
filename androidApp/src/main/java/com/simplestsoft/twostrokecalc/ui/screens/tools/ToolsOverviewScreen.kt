package com.simplestsoft.twostrokecalc.ui.screens.tools

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simplestsoft.twostrokecalc.R
import com.simplestsoft.twostrokecalc.domain.model.ToolId
import com.simplestsoft.twostrokecalc.ui.components.AppColors
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorIcon
import com.simplestsoft.twostrokecalc.ui.theme.ContainerCornerRadius
import com.simplestsoft.twostrokecalc.ui.tools.icon
import com.simplestsoft.twostrokecalc.ui.tools.overviewDescriptionRes
import com.simplestsoft.twostrokecalc.ui.tools.tabLabelRes

private enum class ToolsOverviewViewMode {
    LIST,
    GRID,
}

@Composable
fun ToolsOverviewScreen(
    visibleTools: List<ToolId>,
    readOnlyTools: Set<ToolId> = emptySet(),
    onSelectTool: (ToolId) -> Unit,
    modifier: Modifier = Modifier,
) {
    var viewModeName by rememberSaveable { mutableStateOf(ToolsOverviewViewMode.LIST.name) }
    val viewMode = ToolsOverviewViewMode.valueOf(viewModeName)
    val cardShape = RoundedCornerShape(ContainerCornerRadius)

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ToolsOverviewHeader(
            viewMode = viewMode,
            onViewModeChange = { viewModeName = it.name },
        )

        when (viewMode) {
            ToolsOverviewViewMode.LIST -> ToolsOverviewList(
                visibleTools = visibleTools,
                readOnlyTools = readOnlyTools,
                cardShape = cardShape,
                onSelectTool = onSelectTool,
                modifier = Modifier.weight(1f),
            )
            ToolsOverviewViewMode.GRID -> ToolsOverviewGrid(
                visibleTools = visibleTools,
                readOnlyTools = readOnlyTools,
                cardShape = cardShape,
                onSelectTool = onSelectTool,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ToolsOverviewHeader(
    viewMode: ToolsOverviewViewMode,
    onViewModeChange: (ToolsOverviewViewMode) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.tools_overview_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = AppColors.textPrimary(),
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = {
                    onViewModeChange(
                        if (viewMode == ToolsOverviewViewMode.LIST) {
                            ToolsOverviewViewMode.GRID
                        } else {
                            ToolsOverviewViewMode.LIST
                        },
                    )
                },
            ) {
                Icon(
                    imageVector = when (viewMode) {
                        ToolsOverviewViewMode.LIST -> Icons.Default.GridView
                        ToolsOverviewViewMode.GRID -> Icons.AutoMirrored.Filled.ViewList
                    },
                    contentDescription = null,
                    tint = AppColors.textSecondary(),
                )
            }
        }
        Text(
            text = stringResource(R.string.tools_overview_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.textSecondary(),
        )
    }
}

@Composable
private fun ToolsOverviewList(
    visibleTools: List<ToolId>,
    readOnlyTools: Set<ToolId>,
    cardShape: RoundedCornerShape,
    onSelectTool: (ToolId) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        visibleTools.forEach { toolId ->
            ToolsOverviewListCard(
                toolId = toolId,
                isReadOnly = toolId in readOnlyTools,
                shape = cardShape,
                onClick = { onSelectTool(toolId) },
            )
        }
    }
}

@Composable
private fun ToolsOverviewGrid(
    visibleTools: List<ToolId>,
    readOnlyTools: Set<ToolId>,
    cardShape: RoundedCornerShape,
    onSelectTool: (ToolId) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        modifier = modifier.fillMaxWidth(),
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(visibleTools, key = { it.name }) { toolId ->
            ToolsOverviewGridCard(
                toolId = toolId,
                isReadOnly = toolId in readOnlyTools,
                shape = cardShape,
                onClick = { onSelectTool(toolId) },
            )
        }
    }
}

@Composable
private fun ToolsOverviewListCard(
    toolId: ToolId,
    isReadOnly: Boolean,
    shape: RoundedCornerShape,
    onClick: () -> Unit,
) {
    ToolCardWithBetaRibbon(
        showBeta = toolId.isBetaTool,
        shape = shape,
        onClick = onClick,
    ) {
        val endPadding = if (toolId.isBetaTool) 28.dp else 16.dp
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 16.dp, end = endPadding, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CalculatorIcon(
                imageVector = toolId.icon(),
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
                        text = stringResource(toolId.tabLabelRes()),
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
                    text = stringResource(toolId.overviewDescriptionRes()),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.textSecondary(),
                )
            }
        }
    }
}

@Composable
private fun ToolsOverviewGridCard(
    toolId: ToolId,
    isReadOnly: Boolean,
    shape: RoundedCornerShape,
    onClick: () -> Unit,
) {
    ToolCardWithBetaRibbon(
        showBeta = toolId.isBetaTool,
        shape = shape,
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, top = 20.dp, end = 12.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CalculatorIcon(
                imageVector = toolId.icon(),
                contentDescription = null,
            )
            if (isReadOnly) {
                ProBadge()
            }
            Text(
                text = stringResource(toolId.tabLabelRes()),
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

private val ToolId.isBetaTool: Boolean
    get() = this != ToolId.COMMUNITY_SETUPS

@Composable
private fun ToolCardWithBetaRibbon(
    showBeta: Boolean,
    shape: RoundedCornerShape,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .then(AppColors.cardBorderModifier(shape)),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = AppColors.surface()),
    ) {
        Box(modifier = Modifier.clip(shape)) {
            content()
            if (showBeta) {
                Box(modifier = Modifier.matchParentSize()) {
                    BetaCornerRibbon(modifier = Modifier.align(Alignment.TopEnd))
                }
            }
        }
    }
}

@Composable
private fun BetaCornerRibbon(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.tools_beta_badge).uppercase(),
        color = MaterialTheme.colorScheme.onPrimary,
        fontSize = 8.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
        maxLines = 1,
        modifier = modifier
            .offset(x = 20.dp, y = 4.dp)
            .rotate(45f)
            .background(AppColors.primaryBlue())
            .padding(horizontal = 28.dp, vertical = 2.dp),
    )
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
