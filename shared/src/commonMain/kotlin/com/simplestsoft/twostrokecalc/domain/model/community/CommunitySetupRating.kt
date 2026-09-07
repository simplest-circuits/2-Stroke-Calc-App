package com.simplestsoft.twostrokecalc.domain.model.community

import com.simplestsoft.twostrokecalc.platform.currentTimeMillis
import kotlinx.serialization.Serializable

@Serializable
data class CommunitySetupRating(
    val userId: String = "",
    val value: Int = 5,
    val comment: String = "",
    val createdAtMs: Long = currentTimeMillis(),
) {
    fun isValid(): Boolean {
        if (value !in 1..5) return false
        if (value <= 2 && comment.trim().length < 3) return false
        return true
    }
}
