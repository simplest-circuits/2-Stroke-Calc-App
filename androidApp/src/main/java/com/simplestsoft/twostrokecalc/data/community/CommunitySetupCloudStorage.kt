package com.simplestsoft.twostrokecalc.data.community

import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await

@Singleton
class CommunitySetupCloudStorage @Inject constructor(
    private val storage: FirebaseStorage,
) {
    fun imageRef(setupId: String, fileName: String) =
        storage.reference.child("communitySetups/$setupId/images/$fileName")

    suspend fun uploadImage(
        setupId: String,
        localFile: File,
        fileName: String,
        mimeType: String,
    ): String {
        val ref = imageRef(setupId, fileName)
        val metadata = StorageMetadata.Builder()
            .setContentType(mimeType)
            .build()
        ref.putFile(android.net.Uri.fromFile(localFile), metadata).await()
        return ref.path
    }

    suspend fun downloadUrl(storagePath: String): String? = runCatching {
        storage.reference.child(storagePath.trimStart('/')).downloadUrl.await().toString()
    }.getOrNull()
}
