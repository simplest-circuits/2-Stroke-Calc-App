package com.simplestsoft.twostrokecalc.domain.model

import com.simplestsoft.twostrokecalc.platform.currentTimeMillis
import com.simplestsoft.twostrokecalc.platform.randomUUID
import kotlinx.serialization.Serializable

@Serializable
data class ToolMeasurementSession(
    val id: String = randomUUID(),
    val toolId: ToolId,
    val vehicleId: String? = null,
    val createdAt: Long = currentTimeMillis(),
    val title: String = "",
    val notes: String = "",
    val resultJson: String = "",
    val rawDataPath: String? = null,
)
