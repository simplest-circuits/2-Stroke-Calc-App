package com.simplestsoft.twostrokecalc.data.vehicles

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.simplestsoft.twostrokecalc.data.auth.SharedAuthRepository
import com.simplestsoft.twostrokecalc.domain.model.Vehicle
import com.simplestsoft.twostrokecalc.logging.ErrorLogger
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await

@Singleton
class VehicleFirestoreSync @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
    private val cloudStorage: VehicleCloudStorage,
    private val attachmentStorage: VehicleAttachmentStorage,
    private val errorLogger: ErrorLogger,
) {
    private val syncMutex = Mutex()

    fun currentUserId(): String? = firebaseAuth.currentUser?.uid

    suspend fun syncWithCloud(localVehicles: List<Vehicle>): List<Vehicle> {
        val userId = currentUserId() ?: return localVehicles
        return syncMutex.withLock {
            runCatching { performSync(userId, localVehicles) }
                .onFailure { errorLogger.log("VehicleFirestoreSync", "syncWithCloud", it) }
                .getOrDefault(localVehicles)
        }
    }

    suspend fun pushVehicle(vehicle: Vehicle) {
        val userId = currentUserId() ?: return
        runCatching {
            uploadVehicleFiles(userId, vehicle)
            writeVehicleDocument(userId, vehicle)
        }.onFailure { errorLogger.log("VehicleFirestoreSync", "pushVehicle", it) }
    }

    suspend fun deleteVehicle(vehicleId: String) {
        val userId = currentUserId() ?: return
        runCatching {
            vehiclesCollection(userId).document(vehicleId).delete().await()
            cloudStorage.deleteVehicleFiles(userId, vehicleId)
        }.onFailure { errorLogger.log("VehicleFirestoreSync", "deleteVehicle", it) }
    }

    private suspend fun performSync(userId: String, localVehicles: List<Vehicle>): List<Vehicle> {
        val remoteVehicles = fetchRemoteVehicles(userId)
        val merged = mergeVehicles(localVehicles, remoteVehicles)
        val withFiles = merged.map { vehicle ->
            syncVehicleFiles(userId, vehicle, localVehicles, remoteVehicles)
        }
        withFiles.forEach { vehicle -> writeVehicleDocument(userId, vehicle) }
        return withFiles
    }

    private suspend fun fetchRemoteVehicles(userId: String): List<Vehicle> {
        val snapshot = vehiclesCollection(userId).get().await()
        return snapshot.documents.mapNotNull { doc ->
            VehicleFirestoreMapper.fromMap(doc.data ?: return@mapNotNull null)
        }
    }

    private fun mergeVehicles(local: List<Vehicle>, remote: List<Vehicle>): List<Vehicle> {
        return (local + remote)
            .groupBy { it.id }
            .map { (_, vehicles) -> vehicles.maxBy { it.updatedAtMs } }
    }

    private suspend fun syncVehicleFiles(
        userId: String,
        merged: Vehicle,
        localVehicles: List<Vehicle>,
        remoteVehicles: List<Vehicle>,
    ): Vehicle {
        val local = localVehicles.firstOrNull { it.id == merged.id }
        val remote = remoteVehicles.firstOrNull { it.id == merged.id }
        val useLocalFiles = when {
            local == null -> false
            remote == null -> true
            else -> local.updatedAtMs >= remote.updatedAtMs
        }

        if (useLocalFiles) {
            uploadVehicleFiles(userId, merged)
            return merged
        }

        downloadVehicleFiles(userId, merged)
        return merged
    }

    private suspend fun uploadVehicleFiles(userId: String, vehicle: Vehicle) {
        for (attachment in vehicle.attachments) {
            val localFile = attachmentStorage.attachmentFile(vehicle.id, attachment.fileName)
            if (localFile.exists()) {
                cloudStorage.uploadFile(
                    userId = userId,
                    vehicleId = vehicle.id,
                    localFile = localFile,
                    fileName = attachment.fileName,
                    mimeType = attachment.mimeType,
                    isProfileImage = false,
                )
            }
        }
        vehicle.profileImageFileName?.let { fileName ->
            val localFile = attachmentStorage.profileImageFile(vehicle.id, fileName)
            if (localFile.exists()) {
                cloudStorage.uploadFile(
                    userId = userId,
                    vehicleId = vehicle.id,
                    localFile = localFile,
                    fileName = fileName,
                    mimeType = "image/jpeg",
                    isProfileImage = true,
                )
            }
        }
    }

    private suspend fun downloadVehicleFiles(userId: String, vehicle: Vehicle) {
        for (attachment in vehicle.attachments) {
            val localFile = attachmentStorage.attachmentFile(vehicle.id, attachment.fileName)
            if (!localFile.exists()) {
                cloudStorage.downloadFile(
                    userId = userId,
                    vehicleId = vehicle.id,
                    fileName = attachment.fileName,
                    targetFile = localFile,
                    isProfileImage = false,
                )
            }
        }
        vehicle.profileImageFileName?.let { fileName ->
            val localFile = attachmentStorage.profileImageFile(vehicle.id, fileName)
            if (!localFile.exists()) {
                cloudStorage.downloadFile(
                    userId = userId,
                    vehicleId = vehicle.id,
                    fileName = fileName,
                    targetFile = localFile,
                    isProfileImage = true,
                )
            }
        }
    }

    private suspend fun writeVehicleDocument(userId: String, vehicle: Vehicle) {
        val payload = VehicleFirestoreMapper.toMap(vehicle).toMutableMap()
        payload["cloudSyncedAt"] = FieldValue.serverTimestamp()
        vehiclesCollection(userId)
            .document(vehicle.id)
            .set(payload, SetOptions.merge())
            .await()
    }

    private fun vehiclesCollection(userId: String) =
        firestore.collection(SharedAuthRepository.USERS_COLLECTION)
            .document(userId)
            .collection(VEHICLES_COLLECTION)

    companion object {
        const val VEHICLES_COLLECTION = "vehicles"
    }
}
