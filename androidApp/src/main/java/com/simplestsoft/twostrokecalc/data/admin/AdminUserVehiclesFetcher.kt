package com.simplestsoft.twostrokecalc.data.admin

import com.google.firebase.firestore.FirebaseFirestore
import com.simplestsoft.twostrokecalc.domain.model.remote.AdminUserVehicleDto
import com.simplestsoft.twostrokecalc.domain.vehicles.VehicleDemoDataFactory
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await

@Singleton
class AdminUserVehiclesFetcher @Inject constructor(
    private val firestore: FirebaseFirestore,
) {
    suspend fun fetchVehicleSummaries(userId: String): List<AdminUserVehicleDto> {
        val snapshot = firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(VEHICLES_COLLECTION)
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            val data = doc.data ?: return@mapNotNull null
            val explicitId = (data["id"] as? String)?.trim().orEmpty()
            val id = explicitId.ifBlank { doc.id }
            if (VehicleDemoDataFactory.isDemoVehicle(id)) return@mapNotNull null

            AdminUserVehicleDto(
                id = id,
                name = data.stringField("name"),
                brand = data.stringField("brand"),
                model = data.stringField("model"),
                year = data.stringField("year"),
                isActive = data["isActive"] as? Boolean ?: true,
                currentOdometerKm = data.stringField("currentOdometerKm"),
                currentOperatingHours = data.stringField("currentOperatingHours"),
                updatedAtMs = data.longField("updatedAtMs"),
            )
        }.sortedWith(
            compareByDescending<AdminUserVehicleDto> { it.updatedAtMs ?: 0L }
                .thenBy { vehicleDisplayName(it) },
        )
    }

    private fun Map<String, Any?>.stringField(key: String): String? =
        (this[key] as? String)?.trim()?.takeIf { it.isNotEmpty() }

    private fun Map<String, Any?>.longField(key: String): Long? = when (val value = this[key]) {
        is Number -> value.toLong()
        is String -> value.toLongOrNull()
        else -> null
    }

    private fun vehicleDisplayName(vehicle: AdminUserVehicleDto): String =
        vehicle.name?.takeIf { it.isNotBlank() }
            ?: listOfNotNull(
                vehicle.brand?.takeIf { it.isNotBlank() },
                vehicle.model?.takeIf { it.isNotBlank() },
            ).joinToString(" ")

    companion object {
        private const val USERS_COLLECTION = "users"
        private const val VEHICLES_COLLECTION = "vehicles"
    }
}
