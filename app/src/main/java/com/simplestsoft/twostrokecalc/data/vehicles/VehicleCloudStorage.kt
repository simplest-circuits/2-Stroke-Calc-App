package com.simplestsoft.twostrokecalc.data.vehicles

import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await

@Singleton
class VehicleCloudStorage @Inject constructor(
    private val storage: FirebaseStorage,
) {
    fun attachmentRef(userId: String, vehicleId: String, fileName: String) =
        storage.reference.child("users/$userId/vehicles/$vehicleId/attachments/$fileName")

    fun profileImageRef(userId: String, vehicleId: String, fileName: String) =
        storage.reference.child("users/$userId/vehicles/$vehicleId/profile/$fileName")

    suspend fun uploadFile(
        userId: String,
        vehicleId: String,
        localFile: File,
        fileName: String,
        mimeType: String,
        isProfileImage: Boolean,
    ) {
        if (!localFile.exists()) return
        val ref = if (isProfileImage) {
            profileImageRef(userId, vehicleId, fileName)
        } else {
            attachmentRef(userId, vehicleId, fileName)
        }
        val metadata = StorageMetadata.Builder()
            .setContentType(mimeType)
            .build()
        ref.putFile(android.net.Uri.fromFile(localFile), metadata)
            .await()
    }

    suspend fun downloadFile(
        userId: String,
        vehicleId: String,
        fileName: String,
        targetFile: File,
        isProfileImage: Boolean,
    ): Boolean {
        val ref = if (isProfileImage) {
            profileImageRef(userId, vehicleId, fileName)
        } else {
            attachmentRef(userId, vehicleId, fileName)
        }
        targetFile.parentFile?.mkdirs()
        return runCatching {
            ref.getFile(targetFile).await()
            true
        }.getOrDefault(false)
    }

    suspend fun deleteVehicleFiles(userId: String, vehicleId: String) {
        val baseRef = storage.reference.child("users/$userId/vehicles/$vehicleId")
        runCatching { deleteRecursive(baseRef) }
    }

    private suspend fun deleteRecursive(ref: com.google.firebase.storage.StorageReference) {
        val listResult = ref.listAll().await()
        for (item in listResult.items) {
            runCatching { item.delete().await() }
        }
        for (prefix in listResult.prefixes) {
            deleteRecursive(prefix)
        }
    }
}
