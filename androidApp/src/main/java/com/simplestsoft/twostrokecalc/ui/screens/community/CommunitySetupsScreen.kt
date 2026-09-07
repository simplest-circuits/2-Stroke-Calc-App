package com.simplestsoft.twostrokecalc.ui.screens.community

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.simplestsoft.twostrokecalc.R
import com.simplestsoft.twostrokecalc.data.community.CommunitySetupSort
import com.simplestsoft.twostrokecalc.domain.model.EngineCycleType
import com.simplestsoft.twostrokecalc.domain.model.community.CUSTOM_ENGINE_FAMILY_ID
import com.simplestsoft.twostrokecalc.domain.model.community.CommunitySetup
import com.simplestsoft.twostrokecalc.domain.model.community.CommunitySetupLinkType
import com.simplestsoft.twostrokecalc.domain.model.community.CommunitySetupStatus
import com.simplestsoft.twostrokecalc.domain.model.community.EngineFamilyEntry
import com.simplestsoft.twostrokecalc.domain.model.community.EngineOrientation
import com.simplestsoft.twostrokecalc.domain.model.community.EngineTransmissionType
import com.simplestsoft.twostrokecalc.ui.community.CommunityBrowseSegment
import com.simplestsoft.twostrokecalc.ui.community.CommunitySetupsViewModel
import com.simplestsoft.twostrokecalc.ui.components.AppColors
import com.simplestsoft.twostrokecalc.ui.components.AppOutlinedTextField
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorDecimalField
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorDropdownField
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorDropdownOption
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorFilterChip
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorFilterChipRow
import com.simplestsoft.twostrokecalc.ui.components.calculator.CalculatorSection
import com.simplestsoft.twostrokecalc.ui.theme.ContainerCornerRadius
import java.io.File
import java.io.FileOutputStream

private enum class CommunityPane {
    BROWSE,
    DETAIL,
    SUBMIT,
}

