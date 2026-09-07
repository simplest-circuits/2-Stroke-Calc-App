package com.simplestsoft.twostrokecalc.ui.screens

import android.widget.Toast
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.simplestsoft.twostrokecalc.R
import com.simplestsoft.twostrokecalc.domain.model.ProModuleId
import com.simplestsoft.twostrokecalc.domain.model.ToolId
import com.simplestsoft.twostrokecalc.ui.components.AppColors
import com.simplestsoft.twostrokecalc.ui.components.ProReadOnlyBanner
import com.simplestsoft.twostrokecalc.ui.components.ProUpsellDialog
import com.simplestsoft.twostrokecalc.ui.pro.LocalCalculatorEditingEnabled
import com.simplestsoft.twostrokecalc.ui.screens.community.CommunityDetailTopBarActions
import com.simplestsoft.twostrokecalc.ui.screens.community.CommunitySetupsScreen
import com.simplestsoft.twostrokecalc.ui.screens.tools.AngleMeterScreen
import com.simplestsoft.twostrokecalc.ui.screens.tools.GpsDynoScreen
import com.simplestsoft.twostrokecalc.ui.screens.tools.PortTimingAssistScreen
import com.simplestsoft.twostrokecalc.ui.screens.tools.RpmTachometerScreen
import com.simplestsoft.twostrokecalc.ui.screens.tools.ToolSessionHistoryContent
import com.simplestsoft.twostrokecalc.ui.screens.tools.ToolsOverviewScreen
import com.simplestsoft.twostrokecalc.ui.screens.tools.VibrationAnalyzerScreen
import com.simplestsoft.twostrokecalc.domain.model.ToolMeasurementSession
import androidx.compose.ui.res.stringResource
import com.simplestsoft.twostrokecalc.ui.tools.ToolsViewModel
import com.simplestsoft.twostrokecalc.ui.tools.tabLabelRes
import com.simplestsoft.twostrokecalc.ui.util.screenSystemBarPadding

