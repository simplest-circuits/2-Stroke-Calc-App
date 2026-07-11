package com.simplestsoft.twostrokecalc.data.vehicles

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.simplestsoft.twostrokecalc.data.auth.AuthRepository
import com.simplestsoft.twostrokecalc.domain.model.Vehicle
import com.simplestsoft.twostrokecalc.domain.model.VehicleAttachment
import com.simplestsoft.twostrokecalc.domain.vehicles.VehicleDemoDataFactory
import com.simplestsoft.twostrokecalc.notifications.MaintenanceReminderScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private val Context.vehiclesDataStore: DataStore<Preferences> by preferencesDataStore(name = "vehicles_store")

@Singleton
class VehicleRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson,
    private val attachmentStorage: VehicleAttachmentStorage,
    private val maintenanceReminderScheduler: MaintenanceReminderScheduler,
    private val firestoreSync: VehicleFirestoreSync,
    private val firebaseAuth: FirebaseAuth,
    authRepository: AuthRepository,
) {
    private val dataStore get() = context.vehiclesDataStore
    private val persistMutex = Mutex()
    private var syncCompletedForUid: String? = null

    private object Keys {
        val VEHICLES_JSON_GUEST = stringPreferencesKey("vehicles_json_guest")
        fun vehiclesJsonForUser(userId: String) = stringPreferencesKey("vehicles_json_$userId")
    }

    val vehiclesFlow: Flow<List<Vehicle>> = combine(
        dataStore.data,
        authRepository.authState,
    ) { prefs, user ->
        decodeVehicles(prefs[storageKeyForUser(user?.uid)])
    }

    suspend fun getVehicles(): List<Vehicle> = vehiclesFlow.first()

    suspend fun getVehicle(id: String): Vehicle? = getVehicles().firstOrNull { it.id == id }

    suspend fun ensureFreeDemoVehicle(canEditVehicles: Boolean) {
        if (canEditVehicles) return
        val vehicles = getVehicles()
        if (vehicles.isNotEmpty()) return
        persist(listOf(VehicleDemoDataFactory.createFreeDemoVehicle()))
    }

    suspend fun seedAdminDemoVehicles(): AdminDemoSeedResult {
        val demos = VehicleDemoDataFactory.createDemoVehicles()
        val vehicles = getVehicles().toMutableList()
        var created = 0
        var updated = 0
        for (demo in demos) {
            val index = vehicles.indexOfFirst { it.id == demo.id }
            if (index >= 0) {
                vehicles[index] = demo
                updated++
            } else {
                vehicles.add(demo)
                created++
            }
        }
        persist(vehicles)
        return AdminDemoSeedResult(created = created, updated = updated)
    }

    data class AdminDemoSeedResult(
        val created: Int,
        val updated: Int,
    ) {
        val total: Int get() = created + updated
    }

    suspend fun syncOnLogin() {
        val uid = firebaseAuth.currentUser?.uid ?: return
        if (syncCompletedForUid == uid) return
        persistMutex.withLock {
            if (syncCompletedForUid == uid) return
            val prefs = dataStore.data.first()
            val userKey = Keys.vehiclesJsonForUser(uid)
            var local = decodeVehicles(prefs[userKey])
            if (local.isEmpty()) {
                val guestVehicles = decodeVehicles(prefs[Keys.VEHICLES_JSON_GUEST])
                if (guestVehicles.isNotEmpty()) {
                    local = guestVehicles
                }
            }
            val merged = firestoreSync.syncWithCloud(local)
            persistLocal(merged, skipCloudPush = true)
            syncCompletedForUid = uid
        }
    }

    fun resetSyncState() {
        syncCompletedForUid = null
    }

    suspend fun saveVehicle(vehicle: Vehicle) {
        val now = System.currentTimeMillis()
        val updated = vehicle.copy(updatedAtMs = now)
        val vehicles = getVehicles().toMutableList()
        val index = vehicles.indexOfFirst { it.id == updated.id }
        if (index >= 0) {
            vehicles[index] = updated
        } else {
            vehicles.add(updated)
        }
        persist(vehicles)
    }

    suspend fun deleteVehicle(id: String) {
        attachmentStorage.deleteAllForVehicle(id)
        persist(getVehicles().filterNot { it.id == id })
        firestoreSync.deleteVehicle(id)
    }

    suspend fun addAttachment(vehicleId: String, sourceUri: Uri): VehicleAttachment? {
        val vehicle = getVehicle(vehicleId) ?: return null
        val attachment = attachmentStorage.importAttachment(vehicleId, sourceUri) ?: return null
        saveVehicle(vehicle.copy(attachments = vehicle.attachments + attachment))
        return attachment
    }

    suspend fun removeAttachment(vehicleId: String, attachmentId: String) {
        val vehicle = getVehicle(vehicleId) ?: return
        val attachment = vehicle.attachments.firstOrNull { it.id == attachmentId } ?: return
        attachmentStorage.deleteAttachment(vehicleId, attachment.fileName)
        saveVehicle(vehicle.copy(attachments = vehicle.attachments.filterNot { it.id == attachmentId }))
    }

    suspend fun updateAttachment(vehicleId: String, attachment: VehicleAttachment) {
        val vehicle = getVehicle(vehicleId) ?: return
        saveVehicle(
            vehicle.copy(
                attachments = vehicle.attachments.map {
                    if (it.id == attachment.id) attachment else it
                },
            ),
        )
    }

    fun getAttachmentFile(vehicleId: String, fileName: String) =
        attachmentStorage.attachmentFile(vehicleId, fileName)

    fun getAttachmentShareUri(vehicleId: String, fileName: String) =
        attachmentStorage.shareUri(attachmentStorage.attachmentFile(vehicleId, fileName))

    suspend fun setProfileImage(vehicleId: String, sourceUri: Uri): String? {
        val vehicle = getVehicle(vehicleId) ?: return null
        val fileName = attachmentStorage.importProfileImage(
            vehicleId = vehicleId,
            sourceUri = sourceUri,
            existingFileName = vehicle.profileImageFileName,
        ) ?: return null
        saveVehicle(vehicle.copy(profileImageFileName = fileName))
        return fileName
    }

    suspend fun clearProfileImage(vehicleId: String) {
        val vehicle = getVehicle(vehicleId) ?: return
        vehicle.profileImageFileName?.let {
            attachmentStorage.deleteAttachment(vehicleId, it)
        }
        saveVehicle(vehicle.copy(profileImageFileName = null))
    }

    fun getProfileImageFile(vehicleId: String, fileName: String?) =
        fileName?.let { attachmentStorage.profileImageFile(vehicleId, it) }

    private suspend fun persist(vehicles: List<Vehicle>) {
        persistMutex.withLock {
            persistLocal(vehicles, skipCloudPush = false)
        }
    }

    private suspend fun persistLocal(vehicles: List<Vehicle>, skipCloudPush: Boolean) {
        val sorted = vehicles.sortedWith(
            compareByDescending<Vehicle> { it.isActive }
                .thenByDescending { it.updatedAtMs }
                .thenBy { it.displayTitle().lowercase() },
        )
        dataStore.edit {
            it[currentStorageKey()] = gson.toJson(sorted)
        }
        maintenanceReminderScheduler.runCheckSoon()
        if (!skipCloudPush && firebaseAuth.currentUser != null) {
            sorted.forEach { vehicle -> firestoreSync.pushVehicle(vehicle) }
        }
    }

    private fun storageKeyForUser(userId: String?): Preferences.Key<String> =
        if (userId != null) Keys.vehiclesJsonForUser(userId) else Keys.VEHICLES_JSON_GUEST

    private fun currentStorageKey(): Preferences.Key<String> =
        storageKeyForUser(firebaseAuth.currentUser?.uid)

    private fun decodeVehicles(json: String?): List<Vehicle> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            val type = object : TypeToken<List<Vehicle>>() {}.type
            gson.fromJson<List<Vehicle>>(json, type).map { it.withLegacyMigration() }
        }.getOrDefault(emptyList())
    }
}