@Composable
fun CommunitySetupsScreen(
    isAuthenticated: Boolean,
    onSignInRequired: () -> Unit,
    initialSetupId: String? = null,
    onInitialSetupConsumed: () -> Unit = {},
    onNestedBackAvailableChange: (Boolean) -> Unit = {},
    onDetailTopBarActionsChange: (CommunityDetailTopBarActions?) -> Unit = {},
    nestedBackTicket: Int = 0,
    viewModel: CommunitySetupsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val setups by viewModel.filteredSetups.collectAsStateWithLifecycle()
    var pane by rememberSaveable { mutableStateOf(CommunityPane.BROWSE.name) }
    val currentPane = CommunityPane.valueOf(pane)
    val context = LocalContext.current
    val hasNestedBack = currentPane != CommunityPane.BROWSE
    val detailSetup = state.selectedSetup.takeIf { currentPane == CommunityPane.DETAIL }
    var showReportDialog by rememberSaveable { mutableStateOf(false) }
    var reportReason by rememberSaveable { mutableStateOf("") }

    fun navigateNestedBack() {
        when (currentPane) {
            CommunityPane.DETAIL -> {
                viewModel.clearSelectedSetup()
                pane = CommunityPane.BROWSE.name
            }
            CommunityPane.SUBMIT -> {
                pane = if (state.selectedSetup != null) {
                    CommunityPane.DETAIL.name
                } else {
                    CommunityPane.BROWSE.name
                }
            }
            CommunityPane.BROWSE -> Unit
        }
    }

    LaunchedEffect(initialSetupId) {
        if (initialSetupId != null) {
            viewModel.openSetup(initialSetupId)
            pane = CommunityPane.DETAIL.name
            onInitialSetupConsumed()
        }
    }

    LaunchedEffect(hasNestedBack) {
        onNestedBackAvailableChange(hasNestedBack)
    }

    LaunchedEffect(nestedBackTicket) {
        if (nestedBackTicket > 0 && hasNestedBack) {
            navigateNestedBack()
        }
    }

    val detailTopBarActions = detailSetup?.let { setup ->
        CommunityDetailTopBarActions(
            showEdit = setup.authorId == viewModel.currentUserId(),
            isFavorite = setup.id in state.favoriteIds,
            onEdit = {
                viewModel.startEditDraft(setup)
                pane = CommunityPane.SUBMIT.name
            },
            onFavorite = {
                if (!isAuthenticated) onSignInRequired() else viewModel.toggleFavorite(setup.id)
            },
            onShare = {
                val link = "twostrokecalc://community/setup/${setup.id}"
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(
                        Intent.EXTRA_TEXT,
                        context.getString(R.string.community_share_text, setup.title, link),
                    )
                }
                context.startActivity(Intent.createChooser(send, null))
            },
            onReport = {
                if (!isAuthenticated) {
                    onSignInRequired()
                } else {
                    reportReason = ""
                    showReportDialog = true
                }
            },
        )
    }

    LaunchedEffect(detailSetup?.id) {
        if (detailSetup == null) {
            showReportDialog = false
            reportReason = ""
        }
    }

    if (showReportDialog && detailSetup != null) {
        AlertDialog(
            onDismissRequest = {
                showReportDialog = false
                reportReason = ""
            },
            title = { Text(stringResource(R.string.community_report)) },
            text = {
                AppOutlinedTextField(
                    value = reportReason,
                    onValueChange = { reportReason = it },
                    label = stringResource(R.string.community_report_hint),
                    singleLine = false,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.reportSelected(reportReason.ifBlank { "report" })
                        showReportDialog = false
                        reportReason = ""
                    },
                ) {
                    Text(stringResource(R.string.community_report))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showReportDialog = false
                        reportReason = ""
                    },
                ) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
    SideEffect {
        onDetailTopBarActionsChange(detailTopBarActions)
    }
    DisposableEffect(Unit) {
        onDispose { onDetailTopBarActionsChange(null) }
    }

    BackHandler(enabled = hasNestedBack) {
        navigateNestedBack()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (currentPane) {
            CommunityPane.BROWSE -> CommunityBrowseContent(
                state = state,
                setups = setups,
                onSegment = viewModel::setSegment,
                onQuery = viewModel::setQuery,
                onSort = viewModel::setSort,
                onOrientation = viewModel::setOrientationFilter,
                onEngineFamily = viewModel::setEngineFamilyFilter,
                onOpen = { id ->
                    viewModel.openSetup(id)
                    pane = CommunityPane.DETAIL.name
                },
                onFavorite = { id ->
                    if (!isAuthenticated) onSignInRequired() else viewModel.toggleFavorite(id)
                },
                onSubmit = {
                    if (!isAuthenticated) {
                        onSignInRequired()
                    } else {
                        viewModel.startNewDraft()
                        pane = CommunityPane.SUBMIT.name
                    }
                },
                engineTitle = viewModel::engineTitle,
            )
            CommunityPane.DETAIL -> {
                val setup = state.selectedSetup
                when {
                    setup != null -> CommunityDetailContent(
                        setup = setup,
                        ratings = state.selectedRatings,
                        myRating = state.myRating,
                        engineTitle = viewModel.engineTitle(setup),
                        isOwn = setup.authorId == viewModel.currentUserId(),
                        isAuthenticated = isAuthenticated,
                        onRate = { value, comment ->
                            if (!isAuthenticated) onSignInRequired() else viewModel.submitRating(value, comment)
                        },
                        onOpenLink = { url ->
                            runCatching {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            }
                        },
                    )
                    state.loading -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                    else -> {
                        // Fetch finished without a setup (not found / error) — return to list once.
                        LaunchedEffect(state.error) {
                            pane = CommunityPane.BROWSE.name
                        }
                    }
                }
            }
            CommunityPane.SUBMIT -> CommunitySubmitContent(
                state = state,
                isEditing = viewModel.isEditingDraft(),
                engineFamilies = state.engineFamilies,
                selectedEngine = viewModel.engineFamily(state.draft.engineFamilyId),
                onStep = viewModel::setSubmitStep,
                onUpdate = viewModel::updateDraft,
                onPickVehicle = { vehicle ->
                    viewModel.startNewDraft(fromVehicle = vehicle)
                },
                onBlank = {
                    viewModel.startNewDraft(advanceToStep = 1)
                },
                onAddLink = viewModel::addLinkToDraft,
                onRemoveLink = viewModel::removeLinkFromDraft,
                onAddImage = { file, mime -> viewModel.uploadDraftImage(file, mime) },
                onRemoveImage = viewModel::removeDraftImage,
                onSubmit = {
                    viewModel.submitDraft()
                    pane = CommunityPane.BROWSE.name
                },
                onDelete = {
                    val id = state.draft.id
                    if (id.isNotBlank()) {
                        viewModel.deleteMySetup(id)
                        pane = CommunityPane.BROWSE.name
                    }
                },
            )
        }

        if (state.loading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
        state.error?.let { msg ->
            LaunchedEffect(msg) {
                // keep visible via banner below
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
    ) {
        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        state.message?.let {
            Text(it, color = AppColors.primaryBlue(), style = MaterialTheme.typography.bodySmall)
            LaunchedEffect(it) {
                kotlinx.coroutines.delay(2500)
                viewModel.clearMessage()
            }
        }
    }
}

@Composable
private fun CommunityBrowseContent(
    state: com.simplestsoft.twostrokecalc.ui.community.CommunityUiState,
    setups: List<CommunitySetup>,
    onSegment: (CommunityBrowseSegment) -> Unit,
    onQuery: (String) -> Unit,
    onSort: (CommunitySetupSort) -> Unit,
    onOrientation: (EngineOrientation?) -> Unit,
    onEngineFamily: (String?) -> Unit,
    onOpen: (String) -> Unit,
    onFavorite: (String) -> Unit,
    onSubmit: () -> Unit,
    engineTitle: (CommunitySetup) -> String,
) {
    var sortMenuOpen by remember { mutableStateOf(false) }
    var filterMenuOpen by remember { mutableStateOf(false) }
    val hasActiveFilter = state.selectedOrientation != null || state.selectedEngineFamilyId != null
    val focusManager = LocalFocusManager.current

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CalculatorFilterChipRow {
                CalculatorFilterChip(
                    label = stringResource(R.string.community_segment_all),
                    selected = state.segment == CommunityBrowseSegment.ALL,
                    onClick = { onSegment(CommunityBrowseSegment.ALL) },
                )
                CalculatorFilterChip(
                    label = stringResource(R.string.community_segment_favorites),
                    selected = state.segment == CommunityBrowseSegment.FAVORITES,
                    onClick = { onSegment(CommunityBrowseSegment.FAVORITES) },
                )
                CalculatorFilterChip(
                    label = stringResource(R.string.community_segment_mine),
                    selected = state.segment == CommunityBrowseSegment.MINE,
                    onClick = { onSegment(CommunityBrowseSegment.MINE) },
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                AppOutlinedTextField(
                    value = state.query,
                    onValueChange = onQuery,
                    label = stringResource(R.string.community_search_hint),
                    modifier = Modifier.weight(1f),
                )
                Box {
                    IconButton(onClick = { sortMenuOpen = true }) {
                        Icon(
                            Icons.Default.Sort,
                            contentDescription = stringResource(R.string.community_sort),
                            tint = AppColors.textSecondary(),
                        )
                    }
                    DropdownMenu(
                        expanded = sortMenuOpen,
                        onDismissRequest = { sortMenuOpen = false },
                    ) {
                        CommunitySetupSort.entries.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        when (option) {
                                            CommunitySetupSort.NEWEST ->
                                                stringResource(R.string.community_sort_newest)
                                            CommunitySetupSort.RATING ->
                                                stringResource(R.string.community_sort_rating)
                                            CommunitySetupSort.POPULAR ->
                                                stringResource(R.string.community_sort_popular)
                                        },
                                    )
                                },
                                onClick = {
                                    onSort(option)
                                    sortMenuOpen = false
                                },
                            )
                        }
                    }
                }
                Box {
                    IconButton(onClick = { filterMenuOpen = true }) {
                        Icon(
                            Icons.Default.FilterList,
                            contentDescription = stringResource(R.string.community_filters),
                            tint = if (hasActiveFilter) {
                                AppColors.primaryBlue()
                            } else {
                                AppColors.textSecondary()
                            },
                        )
                    }
                    DropdownMenu(
                        expanded = filterMenuOpen,
                        onDismissRequest = { filterMenuOpen = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.community_orientation_all)) },
                            onClick = {
                                onOrientation(null)
                                filterMenuOpen = false
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.community_orientation_horizontal)) },
                            onClick = {
                                onOrientation(EngineOrientation.LIEGEND)
                                filterMenuOpen = false
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.community_orientation_vertical)) },
                            onClick = {
                                onOrientation(EngineOrientation.STEHEND)
                                filterMenuOpen = false
                            },
                        )
                        if (state.selectedEngineFamilyId != null) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.community_clear_engine_filter)) },
                                onClick = {
                                    onEngineFamily(null)
                                    filterMenuOpen = false
                                },
                            )
                        }
                    }
                }
            }

            if (hasActiveFilter) {
                TextButton(
                    onClick = {
                        onOrientation(null)
                        onEngineFamily(null)
                    },
                ) {
                    Text(stringResource(R.string.community_filters_clear))
                }
            }

            if (setups.isEmpty()) {
                Text(
                    text = stringResource(R.string.community_empty),
                    color = AppColors.textSecondary(),
                    modifier = Modifier.padding(top = 24.dp),
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(bottom = 88.dp),
                ) {
                    items(setups, key = { it.id }) { setup ->
                        CommunitySetupCard(
                            setup = setup,
                            engineTitle = engineTitle(setup),
                            isFavorite = setup.id in state.favoriteIds,
                            onClick = {
                                focusManager.clearFocus()
                                onOpen(setup.id)
                            },
                            onFavorite = { onFavorite(setup.id) },
                        )
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = onSubmit,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.community_submit))
        }
    }
}