@Composable
fun ToolsScreen(
    initialToolId: ToolId? = null,
    initialTargetRpm: Double? = null,
    initialCommunitySetupId: String? = null,
    isAuthenticated: Boolean = false,
    onSignInRequired: () -> Unit = {},
    onCommunitySignInRequired: () -> Unit = onSignInRequired,
    onInitialToolConsumed: () -> Unit = {},
    viewModel: ToolsViewModel = hiltViewModel(),
) {
    var selectedToolId by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(initialToolId) {
        if (initialToolId != null) {
            selectedToolId = initialToolId.name
            onInitialToolConsumed()
        }
    }
    val selectedId = selectedToolId?.let { name -> ToolId.entries.firstOrNull { it.name == name } }
    val visibleTools by viewModel.visibleDisplayOrder.collectAsStateWithLifecycle()
    val readOnlyTools by viewModel.readOnlyTools.collectAsStateWithLifecycle()
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    var proUpsellVisible by rememberSaveable { mutableStateOf(false) }
    var showSessionHistory by rememberSaveable { mutableStateOf(false) }
    var historyDetailVisible by rememberSaveable { mutableStateOf(false) }
    var historyDetailBackTicket by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(selectedId) {
        showSessionHistory = false
        historyDetailVisible = false
    }
    var communityHasNestedBack by rememberSaveable { mutableStateOf(false) }
    var communityNestedBackTicket by rememberSaveable { mutableIntStateOf(0) }
    var communityDetailTopBarActions by remember { mutableStateOf<CommunityDetailTopBarActions?>(null) }

    fun exitOrNestedBack() {
        if (historyDetailVisible) {
            historyDetailBackTicket += 1
        } else if (showSessionHistory) {
            showSessionHistory = false
        } else if (selectedId == ToolId.COMMUNITY_SETUPS && communityHasNestedBack) {
            communityNestedBackTicket += 1
        } else {
            selectedToolId = null
            showSessionHistory = false
            historyDetailVisible = false
            communityHasNestedBack = false
            communityDetailTopBarActions = null
        }
    }

    BackHandler(enabled = selectedId != null) {
        exitOrNestedBack()
    }

    if (selectedId != null) {
        val context = LocalContext.current
        val canSave = viewModel.canSave(selectedId)
        val isCommunity = selectedId == ToolId.COMMUNITY_SETUPS
        val savedMessage = stringResource(R.string.tool_session_saved)
        fun saveSession(session: ToolMeasurementSession) {
            viewModel.saveSession(session)
            Toast.makeText(context, savedMessage, Toast.LENGTH_SHORT).show()
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .screenSystemBarPadding()
                .background(AppColors.background()),
        ) {
            ToolDetailTopBar(
                toolId = selectedId,
                showHistory = showSessionHistory,
                showHistoryDetail = historyDetailVisible,
                historyVisible = !isCommunity,
                onBack = { exitOrNestedBack() },
                onToggleHistory = {
                    showSessionHistory = !showSessionHistory
                    historyDetailVisible = false
                },
                detailActions = communityDetailTopBarActions.takeIf { isCommunity },
            )
            if (!canSave && !isCommunity && !showSessionHistory) {
                ProReadOnlyBanner(
                    onBuyPro = { proUpsellVisible = true },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }
            Box(modifier = Modifier.weight(1f).padding(horizontal = if (isCommunity) 16.dp else 0.dp)) {
                if (showSessionHistory) {
                    ToolSessionHistoryContent(
                        toolId = selectedId,
                        sessions = sessions,
                        canDelete = canSave,
                        onDeleteSession = viewModel::deleteSession,
                        detailBackTicket = historyDetailBackTicket,
                        onDetailVisibleChange = { historyDetailVisible = it },
                    )
                } else {
                CompositionLocalProvider(LocalCalculatorEditingEnabled provides true) {
                    when (selectedId) {
                        ToolId.COMMUNITY_SETUPS -> CommunitySetupsScreen(
                            isAuthenticated = isAuthenticated,
                            onSignInRequired = onCommunitySignInRequired,
                            initialSetupId = initialCommunitySetupId,
                            onInitialSetupConsumed = onInitialToolConsumed,
                            onNestedBackAvailableChange = { communityHasNestedBack = it },
                            onDetailTopBarActionsChange = { communityDetailTopBarActions = it },
                            nestedBackTicket = communityNestedBackTicket,
                        )
                        ToolId.RPM_TACHOMETER -> RpmTachometerScreen(
                            canSave = canSave,
                            initialTargetRpm = initialTargetRpm,
                            onSaveSession = ::saveSession,
                            onRequestPro = { proUpsellVisible = true },
                        )
                        ToolId.PORT_TIMING_ASSIST -> PortTimingAssistScreen(
                            canSave = canSave,
                            onSaveSession = ::saveSession,
                            onRequestPro = { proUpsellVisible = true },
                        )
                        ToolId.ANGLE_METER -> AngleMeterScreen(
                            canSave = canSave,
                            onSaveSession = ::saveSession,
                            onRequestPro = { proUpsellVisible = true },
                        )
                        ToolId.VIBRATION_ANALYZER -> VibrationAnalyzerScreen(
                            canSave = canSave,
                            onSaveSession = ::saveSession,
                            onRequestPro = { proUpsellVisible = true },
                        )
                        ToolId.GPS_DYNO -> GpsDynoScreen(
                            canSave = canSave,
                            onSaveSession = ::saveSession,
                            onRequestPro = { proUpsellVisible = true },
                        )
                    }
                }
                }
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
            ToolsOverviewScreen(
                visibleTools = visibleTools,
                readOnlyTools = readOnlyTools,
                onSelectTool = { id ->
                    if (viewModel.canOpen(id)) {
                        selectedToolId = id.name
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    if (proUpsellVisible) {
        ProUpsellDialog(
            module = ProModuleId.TOOLS,
            isAuthenticated = isAuthenticated,
            onDismiss = { proUpsellVisible = false },
            onSignInRequired = {
                proUpsellVisible = false
                onSignInRequired()
            },
        )
    }
}

@Composable
private fun ToolDetailTopBar(
    toolId: ToolId,
    showHistory: Boolean,
    showHistoryDetail: Boolean,
    historyVisible: Boolean,
    onBack: () -> Unit,
    onToggleHistory: () -> Unit,
    detailActions: CommunityDetailTopBarActions? = null,
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
                contentDescription = stringResource(R.string.tools_back),
                tint = AppColors.textPrimary(),
            )
        }
        Text(
            text = when {
                showHistoryDetail -> stringResource(R.string.tool_session_detail)
                showHistory -> stringResource(R.string.tool_session_history)
                else -> stringResource(toolId.tabLabelRes())
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = AppColors.textPrimary(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (historyVisible) {
            IconButton(onClick = onToggleHistory) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = stringResource(
                        if (showHistory) {
                            R.string.tool_session_back_to_tool
                        } else {
                            R.string.tool_session_history
                        },
                    ),
                    tint = if (showHistory && !showHistoryDetail) AppColors.primaryBlue() else AppColors.textSecondary(),
                )
            }
        }
        if (detailActions != null) {
            if (detailActions.showEdit) {
                IconButton(onClick = detailActions.onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = stringResource(R.string.community_edit),
                        tint = AppColors.textSecondary(),
                    )
                }
            }
            IconButton(onClick = detailActions.onFavorite) {
                Icon(
                    if (detailActions.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = stringResource(R.string.community_segment_favorites),
                    tint = if (detailActions.isFavorite) {
                        MaterialTheme.colorScheme.error
                    } else {
                        AppColors.textSecondary()
                    },
                )
            }
            IconButton(onClick = detailActions.onShare) {
                Icon(
                    Icons.Default.Share,
                    contentDescription = stringResource(R.string.community_share),
                    tint = AppColors.textSecondary(),
                )
            }
            IconButton(onClick = detailActions.onReport) {
                Icon(
                    Icons.Default.Flag,
                    contentDescription = stringResource(R.string.community_report),
                    tint = AppColors.textSecondary(),
                )
            }
        }
    }
}
