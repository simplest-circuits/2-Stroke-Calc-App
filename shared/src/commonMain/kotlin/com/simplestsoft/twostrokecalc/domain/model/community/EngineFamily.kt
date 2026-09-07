package com.simplestsoft.twostrokecalc.domain.model.community

import kotlinx.serialization.Serializable

@Serializable
enum class EngineOrientation {
    LIEGEND,
    STEHEND,
    UNBEKANNT,
}

/** Antriebskonzept der Motorfamilie – steuert Vario- vs. Getriebe-Felder. */
@Serializable
enum class EngineTransmissionType {
    VARIATOR,
    GEARBOX,
    UNKNOWN,
}

@Serializable
data class EngineCatalogFile(
    val version: Int = 1,
    val source: String = "",
    val entries: List<EngineFamilyEntry> = emptyList(),
)

@Serializable
data class EngineFamilyEntry(
    val id: String,
    val manufacturer: String,
    val familyName: String,
    val orientation: EngineOrientation = EngineOrientation.UNBEKANNT,
    val transmissionType: EngineTransmissionType = EngineTransmissionType.UNKNOWN,
    val coolingType: String = "",
    val typicalDisplacementCc: String = "",
    val aliasNames: List<String> = emptyList(),
    val commonVehicles: List<String> = emptyList(),
) {
    fun displayTitle(): String = listOf(manufacturer, familyName)
        .filter { it.isNotBlank() }
        .joinToString(" ")
        .ifBlank { id }

    fun matchesQuery(query: String): Boolean {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return true
        return manufacturer.lowercase().contains(q) ||
            familyName.lowercase().contains(q) ||
            aliasNames.any { it.lowercase().contains(q) } ||
            commonVehicles.any { it.lowercase().contains(q) } ||
            coolingType.lowercase().contains(q) ||
            typicalDisplacementCc.lowercase().contains(q)
    }
}