@Composable
private fun CommunitySetupCard(
    setup: CommunitySetup,
    engineTitle: String,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onFavorite: () -> Unit,
) {
    val shape = RoundedCornerShape(ContainerCornerRadius)
    val meta = listOfNotNull(
        engineTitle.takeIf { it.isNotBlank() },
        setup.vehicleContextLabel().takeIf { it.isNotBlank() },
    ).joinToString(" · ")
    val ratingText = if (setup.ratingCount > 0) {
        String.format("%.1f (%d)", setup.ratingAverage, setup.ratingCount)
    } else {
        stringResource(R.string.community_no_ratings)
    }
    val authorText = if (setup.showAuthorName && setup.authorDisplayName.isNotBlank()) {
        stringResource(R.string.community_by_author, setup.authorDisplayName)
    } else {
        stringResource(R.string.community_anonymous_author)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .then(AppColors.cardBorderModifier(shape)),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = AppColors.surface()),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = setup.title.ifBlank { stringResource(R.string.community_untitled) },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = AppColors.textPrimary(),
                )
                if (meta.isNotBlank()) {
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = AppColors.textSecondary(),
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = AppColors.primaryBlue(),
                    )
                    Text(
                        text = ratingText,
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.textSecondary(),
                        maxLines = 1,
                    )
                    if (setup.isCriticallyRated()) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = stringResource(R.string.community_critical_badge),
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                    if (setup.status != CommunitySetupStatus.PUBLISHED) {
                        Text(
                            text = communityStatusLabel(setup.status),
                            style = MaterialTheme.typography.labelSmall,
                            color = AppColors.primaryBlue(),
                            maxLines = 1,
                        )
                    }
                    Text(
                        text = authorText,
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.textSecondary(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }
            }
            IconButton(
                onClick = onFavorite,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (isFavorite) MaterialTheme.colorScheme.error else AppColors.textSecondary(),
                )
            }
        }
    }
}

