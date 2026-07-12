package com.simplestsoft.twostrokecalc.data.vehicles

import com.simplestsoft.twostrokecalc.data.auth.SharedAuthRepository
import com.simplestsoft.twostrokecalc.data.preferences.AppPreferencesStore
import com.simplestsoft.twostrokecalc.domain.model.Vehicle
import com.simplestsoft.twostrokecalc.domain.vehicles.VehicleDemoDataFactory
import com.simplestsoft.twostrokecalc.platform.currentTimeMillis
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.FieldValue
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SharedVehicleRepository(
    private val preferencesStore: AppPreferencesStore,
    private val authRepository: SharedAuthRepository,
) {
    private val persistMutex = Mutex()
    private val syncMutex = Mutex()
    private var syncCompletedForUid: String? = null

    private val _vehicles = MutableStateFlow<List<Vehicle>>(emptyList())
    val vehiclesFlow: StateFlow<List<Vehicle>> = _vehicles.asStateFlow()

    suspend fun refresh() {
        val local = loadLocal()
        val synced = syncWithCloud(local)
        _vehicles.value = synced
        saveLocal(synced)
    }

    suspend fun getVehicles(): List<Vehicle> {
        refresh()
        return _vehicles.value
    }

    suspend fun getVehicle(id: String): Vehicle? = getVehicles().firstOrNull { it.id == id }

    suspend fun saveVehicle(vehicle: Vehicle) {
        val updated = vehicle.copy(updatedAtMs = currentTimeMillis())
        persist(updated)
        authRepository.currentUserId()?.let { pushVehicle(it, updated) }
    }

    suspend fun deleteVehicle(vehicleId: String) {
        val remaining = getVehicles().filterNot { it.id == vehicleId }
        persistAll(remaining)
        authRepository.currentUserId()?.let { deleteRemote(it, vehicleId) }
    }

    suspend fun ensureFreeDemoVehicle(canEditVehicles: Boolean, demoVehiclesEnabled: Boolean = true) {
        if (canEditVehicles || !demoVehiclesEnabled) return
        if (getVehicles().isNotEmpty()) return
        persistAll(listOf(VehicleDemoDataFactory.createFreeDemoVehicle()))
    }

    suspend fun reconcileDemoVehicles(demoVehiclesEnabled: Boolean, canEditVehicles: Boolean) {
        if (!demoVehiclesEnabled) {
            val filtered = getVehicles().filterNot { VehicleDemoDataFactory.isDemoVehicle(it.id) }
            if (filtered.size != _vehicles.value.size) persistAll(filtered)
            return
        }
        ensureFreeDemoVehicle(canEditVehicles, demoVehiclesEnabled)
    }

    private suspend fun persist(vehicle: Vehicle) {
        val vehicles = _vehicles.value.toMutableList()
        val index = vehicles.indexOfFirst { it.id == vehicle.id }
        if (index >= 0) vehicles[index] = vehicle else vehicles.add(vehicle)
        persistAll(vehicles)
    }

    private suspend fun persistAll(vehicles: List<Vehicle>) {
        persistMutex.withLock {
            saveLocal(vehicles)
            _vehicles.value = vehicles
        }
    }

    private fun storageKey(userId: String?): String =
        if (userId.isNullOrBlank()) KEY_VEHICLES_GUEST else "$KEY_VEHICLES_USER_PREFIX$userId"

    private fun loadLocal(): List<Vehicle> {
        val userId = preferencesStore.getUserId()
        val guest = VehicleJsonCodec.decode(preferencesStore.getString(KEY_VEHICLES_GUEST))
        val user = VehicleJsonCodec.decode(preferencesStore.getString(storageKey(userId)))
        return if (userId.isNullOrBlank()) guest else user.ifEmpty { guest }
    }

    private fun saveLocal(vehicles: List<Vehicle>) {
        val userId = preferencesStore.getUserId()
        val key = storageKey(userId)
        preferencesStore.putString(key, VehicleJsonCodec.encode(vehicles))
        if (!userId.isNullOrBlank()) {
            preferencesStore.putString(KEY_VEHICLES_GUEST, VehicleJsonCodec.encode(emptyList()))
        }
    }

    private suspend fun syncWithCloud(localVehicles: List<Vehicle>): List<Vehicle> {
        val userId = authRepository.currentUserId() ?: return localVehicles
        if (syncCompletedForUid == userId && localVehicles.isNotEmpty()) return localVehicles
        return syncMutex.withLock {
            runCatching {
                val remote = fetchRemoteVehicles(userId)
                val merged = mergeVehicles(localVehicles, remote)
                merged.forEach { pushVehicle(userId, it) }
                syncCompletedForUid = userId
                merged
            }.getOrDefault(localVehicles)
        }
    }

    private suspend fun fetchRemoteVehicles(userId: String): List<Vehicle> {
        val snapshot = vehiclesCollection(userId).get()
        return snapshot.documents.mapNotNull { doc ->
            VehicleFirestoreMapper.fromMap(doc.data())
        }
    }

    private fun mergeVehicles(local: List<Vehicle>, remote: List<Vehicle>): List<Vehicle> =
        (local + remote)
            .groupBy { it.id }
            .map { (_, vehicles) -> vehicles.maxBy { it.updatedAtMs } }

    private suspend fun pushVehicle(userId: String, vehicle: Vehicle) {
        val payload = VehicleFirestoreMapper.toMap(vehicle).toMutableMap()
        payload["cloudSyncedAt"] = FieldValue.serverTimestamp
        vehiclesCollection(userId).document(vehicle.id).set(payload, merge = true)
    }

    private suspend fun deleteRemote(userId: String, vehicleId: String) {
        vehiclesCollection(userId).document(vehicleId).delete()
    }

    private fun vehiclesCollection(userId: String) =
        Firebase.firestore.collection(SharedAuthRepository.USERS_COLLECTION)
            .document(userId)
            .collection(VEHICLES_COLLECTION)

    companion object {
        const val VEHICLES_COLLECTION = "vehicles"
        private const val KEY_VEHICLES_GUEST = "vehicles_json_guest"
        private const val KEY_VEHICLES_USER_PREFIX = "vehicles_json_"
    }
}
