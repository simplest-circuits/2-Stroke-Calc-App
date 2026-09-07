package com.simplestsoft.twostrokecalc.ui.screens.community

/** Actions shown in the Tools top bar while a Community setup detail is open. */
data class CommunityDetailTopBarActions(
    val showEdit: Boolean,
    val isFavorite: Boolean,
    val onEdit: () -> Unit,
    val onFavorite: () -> Unit,
    val onShare: () -> Unit,
    val onReport: () -> Unit,
)