@Composable
private fun CommunityDetailContent(
    setup: CommunitySetup,
    ratings: List<com.simplestsoft.twostrokecalc.domain.model.community.CommunitySetupRating>,
    myRating: com.simplestsoft.twostrokecalc.domain.model.community.CommunitySetupRating?,
    engineTitle: String,
    isOwn: Boolean,
    isAuthenticated: Boolean,
    onRate: (Int, String) -> Unit,
    onOpenLink: (String) -> Unit,
) {
    var ratingValue by remember { mutableIntStateOf(myRating?.value ?: 5) }
    var ratingComment by remember { mutableStateOf(myRating?.comment.orEmpty()) }
    val authorText = if (setup.showAuthorName && setup.authorDisplayName.isNotBlank()) {
        stringResource(R.string.community_by_author, setup.authorDisplayName)
    } else {
        stringResource(R.string.community_anonymous_author)
    }
    val ratingText = if (setup.ratingCount > 0) {
        String.format("%.1f", setup.ratingAverage)
    } else {
        "—"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .then(AppColors.cardBorderModifier(RoundedCornerShape(ContainerCornerRadius))),
            shape = RoundedCornerShape(ContainerCornerRadius),
            colors = CardDefaults.cardColors(containerColor = AppColors.surface()),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    setup.title.ifBlank { stringResource(R.string.community_untitled) },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.textPrimary(),
                )
                val context = listOfNotNull(
                    engineTitle.takeIf { it.isNotBlank() },
                    setup.vehicleContextLabel().takeIf { it.isNotBlank() },
                )
                if (context.isNotEmpty()) {
                    Text(
                        context.joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppColors.textSecondary(),
                    )
                }
                Text(
                    authorText,
                    style = MaterialTheme.typography.labelMedium,
                    color = AppColors.textSecondary(),
                )

                HorizontalDivider(color = AppColors.textSecondary().copy(alpha = 0.16f))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CommunityStat(
                        value = ratingText,
                        label = if (setup.ratingCount > 0) {
                            "${setup.ratingCount} ×"
                        } else {
                            stringResource(R.string.community_no_ratings)
                        },
                        showStar = true,
                    )
                    CommunityStat(
                        value = setup.favoriteCount.toString(),
                        label = stringResource(R.string.community_favorite_count, setup.favoriteCount)
                            .replace(setup.favoriteCount.toString(), "")
                            .trim(),
                    )
                    if (isOwn && setup.status != CommunitySetupStatus.PUBLISHED) {
                        CommunityStat(
                            value = communityStatusLabel(setup.status),
                            label = "",
                        )
                    }
                }
            }
        }

        if (setup.isCriticallyRated()) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    Text(
                        stringResource(R.string.community_critical_badge),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }

        DetailTextCard(
            title = stringResource(R.string.community_section_experience),
            body = setup.experienceNotes,
        )
        DetailTextCard(
            title = stringResource(R.string.community_section_result),
            body = setup.resultSummary,
            highlighted = true,
        )

        DetailSectionCard(
            title = stringResource(R.string.community_section_engine),
            rows = listOf(
                stringResource(R.string.community_field_cc).removeSuffix(" *") to
                    setup.engine.displacementCc.takeIf { it.isNotBlank() }?.let { "$it ccm" }.orEmpty(),
                stringResource(R.string.vehicles_field_bore) to
                    setup.engine.boreMm.takeIf { it.isNotBlank() }?.let { "$it mm" }.orEmpty(),
                stringResource(R.string.vehicles_field_stroke) to
                    setup.engine.strokeMm.takeIf { it.isNotBlank() }?.let { "$it mm" }.orEmpty(),
                stringResource(R.string.vehicles_field_compression) to setup.engine.compressionRatio,
                stringResource(R.string.community_field_exhaust) to setup.engine.exhaustSystem,
            ),
        )
        if (setup.engine.cycleType == EngineCycleType.TWO_STROKE) {
            DetailSectionCard(
                title = stringResource(R.string.community_section_ports),
                rows = listOf(
                    stringResource(R.string.community_field_transfers) to setup.engine.transferPortsNotes,
                    stringResource(R.string.community_field_exhaust_port) to setup.engine.exhaustPortNotes,
                    stringResource(R.string.community_field_intake_port) to setup.engine.intakePortNotes,
                    stringResource(R.string.community_field_port_timing) to setup.engine.portTimingNotes,
                ),
            )
        }
        DetailSectionCard(
            title = stringResource(R.string.community_section_carb),
            rows = listOf(
                stringResource(R.string.community_field_carb) to setup.carbIgnition.carbType,
                stringResource(R.string.community_field_mainjet) to setup.carbIgnition.mainJet,
                stringResource(R.string.vehicles_field_pilot_jet) to setup.carbIgnition.pilotJet,
                stringResource(R.string.vehicles_field_needle) to setup.carbIgnition.needle,
                stringResource(R.string.vehicles_field_ignition_timing) to setup.carbIgnition.ignitionTimingDeg,
                stringResource(R.string.vehicles_field_spark_plug) to setup.carbIgnition.sparkPlug,
            ),
        )
        DetailSectionCard(
            title = stringResource(R.string.community_section_drivetrain),
            rows = buildList {
                when (setup.transmissionType) {
                    EngineTransmissionType.VARIATOR -> {
                        add(stringResource(R.string.community_field_variator_brand) to setup.drivetrain.variatorBrand)
                        add(
                            stringResource(R.string.community_field_weights).removeSuffix(" (g)") to
                                setup.drivetrain.variatorWeightsG.takeIf { it.isNotBlank() }?.let { "$it g" }.orEmpty(),
                        )
                        add(stringResource(R.string.community_field_variator_belt) to setup.drivetrain.variatorBelt)
                        add(stringResource(R.string.community_field_clutch_springs) to setup.drivetrain.clutchSprings)
                    }
                    EngineTransmissionType.GEARBOX -> {
                        add(stringResource(R.string.community_field_gearing_primary) to setup.drivetrain.gearingPrimary)
                        add(stringResource(R.string.community_field_gearing_secondary) to setup.drivetrain.gearingSecondary)
                        add(stringResource(R.string.community_field_front_sprocket) to setup.drivetrain.frontSprocketTeeth)
                        add(stringResource(R.string.community_field_rear_sprocket) to setup.drivetrain.rearSprocketTeeth)
                        add(stringResource(R.string.community_field_clutch_type) to setup.drivetrain.clutchType)
                        add(stringResource(R.string.community_field_clutch_springs) to setup.drivetrain.clutchSprings)
                    }
                    EngineTransmissionType.UNKNOWN -> {
                        add(stringResource(R.string.community_field_weights) to setup.drivetrain.variatorWeightsG)
                        add(stringResource(R.string.community_field_clutch_springs) to setup.drivetrain.clutchSprings)
                        add(stringResource(R.string.community_field_gearing_primary) to setup.drivetrain.gearingPrimary)
                        add(stringResource(R.string.community_field_gearing_secondary) to setup.drivetrain.gearingSecondary)
                    }
                }
                add(stringResource(R.string.community_section_drivetrain) to setup.drivetrain.finalDriveNotes)
            },
        )

        if (setup.tags.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(AppColors.cardBorderModifier(RoundedCornerShape(ContainerCornerRadius))),
                shape = RoundedCornerShape(ContainerCornerRadius),
                colors = CardDefaults.cardColors(containerColor = AppColors.surface()),
            ) {
                Text(
                    setup.tags.joinToString("  •  "),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = AppColors.primaryBlue(),
                )
            }
        }

        if (setup.externalLinks.isNotEmpty()) {
            DetailContainer(title = stringResource(R.string.community_section_links)) {
                setup.externalLinks.forEachIndexed { index, link ->
                    TextButton(
                        onClick = { onOpenLink(link.url) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            buildString {
                                append(if (link.type == CommunitySetupLinkType.YOUTUBE) "YouTube · " else "")
                                append(link.label.ifBlank { link.url })
                            },
                            modifier = Modifier.weight(1f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                    }
                    if (index < setup.externalLinks.lastIndex) {
                        HorizontalDivider(color = AppColors.textSecondary().copy(alpha = 0.12f))
                    }
                }
            }
        }
        if (setup.images.isNotEmpty()) {
            DetailContainer(title = stringResource(R.string.community_section_images)) {
                setup.images.forEach { image ->
                    Surface(
                        color = AppColors.textSecondary().copy(alpha = 0.07f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            image.storagePath,
                            modifier = Modifier.padding(10.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.textSecondary(),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        if (!isOwn) {
            DetailContainer(title = stringResource(R.string.community_rate_title)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    (1..5).forEach { star ->
                        IconButton(onClick = { ratingValue = star }) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = if (star <= ratingValue) {
                                    AppColors.primaryBlue()
                                } else {
                                    AppColors.textSecondary().copy(alpha = 0.35f)
                                },
                            )
                        }
                    }
                }
                AppOutlinedTextField(
                    value = ratingComment,
                    onValueChange = { ratingComment = it },
                    label = stringResource(R.string.community_rate_comment),
                    singleLine = false,
                )
                Button(
                    onClick = { onRate(ratingValue, ratingComment) },
                    enabled = isAuthenticated && (ratingValue > 2 || ratingComment.trim().length >= 3),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.community_rate_submit))
                }
            }
        }

        if (ratings.isNotEmpty()) {
            DetailContainer(title = stringResource(R.string.community_ratings_list)) {
                ratings.take(20).forEachIndexed { index, rating ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Surface(
                            color = AppColors.primaryBlue().copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = AppColors.primaryBlue(),
                                )
                                Spacer(Modifier.width(3.dp))
                                Text(
                                    rating.value.toString(),
                                    fontWeight = FontWeight.Bold,
                                    color = AppColors.primaryBlue(),
                                )
                            }
                        }
                        Text(
                            rating.comment.ifBlank { "—" },
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppColors.textPrimary(),
                        )
                    }
                    if (index < ratings.take(20).lastIndex) {
                        HorizontalDivider(color = AppColors.textSecondary().copy(alpha = 0.12f))
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun CommunityStat(
    value: String,
    label: String,
    showStar: Boolean = false,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (showStar) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = AppColors.primaryBlue(),
                )
                Spacer(Modifier.width(4.dp))
            }
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = AppColors.textPrimary(),
            )
        }
        if (label.isNotBlank()) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.textSecondary(),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun DetailTextCard(
    title: String,
    body: String,
    highlighted: Boolean = false,
) {
    if (body.isBlank()) return
    val shape = RoundedCornerShape(ContainerCornerRadius)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(AppColors.cardBorderModifier(shape)),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = if (highlighted) {
                AppColors.primaryBlue().copy(alpha = 0.08f)
            } else {
                AppColors.surface()
            },
        ),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (highlighted) AppColors.primaryBlue() else AppColors.textPrimary(),
            )
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.textPrimary(),
            )
        }
    }
}

