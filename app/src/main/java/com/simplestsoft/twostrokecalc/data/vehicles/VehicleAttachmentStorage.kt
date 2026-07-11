package com.simplestsoft.twostrokecalc.data.vehicles

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import com.simplestsoft.twostrokecalc.domain.model.VehicleAttachment
import com.simplestsoft.twostrokecalc.domain.model.VehicleAttachmentCategory
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VehicleAttachmentStorage @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun importAttachment(vehicleId: String, sourceUri: Uri): VehicleAttachment? {
        val resolver = context.contentResolver
        val originalName = queryDisplayName(sourceUri) ?: "document"
        val mimeType = resolver.getType(sourceUri) ?: guessMimeType(originalName)
        val extension = extensionFromMimeOrName(mimeType, originalName)
        val storedName = "${UUID.randomUUID()}.$extension"
        val targetFile = attachmentFile(vehicleId, storedName)

        targetFile.parentFile?.mkdirs()
        resolver.openInputStream(sourceUri)?.use { input ->
            targetFile.outputStream().use { output -> input.copyTo(output) }
        } ?: return null

        return VehicleAttachment(
            fileName = storedName,
            displayName = originalName,
            mimeType = mimeType,
            category = guessCategory(originalName),
            fileSizeBytes = targetFile.length(),
        )
    }

    fun attachmentFile(vehicleId: String, fileName: String): File =
        File(vehicleDirectory(vehicleId), fileName)

    fun attachmentExists(vehicleId: String, fileName: String): Boolean =
        attachmentFile(vehicleId, fileName).exists()

    fun deleteAttachment(vehicleId: String, fileName: String) {
        attachmentFile(vehicleId, fileName).delete()
    }

    fun deleteAllForVehicle(vehicleId: String) {
        vehicleDirectory(vehicleId).deleteRecursively()
    }

    fun importProfileImage(vehicleId: String, sourceUri: Uri, existingFileName: String?): String? {
        val resolver = context.contentResolver
        val mimeType = resolver.getType(sourceUri) ?: guessMimeType("image.jpg")
        if (!mimeType.startsWith("image/")) return null

        existingFileName?.let { deleteAttachment(vehicleId, it) }
        val extension = extensionFromMimeOrName(mimeType, "image.jpg")
        val storedName = "profile.$extension"
        val targetFile = attachmentFile(vehicleId, storedName)

        targetFile.parentFile?.mkdirs()
        resolver.openInputStream(sourceUri)?.use { input ->
            targetFile.outputStream().use { output -> input.copyTo(output) }
        } ?: return null

        return storedName
    }

    fun profileImageFile(vehicleId: String, fileName: String): File =
        attachmentFile(vehicleId, fileName)

    fun shareUri(file: File): Uri =
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )

    private fun vehicleDirectory(vehicleId: String): File =
        File(context.filesDir, "vehicle_attachments/$vehicleId")

    private fun queryDisplayName(uri: Uri): String? {
        val resolver = context.contentResolver
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) {
                return cursor.getString(index)
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/')
    }

    private fun guessMimeType(fileName: String): String {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            ?: "application/octet-stream"
    }

    private fun extensionFromMimeOrName(mimeType: String, fileName: String): String {
        MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)?.let { return it }
        fileName.substringAfterLast('.', "").takeIf { it.isNotBlank() }?.let { return it.lowercase() }
        return when {
            mimeType.startsWith("image/") -> "jpg"
            mimeType == "application/pdf" -> "pdf"
            else -> "bin"
        }
    }

    private fun guessCategory(fileName: String): VehicleAttachmentCategory {
        val lower = fileName.lowercase()
        return when {
            "versicher" in lower || "insurance" in lower -> VehicleAttachmentCategory.INSURANCE
            "tüv" in lower || "tuv" in lower || "hu" in lower -> VehicleAttachmentCategory.TUV
            "brief" in lower || "schein" in lower || "zulass" in lower -> VehicleAttachmentCategory.REGISTRATION
            "kauf" in lower || "rechnung" in lower && "werkstatt" !in lower -> VehicleAttachmentCategory.PURCHASE
            "rechnung" in lower || "invoice" in lower -> VehicleAttachmentCategory.INVOICE
            "service" in lower || "wartung" in lower -> VehicleAttachmentCategory.SERVICE
            else -> VehicleAttachmentCategory.OTHER
        }
    }
}
