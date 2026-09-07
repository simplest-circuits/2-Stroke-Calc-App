package com.simplestsoft.twostrokecalc.domain.model.community

import com.simplestsoft.twostrokecalc.platform.currentTimeMillis
import com.simplestsoft.twostrokecalc.platform.randomUUID
import kotlinx.serialization.Serializable

@Serializable
data class CommunitySetupImage(
    val id: String = randomUUID(),
    val storagePath: String = "",
    val addedAtMs: Long = currentTimeMillis(),
)

@Serializable
enum class CommunitySetupLinkType {
    YOUTUBE,
    OTHER,
}

@Serializable
data class CommunitySetupLink(
    val id: String = randomUUID(),
    val type: CommunitySetupLinkType = CommunitySetupLinkType.OTHER,
    val url: String = "",
    val label: String = "",
)