@Composable
private fun DetailSectionCard(
    title: String,
    rows: List<Pair<String, String>>,
) {
    val visibleRows = rows.filter { it.second.isNotBlank() }
    if (visibleRows.isEmpty()) return
    DetailContainer(title = title) {
        visibleRows.forEachIndexed { index, (label, value) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    label,
                    modifier = Modifier.weight(0.42f),
                    style = MaterialTheme.typography.labelMedium,
                    color = AppColors.textSecondary(),
                )
                Text(
                    value,
                    modifier = Modifier.weight(0.58f),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = AppColors.textPrimary(),
                )
            }
            if (index < visibleRows.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 5.dp),
                    color = AppColors.textSecondary().copy(alpha = 0.12f),
                )
            }
        }
    }
}

@Composable
private fun DetailContainer(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(ContainerCornerRadius)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(AppColors.cardBorderModifier(shape)),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = AppColors.surface()),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = AppColors.textPrimary(),
            )
            HorizontalDivider(color = AppColors.primaryBlue().copy(alpha = 0.35f))
            content()
        }
    }
}

@Composable
private fun communityStatusLabel(status: CommunitySetupStatus): String = when (status) {
    CommunitySetupStatus.PENDING_REVIEW -> stringResource(R.string.community_status_pending)
    CommunitySetupStatus.REJECTED -> stringResource(R.string.community_status_rejected)
    CommunitySetupStatus.HIDDEN -> stringResource(R.string.community_status_hidden)
    CommunitySetupStatus.PUBLISHED -> stringResource(R.string.community_status_published)
}

