package com.simplestsoft.twostrokecalc.data.vehicles

import com.simplestsoft.twostrokecalc.data.preferences.AppPreferencesStore
import com.simplestsoft.twostrokecalc.domain.model.VehicleCatalogEntry
import com.simplestsoft.twostrokecalc.domain.model.VehicleCatalogFile
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject

class SharedVehicleCatalogFirestoreSync(
    private val preferencesStore: AppPreferencesStore,
) {
    suspend fun syncIfNeeded(): VehicleCatalogFile? = runCatching {
        val meta = fetchMeta() ?: return@runCatching null
        val remoteVersion = meta.version
        val cachedVersion = preferencesStore.getVehicleCatalogVersion()
        val cached = loadCacheFile()
        if (remoteVersion <= cachedVersion && cached != null && cached.entries.isNotEmpty()) {
            return@runCatching null
        }
        val catalog = fetchCatalog(meta)
        writeCacheFile(catalog)
        preferencesStore.setVehicleCatalogVersion(remoteVersion)
        catalog
    }.getOrNull()

    fun loadCacheFile(): VehicleCatalogFile? {
        val raw = preferencesStore.getVehicleCatalogJson() ?: return null
        val decoded = VehicleCatalogJsonCodec.decode(raw)
        return decoded.takeIf { it.entries.isNotEmpty() }
    }

    fun seedFromJson(raw: String): VehicleCatalogFile {
        val catalog = VehicleCatalogJsonCodec.decode(raw)
        if (catalog.entries.isNotEmpty()) {
            writeCacheFile(catalog)
        }
        return catalog
    }

    private suspend fun fetchMeta(): CatalogMeta? {
        val snapshot = Firebase.firestore.collection(COLLECTION).document(META_DOC_ID).get()
        if (!snapshot.exists) return null
        val chunkIds = snapshot.get<List<String>>("chunkIds").orEmpty()
        if (chunkIds.isEmpty()) return null
        return CatalogMeta(
            version = snapshot.get<Long>("version")?.toInt() ?: snapshot.get<Int>("version") ?: 0,
            entryCount = snapshot.get<Long>("entryCount")?.toInt() ?: snapshot.get<Int>("entryCount") ?: 0,
            source = snapshot.get<String>("source").orEmpty(),
            chunkIds = chunkIds,
        )
    }

    private suspend fun fetchCatalog(meta: CatalogMeta): VehicleCatalogFile {
        val entries = mutableListOf<VehicleCatalogEntry>()
        for (chunkId in meta.chunkIds) {
            val snapshot = Firebase.firestore.collection(COLLECTION).document(chunkId).get()
            val chunkEntries = snapshot.get<List<Map<String, Any?>>>("entries").orEmpty()
            chunkEntries.forEach { map ->
                runCatching {
                    val jsonObject = mapToJsonObject(map)
                    entries += json.decodeFromJsonElement(VehicleCatalogEntry.serializer(), jsonObject)
                }
            }
        }
        return VehicleCatalogFile(
            version = meta.version,
            source = meta.source,
            entries = entries,
        )
    }

    private fun mapToJsonObject(map: Map<String, Any?>): JsonObject = buildJsonObject {
        map.forEach { (key, value) ->
            put(key, valueToJsonElement(value))
        }
    }

    private fun valueToJsonElement(value: Any?): JsonElement = when (value) {
        null -> JsonNull
        is String -> JsonPrimitive(value)
        is Number -> JsonPrimitive(value)
        is Boolean -> JsonPrimitive(value)
        is Map<*, *> -> {
            @Suppress("UNCHECKED_CAST")
            mapToJsonObject(value as Map<String, Any?>)
        }
        is List<*> -> JsonArray(value.map { valueToJsonElement(it) })
        else -> JsonPrimitive(value.toString())
    }

    private fun writeCacheFile(catalog: VehicleCatalogFile) {
        preferencesStore.setVehicleCatalogJson(VehicleCatalogJsonCodec.encode(catalog))
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
        private val json = Json { ignoreUnknownKeys = true }
    }
}
