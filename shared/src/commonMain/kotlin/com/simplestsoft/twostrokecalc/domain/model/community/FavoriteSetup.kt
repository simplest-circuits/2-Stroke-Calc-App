package com.simplestsoft.twostrokecalc.domain.model.community

import com.simplestsoft.twostrokecalc.platform.currentTimeMillis
import kotlinx.serialization.Serializable

@Serializable
data class FavoriteSetup(
    val setupId: String = "",
    val addedAtMs: Long = currentTimeMillis(),
)