@Composable
private fun CommunitySubmitContent(
    state: com.simplestsoft.twostrokecalc.ui.community.CommunityUiState,
    isEditing: Boolean,
    engineFamilies: List<EngineFamilyEntry>,
    selectedEngine: EngineFamilyEntry?,
    onStep: (Int) -> Unit,
    onUpdate: ((CommunitySetup) -> CommunitySetup) -> Unit,
    onPickVehicle: (com.simplestsoft.twostrokecalc.domain.model.Vehicle) -> Unit,
    onBlank: () -> Unit,
    onAddLink: (String, String, Boolean) -> Unit,
    onRemoveLink: (String) -> Unit,
    onAddImage: (File, String) -> Unit,
    onRemoveImage: (String) -> Unit,
    onSubmit: () -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    val draft = state.draft
    var linkUrl by remember { mutableStateOf("") }
    var linkLabel by remember { mutableStateOf("") }
    var linkError by remember { mutableStateOf<String?>(null) }
    var stepValidationCode by remember { mutableStateOf<String?>(null) }
    var showStepErrors by remember { mutableStateOf(false) }
    var manualEngineName by rememberSaveable(draft.engineFamilyCustomName) {
        mutableStateOf(draft.engineFamilyCustomName)
    }
    val isCustomFamily = draft.engineFamilyId == CUSTOM_ENGINE_FAMILY_ID ||
        (draft.engineFamilyId.isBlank() && draft.engineFamilyCustomName.isNotBlank())
    val selectedFamilyKey = when {
        isCustomFamily -> CUSTOM_ENGINE_FAMILY_ID
        draft.engineFamilyId.isNotBlank() -> draft.engineFamilyId
        else -> ""
    }
    val noneLabel = stringResource(R.string.vehicles_engine_family_none)
    val customLabel = stringResource(R.string.community_engine_manual)
    val familyOptions = remember(engineFamilies, noneLabel, customLabel, context) {
        buildList {
            add(CalculatorDropdownOption(key = "", label = noneLabel))
            engineFamilies.forEach { entry ->
                add(
                    CalculatorDropdownOption(
                        key = entry.id,
                        label = buildString {
                            append(entry.displayTitle())
                            when (entry.transmissionType) {
                                EngineTransmissionType.VARIATOR ->
                                    append(" · ")
                                        .append(context.getString(R.string.community_transmission_variator))
                                EngineTransmissionType.GEARBOX ->
                                    append(" · ")
                                        .append(context.getString(R.string.community_transmission_gearbox))
                                EngineTransmissionType.UNKNOWN -> Unit
                            }
                        },
                    ),
                )
            }
            add(CalculatorDropdownOption(key = CUSTOM_ENGINE_FAMILY_ID, label = customLabel))
        }
    }
    val familySupportingText = when {
        selectedFamilyKey.isBlank() -> stringResource(R.string.vehicles_engine_family_hint)
        draft.transmissionType == EngineTransmissionType.VARIATOR ->
            stringResource(R.string.community_transmission_variator)
        draft.transmissionType == EngineTransmissionType.GEARBOX ->
            stringResource(R.string.community_transmission_gearbox)
        else -> null
    }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val tmp = File(context.cacheDir, "community_${System.currentTimeMillis()}.jpg")
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tmp).use { output -> input.copyTo(output) }
            }
            onAddImage(tmp, context.contentResolver.getType(uri) ?: "image/jpeg")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            stringResource(
                if (isEditing) R.string.community_edit_title else R.string.community_submit_title,
            ),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = AppColors.textPrimary(),
        )
        Text(
            stringResource(R.string.community_submit_step, state.submitStep + 1, 7),
            color = AppColors.textSecondary(),
        )
        LaunchedEffect(state.submitStep) {
            showStepErrors = false
            stepValidationCode = null
        }
        when (state.submitStep) {
            0 -> if (!isEditing) CalculatorSection(title = stringResource(R.string.community_submit_source)) {
                Button(onClick = onBlank, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.community_submit_blank))
                }
                state.vehicles.forEach { vehicle ->
                    OutlinedButton(
                        onClick = { onPickVehicle(vehicle) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(vehicle.displayTitle())
                    }
                }
            } else {
                LaunchedEffect(Unit) { onStep(1) }
            }
            1 -> {
                CalculatorSection(title = stringResource(R.string.community_pick_engine)) {
                    CalculatorDropdownField(
                        label = stringResource(R.string.community_pick_engine),
                        options = familyOptions,
                        selectedKey = selectedFamilyKey,
                        onOptionSelected = { key ->
                            when (key) {
                                "" -> {
                                    manualEngineName = ""
                                    onUpdate {
                                        it.copy(
                                            engineFamilyId = "",
                                            engineFamilyCustomName = "",
                                            transmissionType = EngineTransmissionType.UNKNOWN,
                                        )
                                    }
                                }
                                CUSTOM_ENGINE_FAMILY_ID -> {
                                    val name = manualEngineName.trim().ifBlank { draft.engineFamilyCustomName }
                                    val transmission = if (draft.transmissionType != EngineTransmissionType.UNKNOWN) {
                                        draft.transmissionType
                                    } else {
                                        EngineTransmissionType.VARIATOR
                                    }
                                    onUpdate {
                                        it.copy(
                                            engineFamilyId = CUSTOM_ENGINE_FAMILY_ID,
                                            engineFamilyCustomName = name,
                                            transmissionType = transmission,
                                        )
                                    }
                                }
                                else -> {
                                    val entry = engineFamilies.firstOrNull { it.id == key } ?: return@CalculatorDropdownField
                                    manualEngineName = ""
                                    onUpdate {
                                        it.copy(
                                            engineFamilyId = entry.id,
                                            engineFamilyCustomName = "",
                                            transmissionType = entry.transmissionType,
                                        )
                                    }
                                }
                            }
                        },
                        supportingText = familySupportingText,
                    )
                    if (isCustomFamily) {
                        AppOutlinedTextField(
                            value = manualEngineName,
                            onValueChange = { value ->
                                manualEngineName = value
                                val transmission = if (draft.transmissionType != EngineTransmissionType.UNKNOWN) {
                                    draft.transmissionType
                                } else {
                                    EngineTransmissionType.VARIATOR
                                }
                                onUpdate {
                                    it.copy(
                                        engineFamilyId = CUSTOM_ENGINE_FAMILY_ID,
                                        engineFamilyCustomName = value.trim(),
                                        transmissionType = transmission,
                                    )
                                }
                            },
                            label = stringResource(R.string.community_engine_manual),
                        )
                        CalculatorFilterChipRow {
                            CalculatorFilterChip(
                                label = stringResource(R.string.community_transmission_variator),
                                selected = draft.transmissionType == EngineTransmissionType.VARIATOR,
                                onClick = {
                                    onUpdate {
                                        it.copy(
                                            engineFamilyId = CUSTOM_ENGINE_FAMILY_ID,
                                            engineFamilyCustomName = manualEngineName.trim()
                                                .ifBlank { draft.engineFamilyCustomName },
                                            transmissionType = EngineTransmissionType.VARIATOR,
                                        )
                                    }
                                },
                            )
                            CalculatorFilterChip(
                                label = stringResource(R.string.community_transmission_gearbox),
                                selected = draft.transmissionType == EngineTransmissionType.GEARBOX,
                                onClick = {
                                    onUpdate {
                                        it.copy(
                                            engineFamilyId = CUSTOM_ENGINE_FAMILY_ID,
                                            engineFamilyCustomName = manualEngineName.trim()
                                                .ifBlank { draft.engineFamilyCustomName },
                                            transmissionType = EngineTransmissionType.GEARBOX,
                                        )
                                    }
                                },
                            )
                        }
                    }
                }
            }
            2 -> {
                val suggestions = selectedEngine?.commonVehicles.orEmpty()
                if (suggestions.isNotEmpty()) {
                    CalculatorSection(title = stringResource(R.string.community_vehicle_suggestions)) {
                        CalculatorFilterChipRow {
                            suggestions.forEach { label ->
                                CalculatorFilterChip(
                                    label = label,
                                    selected = false,
                                    onClick = {
                                        val parts = label.trim().split(Regex("\\s+"), limit = 2)
                                        onUpdate {
                                            it.copy(
                                                vehicleBrand = parts.getOrNull(0).orEmpty(),
                                                vehicleModel = parts.getOrNull(1).orEmpty(),
                                            )
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
                CalculatorSection(title = stringResource(R.string.community_vehicle_manual_hint)) {
                    AppOutlinedTextField(
                        value = draft.vehicleBrand,
                        onValueChange = { v -> onUpdate { it.copy(vehicleBrand = v) } },
                        label = stringResource(R.string.community_field_brand),
                    )
                    AppOutlinedTextField(
                        value = draft.vehicleModel,
                        onValueChange = { v -> onUpdate { it.copy(vehicleModel = v) } },
                        label = stringResource(R.string.community_field_model),
                    )
                    CalculatorDecimalField(
                        value = draft.yearFrom?.toString().orEmpty(),
                        onValueChange = { v ->
                            onUpdate { it.copy(yearFrom = v.toIntOrNull(), yearTo = v.toIntOrNull()) }
                        },
                        label = stringResource(R.string.community_field_year),
                    )
                }
            }
            3 -> {
                val transmission = draft.transmissionType
                CalculatorSection(title = stringResource(R.string.community_section_engine)) {
                    CalculatorDecimalField(
                        value = draft.engine.displacementCc,
                        onValueChange = { v ->
                            showStepErrors = false
                            onUpdate { it.copy(engine = it.engine.copy(displacementCc = v)) }
                        },
                        label = stringResource(R.string.community_field_cc),
                        suffix = "ccm",
                    )
                    CalculatorDecimalField(
                        value = draft.engine.boreMm,
                        onValueChange = { v ->
                            onUpdate { it.copy(engine = it.engine.copy(boreMm = v)) }
                        },
                        label = stringResource(R.string.vehicles_field_bore),
                        suffix = "mm",
                    )
                    CalculatorDecimalField(
                        value = draft.engine.strokeMm,
                        onValueChange = { v ->
                            onUpdate { it.copy(engine = it.engine.copy(strokeMm = v)) }
                        },
                        label = stringResource(R.string.vehicles_field_stroke),
                        suffix = "mm",
                    )
                    AppOutlinedTextField(
                        value = draft.engine.compressionRatio,
                        onValueChange = { v ->
                            onUpdate { it.copy(engine = it.engine.copy(compressionRatio = v)) }
                        },
                        label = stringResource(R.string.vehicles_field_compression),
                    )
                    AppOutlinedTextField(
                        value = draft.carbIgnition.carbType,
                        onValueChange = { v ->
                            onUpdate { it.copy(carbIgnition = it.carbIgnition.copy(carbType = v)) }
                        },
                        label = stringResource(R.string.community_field_carb),
                    )
                    AppOutlinedTextField(
                        value = draft.carbIgnition.mainJet,
                        onValueChange = { v ->
                            onUpdate { it.copy(carbIgnition = it.carbIgnition.copy(mainJet = v)) }
                        },
                        label = stringResource(R.string.community_field_mainjet),
                    )
                    AppOutlinedTextField(
                        value = draft.carbIgnition.pilotJet,
                        onValueChange = { v ->
                            onUpdate { it.copy(carbIgnition = it.carbIgnition.copy(pilotJet = v)) }
                        },
                        label = stringResource(R.string.vehicles_field_pilot_jet),
                    )
                    AppOutlinedTextField(
                        value = draft.carbIgnition.needle,
                        onValueChange = { v ->
                            onUpdate { it.copy(carbIgnition = it.carbIgnition.copy(needle = v)) }
                        },
                        label = stringResource(R.string.vehicles_field_needle),
                    )
                    AppOutlinedTextField(
                        value = draft.carbIgnition.sparkPlug,
                        onValueChange = { v ->
                            onUpdate { it.copy(carbIgnition = it.carbIgnition.copy(sparkPlug = v)) }
                        },
                        label = stringResource(R.string.vehicles_field_spark_plug),
                    )
                    AppOutlinedTextField(
                        value = draft.carbIgnition.ignitionTimingDeg,
                        onValueChange = { v ->
                            onUpdate { it.copy(carbIgnition = it.carbIgnition.copy(ignitionTimingDeg = v)) }
                        },
                        label = stringResource(R.string.vehicles_field_ignition_timing),
                    )
                    AppOutlinedTextField(
                        value = draft.engine.exhaustSystem,
                        onValueChange = { v ->
                            onUpdate { it.copy(engine = it.engine.copy(exhaustSystem = v)) }
                        },
                        label = stringResource(R.string.community_field_exhaust),
                    )
                }
                CalculatorSection(title = stringResource(R.string.community_section_ports)) {
                    AppOutlinedTextField(
                        value = draft.engine.transferPortsNotes,
                        onValueChange = { v ->
                            onUpdate { it.copy(engine = it.engine.copy(transferPortsNotes = v)) }
                        },
                        label = stringResource(R.string.community_field_transfers),
                        supportingText = stringResource(R.string.community_field_transfers_hint),
                        singleLine = false,
                    )
                    AppOutlinedTextField(
                        value = draft.engine.exhaustPortNotes,
                        onValueChange = { v ->
                            onUpdate { it.copy(engine = it.engine.copy(exhaustPortNotes = v)) }
                        },
                        label = stringResource(R.string.community_field_exhaust_port),
                        supportingText = stringResource(R.string.community_field_exhaust_port_hint),
                        singleLine = false,
                    )
                    AppOutlinedTextField(
                        value = draft.engine.intakePortNotes,
                        onValueChange = { v ->
                            onUpdate { it.copy(engine = it.engine.copy(intakePortNotes = v)) }
                        },
                        label = stringResource(R.string.community_field_intake_port),
                        singleLine = false,
                    )
                    AppOutlinedTextField(
                        value = draft.engine.portTimingNotes,
                        onValueChange = { v ->
                            onUpdate { it.copy(engine = it.engine.copy(portTimingNotes = v)) }
                        },
                        label = stringResource(R.string.community_field_port_timing),
                        supportingText = stringResource(R.string.community_field_port_timing_hint),
                        singleLine = false,
                    )
                }
                CalculatorSection(title = stringResource(R.string.community_section_drivetrain)) {
                    if (transmission == EngineTransmissionType.UNKNOWN) {
                        Text(
                            stringResource(R.string.community_transmission_pick),
                            style = MaterialTheme.typography.labelLarge,
                            color = AppColors.textSecondary(),
                        )
                        CalculatorFilterChipRow {
                            CalculatorFilterChip(
                                label = stringResource(R.string.community_transmission_variator),
                                selected = false,
                                onClick = {
                                    showStepErrors = false
                                    onUpdate { it.copy(transmissionType = EngineTransmissionType.VARIATOR) }
                                },
                            )
                            CalculatorFilterChip(
                                label = stringResource(R.string.community_transmission_gearbox),
                                selected = false,
                                onClick = {
                                    showStepErrors = false
                                    onUpdate { it.copy(transmissionType = EngineTransmissionType.GEARBOX) }
                                },
                            )
                        }
                    }
                    if (transmission == EngineTransmissionType.VARIATOR) {
                        AppOutlinedTextField(
                            value = draft.drivetrain.variatorBrand,
                            onValueChange = { v ->
                                onUpdate { it.copy(drivetrain = it.drivetrain.copy(variatorBrand = v)) }
                            },
                            label = stringResource(R.string.community_field_variator_brand),
                        )
                        CalculatorDecimalField(
                            value = draft.drivetrain.variatorWeightsG,
                            onValueChange = { v ->
                                onUpdate { it.copy(drivetrain = it.drivetrain.copy(variatorWeightsG = v)) }
                            },
                            label = stringResource(R.string.community_field_weights),
                            suffix = "g",
                        )
                        AppOutlinedTextField(
                            value = draft.drivetrain.variatorBelt,
                            onValueChange = { v ->
                                onUpdate { it.copy(drivetrain = it.drivetrain.copy(variatorBelt = v)) }
                            },
                            label = stringResource(R.string.community_field_variator_belt),
                        )
                        AppOutlinedTextField(
                            value = draft.drivetrain.clutchSprings,
                            onValueChange = { v ->
                                onUpdate { it.copy(drivetrain = it.drivetrain.copy(clutchSprings = v)) }
                            },
                            label = stringResource(R.string.community_field_clutch_springs),
                        )
                    }
                    if (transmission == EngineTransmissionType.GEARBOX) {
                        AppOutlinedTextField(
                            value = draft.drivetrain.gearingPrimary,
                            onValueChange = { v ->
                                onUpdate { it.copy(drivetrain = it.drivetrain.copy(gearingPrimary = v)) }
                            },
                            label = stringResource(R.string.community_field_gearing_primary),
                        )
                        AppOutlinedTextField(
                            value = draft.drivetrain.gearingSecondary,
                            onValueChange = { v ->
                                onUpdate { it.copy(drivetrain = it.drivetrain.copy(gearingSecondary = v)) }
                            },
                            label = stringResource(R.string.community_field_gearing_secondary),
                        )
                        CalculatorDecimalField(
                            value = draft.drivetrain.frontSprocketTeeth,
                            onValueChange = { v ->
                                onUpdate { it.copy(drivetrain = it.drivetrain.copy(frontSprocketTeeth = v)) }
                            },
                            label = stringResource(R.string.community_field_front_sprocket),
                        )
                        CalculatorDecimalField(
                            value = draft.drivetrain.rearSprocketTeeth,
                            onValueChange = { v ->
                                onUpdate { it.copy(drivetrain = it.drivetrain.copy(rearSprocketTeeth = v)) }
                            },
                            label = stringResource(R.string.community_field_rear_sprocket),
                        )
                        AppOutlinedTextField(
                            value = draft.drivetrain.clutchType,
                            onValueChange = { v ->
                                onUpdate { it.copy(drivetrain = it.drivetrain.copy(clutchType = v)) }
                            },
                            label = stringResource(R.string.community_field_clutch_type),
                        )
                        AppOutlinedTextField(
                            value = draft.drivetrain.clutchSprings,
                            onValueChange = { v ->
                                onUpdate { it.copy(drivetrain = it.drivetrain.copy(clutchSprings = v)) }
                            },
                            label = stringResource(R.string.community_field_clutch_springs),
                        )
                    }
                }
            }
            4 -> CalculatorSection(title = stringResource(R.string.community_field_title)) {
                AppOutlinedTextField(
                    value = draft.title,
                    onValueChange = { v ->
                        showStepErrors = false
                        onUpdate { it.copy(title = v) }
                    },
                    label = stringResource(R.string.community_field_title),
                    isError = showStepErrors && draft.title.isBlank(),
                )
                AppOutlinedTextField(
                    value = draft.experienceNotes,
                    onValueChange = { v ->
                        showStepErrors = false
                        onUpdate { it.copy(experienceNotes = v) }
                    },
                    label = stringResource(R.string.community_field_experience),
                    singleLine = false,
                    minLines = 3,
                    isError = showStepErrors && draft.experienceNotes.isBlank(),
                )
                AppOutlinedTextField(
                    value = draft.resultSummary,
                    onValueChange = { v -> onUpdate { it.copy(resultSummary = v) } },
                    label = stringResource(R.string.community_field_result),
                    singleLine = false,
                )
                AppOutlinedTextField(
                    value = draft.tags.joinToString(", "),
                    onValueChange = { v ->
                        onUpdate {
                            it.copy(tags = v.split(',').map { t -> t.trim() }.filter { t -> t.isNotEmpty() })
                        }
                    },
                    label = stringResource(R.string.community_field_tags),
                )
            }
            5 -> CalculatorSection(title = stringResource(R.string.community_section_links)) {
                Text(
                    stringResource(R.string.community_media_links_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.textSecondary(),
                )
                AppOutlinedTextField(
                    value = linkUrl,
                    onValueChange = {
                        linkUrl = it
                        linkError = null
                    },
                    label = stringResource(R.string.community_field_link_url),
                    isError = linkError != null,
                    supportingText = linkError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                )
                AppOutlinedTextField(
                    value = linkLabel,
                    onValueChange = { linkLabel = it },
                    label = stringResource(R.string.community_field_link_label),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            if (linkUrl.isBlank()) {
                                linkError = context.getString(R.string.community_link_url_required)
                                return@OutlinedButton
                            }
                            onAddLink(linkUrl, linkLabel, true)
                            linkUrl = ""
                            linkLabel = ""
                            linkError = null
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text("YouTube") }
                    OutlinedButton(
                        onClick = {
                            if (linkUrl.isBlank()) {
                                linkError = context.getString(R.string.community_link_url_required)
                                return@OutlinedButton
                            }
                            onAddLink(linkUrl, linkLabel, false)
                            linkUrl = ""
                            linkLabel = ""
                            linkError = null
                        },
                        modifier = Modifier.weight(1f),
                    ) { Text(stringResource(R.string.community_add_link)) }
                }
                draft.externalLinks.forEach { link ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            buildString {
                                append(link.label.ifBlank { link.url })
                                if (link.label.isNotBlank()) {
                                    append(" · ")
                                    append(link.url)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppColors.textPrimary(),
                        )
                        TextButton(onClick = { onRemoveLink(link.id) }) {
                            Text(stringResource(R.string.community_remove))
                        }
                    }
                }
                OutlinedButton(
                    onClick = { imagePicker.launch("image/*") },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.community_add_image))
                }
                draft.images.forEach { image ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            image.storagePath,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall,
                            color = AppColors.textSecondary(),
                        )
                        TextButton(onClick = { onRemoveImage(image.id) }) {
                            Text(stringResource(R.string.community_remove))
                        }
                    }
                }
            }
            6 -> CalculatorSection(title = stringResource(R.string.community_show_name)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = draft.showAuthorName,
                        onCheckedChange = { checked -> onUpdate { it.copy(showAuthorName = checked) } },
                    )
                    Text(stringResource(R.string.community_show_name))
                }
                Text(
                    stringResource(R.string.community_disclaimer),
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.textSecondary(),
                )
            }
        }
        if (state.submitStep > 0) {
            val isLastStep = state.submitStep >= 6
            val stepValid = draft.isSubmitStepValid(state.submitStep)
            if (showStepErrors && !stepValid) {
                Text(
                    text = when (stepValidationCode ?: draft.submitStepValidationCode(state.submitStep)) {
                        "engine" -> stringResource(R.string.community_validation_engine)
                        "transmission" -> stringResource(R.string.community_validation_transmission)
                        else -> stringResource(R.string.community_validation_required)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            CommunitySubmitNavRow(
                showBack = state.submitStep > if (isEditing) 1 else 0,
                primaryLabel = if (isLastStep) {
                    stringResource(
                        if (isEditing) R.string.community_submit_update else R.string.community_submit_send,
                    )
                } else {
                    stringResource(R.string.community_next)
                },
                showForwardIcon = !isLastStep,
                primaryEnabled = true,
                onBack = {
                    val minStep = if (isEditing) 1 else 0
                    onStep((state.submitStep - 1).coerceAtLeast(minStep))
                },
                onPrimary = {
                    val code = draft.submitStepValidationCode(state.submitStep)
                    if (code != null) {
                        showStepErrors = true
                        stepValidationCode = code
                        return@CommunitySubmitNavRow
                    }
                    showStepErrors = false
                    stepValidationCode = null
                    if (isLastStep) onSubmit() else onStep(state.submitStep + 1)
                },
            )
        }
        if (isEditing && state.submitStep == 1) {
            OutlinedButton(
                onClick = onDelete,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
            ) {
                Text(stringResource(R.string.community_delete))
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

private fun CommunitySetup.isSubmitStepValid(step: Int): Boolean =
    submitStepValidationCode(step) == null

private fun CommunitySetup.submitStepValidationCode(step: Int): String? = when (step) {
    1 -> {
        when {
            engineFamilyId.isBlank() -> "engine"
            engineFamilyId == CUSTOM_ENGINE_FAMILY_ID && engineFamilyCustomName.isBlank() -> "engine"
            transmissionType == EngineTransmissionType.UNKNOWN &&
                engineFamilyId == CUSTOM_ENGINE_FAMILY_ID -> "transmission"
            else -> null
        }
    }
    2 -> null
    3 -> {
        when {
            transmissionType == EngineTransmissionType.UNKNOWN -> "transmission"
            engine.displacementCc.isBlank() -> "required"
            else -> null
        }
    }
    4 -> {
        when {
            title.isBlank() || experienceNotes.isBlank() -> "required"
            else -> null
        }
    }
    else -> null
}

@Composable
private fun CommunitySubmitNavRow(
    showBack: Boolean,
    primaryLabel: String,
    showForwardIcon: Boolean,
    primaryEnabled: Boolean,
    onBack: () -> Unit,
    onPrimary: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showBack) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.community_back_step))
            }
        }
        Button(
            onClick = onPrimary,
            enabled = primaryEnabled,
            modifier = Modifier.weight(1f),
        ) {
            Text(primaryLabel)
            if (showForwardIcon) {
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
