package com.simplestsoft.twostrokecalc.domain.model.community

import com.simplestsoft.twostrokecalc.domain.model.VehicleCarbIgnitionSpecs
import com.simplestsoft.twostrokecalc.domain.model.VehicleDrivetrainSpecs
import com.simplestsoft.twostrokecalc.domain.model.VehicleEngineSpecs
import com.simplestsoft.twostrokecalc.platform.currentTimeMillis
import com.simplestsoft.twostrokecalc.platform.randomUUID
import kotlinx.serialization.Serializable

@Serializable
enum class CommunitySetupStatus {
    PENDING_REVIEW,
    PUBLISHED,
    REJECTED,
    HIDDEN,
}

@Serializable
data class CommunitySetup(
    val id: String = randomUUID(),
    val engineFamilyId: String = "",
    /** Free-text Motortyp when not chosen from the curated catalog (`engineFamilyId` = custom). */
    val engineFamilyCustomName: String = "",
    /** Snapshot of Vario vs. Getriebe; taken from catalog or set on manual engine entry. */
    val transmissionType: EngineTransmissionType = EngineTransmissionType.UNKNOWN,
    val vehicleBrand: String = "",
    val vehicleModel: String = "",
    val vehicleVariant: String = "",
    val yearFrom: Int? = null,
    val yearTo: Int? = null,
    val engine: VehicleEngineSpecs = VehicleEngineSpecs(),
    val carbIgnition: VehicleCarbIgnitionSpecs = VehicleCarbIgnitionSpecs(),
    val drivetrain: VehicleDrivetrainSpecs = VehicleDrivetrainSpecs(),
    val title: String = "",
    val experienceNotes: String = "",
    val resultSummary: String = "",
    val tags: List<String> = emptyList(),
    val authorId: String = "",
    val authorDisplayName: String = "",
    val showAuthorName: Boolean = false,
    val sourceVehicleId: String? = null,
    val status: CommunitySetupStatus = CommunitySetupStatus.PENDING_REVIEW,
    val moderatorNote: String = "",
    val ratingAverage: Double = 0.0,
    val ratingCount: Int = 0,
    val favoriteCount: Int = 0,
    val reportCount: Int = 0,
    val images: List<CommunitySetupImage> = emptyList(),
    val externalLinks: List<CommunitySetupLink> = emptyList(),
    val createdAtMs: Long = currentTimeMillis(),
    val updatedAtMs: Long = currentTimeMillis(),
) {
    companion object

    fun displayAuthor(): String =
        if (showAuthorName && authorDisplayName.isNotBlank()) authorDisplayName else ""

    fun isCriticallyRated(): Boolean =
        ratingCount >= 3 && ratingAverage < 2.5

    fun isCustomEngineFamily(): Boolean =
        engineFamilyId == CUSTOM_ENGINE_FAMILY_ID || engineFamilyCustomName.isNotBlank()

    fun engineDisplayLabel(catalogTitle: String?): String =
        engineFamilyCustomName.ifBlank { catalogTitle.orEmpty() }.ifBlank {
            if (engineFamilyId == CUSTOM_ENGINE_FAMILY_ID) "" else engineFamilyId
        }

    fun vehicleContextLabel(): String = buildList {
        if (vehicleBrand.isNotBlank()) add(vehicleBrand)
        if (vehicleModel.isNotBlank()) add(vehicleModel)
        if (vehicleVariant.isNotBlank()) add(vehicleVariant)
        when {
            yearFrom != null && yearTo != null && yearFrom == yearTo -> add(yearFrom.toString())
            yearFrom != null && yearTo != null -> add("$yearFrom–$yearTo")
            yearFrom != null -> add(yearFrom.toString())
            yearTo != null -> add(yearTo.toString())
        }
    }.joinToString(" ")
}

const val CUSTOM_ENGINE_FAMILY_ID = "custom"
