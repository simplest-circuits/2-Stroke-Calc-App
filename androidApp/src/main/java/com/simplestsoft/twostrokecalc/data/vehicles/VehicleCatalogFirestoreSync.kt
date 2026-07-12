package com.simplestsoft.twostrokecalc.data.vehicles

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.simplestsoft.twostrokecalc.data.preferences.PreferencesManager
import com.simplestsoft.twostrokecalc.domain.model.VehicleCatalogEntry
import com.simplestsoft.twostrokecalc.domain.model.VehicleCatalogFile
import com.simplestsoft.twostrokecalc.logging.ErrorLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

@Singleton
class VehicleCatalogFirestoreSync @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firestore: FirebaseFirestore,
    private val gson: Gson,
    private val preferencesManager: PreferencesManager,
    private val errorLogger: ErrorLogger,
) {
    private val cacheFile: File
        get() = File(context.filesDir, CACHE_FILE_NAME)

    suspend fun syncIfNeeded(): VehicleCatalogFile? = withContext(Dispatchers.IO) {
        runCatching {
            val meta = fetchMeta() ?: return@runCatching null
            val remoteVersion = meta.version
            val cachedVersion = preferencesManager.getVehicleCatalogVersion()
            if (remoteVersion <= cachedVersion && loadCacheFile() != null) {
                return@runCatching null
            }
            val catalog = fetchCatalog(meta)
            writeCacheFile(catalog)
            preferencesManager.setVehicleCatalogVersion(remoteVersion)
            catalog
        }.onFailure { error ->
            errorLogger.log("VehicleCatalogFirestoreSync", "syncIfNeeded", error)
        }.getOrNull()
    }

    fun loadCacheFile(): VehicleCatalogFile? = runCatching {
        if (!cacheFile.exists()) return null
        gson.fromJson(cacheFile.readText(), VehicleCatalogFile::class.java)
    }.getOrNull()

    private suspend fun fetchMeta(): CatalogMeta? {
        val snapshot = firestore.collection(COLLECTION).document(META_DOC_ID).get().await()
        if (!snapshot.exists()) return null
        val data = snapshot.data ?: return null
        val chunkIds = (data["chunkIds"] as? List<*>)?.mapNotNull { it as? String }.orEmpty()
        if (chunkIds.isEmpty()) return null
        return CatalogMeta(
            version = (data["version"] as? Number)?.toInt() ?: 0,
            entryCount = (data["entryCount"] as? Number)?.toInt() ?: 0,
            source = data["source"] as? String ?: "",
            chunkIds = chunkIds,
        )
    }

    private suspend fun fetchCatalog(meta: CatalogMeta): VehicleCatalogFile {
        val entryType = object : TypeToken<List<VehicleCatalogEntry>>() {}.type
        val entries = mutableListOf<VehicleCatalogEntry>()
        for (chunkId in meta.chunkIds) {
            val snapshot = firestore.collection(COLLECTION).document(chunkId).get().await()
            val chunkEntries = snapshot.data?.get("entries")
            if (chunkEntries is List<*>) {
                val json = gson.toJson(chunkEntries)
                entries += gson.fromJson<List<VehicleCatalogEntry>>(json, entryType)
            }
        }
        return VehicleCatalogFile(
            version = meta.version,
            source = meta.source,
            entries = entries,
        )
    }

    private fun writeCacheFile(catalog: VehicleCatalogFile) {
        cacheFile.writeText(gson.toJson(catalog))
    }

    private data class CatalogMeta(
        val version: Int,
        val entryCount: Int,
        val source: String,
        val chunkIds: List<String>,
    )

    companion object {
        const val COLLECTION = "vehicleCatalog"
        const val META_DOC_ID = "meta"
        const val CACHE_FILE_NAME = "vehicle_catalog_cache.json"
    }
}
