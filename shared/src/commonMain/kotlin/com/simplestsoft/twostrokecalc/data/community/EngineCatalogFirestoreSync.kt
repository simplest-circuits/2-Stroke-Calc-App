package com.simplestsoft.twostrokecalc.data.community

import com.simplestsoft.twostrokecalc.data.preferences.AppPreferencesStore
import com.simplestsoft.twostrokecalc.domain.model.community.EngineCatalogFile
import com.simplestsoft.twostrokecalc.domain.model.community.EngineFamilyEntry
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

class EngineCatalogFirestoreSync(
    private val preferencesStore: AppPreferencesStore,
) {
    suspend fun syncIfNeeded(): EngineCatalogFile? = runCatching {
        val meta = fetchMeta() ?: return@runCatching null
        val remoteVersion = meta.version
        val cachedVersion = preferencesStore.getEngineCatalogVersion()
        val cached = loadCacheFile()
        if (remoteVersion <= cachedVersion && cached != null && cached.entries.isNotEmpty()) {
            return@runCatching null
        }
        val catalog = fetchCatalog(meta)
        writeCacheFile(catalog)
        preferencesStore.setEngineCatalogVersion(remoteVersion)
        catalog
    }.getOrNull()

    fun loadCacheFile(): EngineCatalogFile? {
        val raw = preferencesStore.getEngineCatalogJson() ?: return null
        val decoded = EngineCatalogJsonCodec.decode(raw)
        return decoded.takeIf { it.entries.isNotEmpty() }
    }

    fun seedFromJson(raw: String, onlyIfNewerOrEmpty: Boolean = false): EngineCatalogFile {
        val catalog = EngineCatalogJsonCodec.decode(raw)
        if (catalog.entries.isEmpty()) return catalog
        if (onlyIfNewerOrEmpty) {
            val cached = loadCacheFile()
            val cachedVersion = preferencesStore.getEngineCatalogVersion()
            if (cached != null &&
                cached.entries.isNotEmpty() &&
                cachedVersion >= catalog.version
            ) {
                return cached
            }
        }
        writeCacheFile(catalog)
        preferencesStore.setEngineCatalogVersion(catalog.version)
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

    private suspend fun fetchCatalog(meta: CatalogMeta): EngineCatalogFile {
        val entries = mutableListOf<EngineFamilyEntry>()
        for (chunkId in meta.chunkIds) {
            val snapshot = Firebase.firestore.collection(COLLECTION).document(chunkId).get()
            val chunkEntries = snapshot.get<List<Map<String, Any?>>>("entries").orEmpty()
            chunkEntries.forEach { map ->
                runCatching {
                    val jsonObject = mapToJsonObject(map)
                    entries += json.decodeFromJsonElement(EngineFamilyEntry.serializer(), jsonObject)
                }
            }
        }
        return EngineCatalogFile(
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

    private fun writeCacheFile(catalog: EngineCatalogFile) {
        preferencesStore.setEngineCatalogJson(EngineCatalogJsonCodec.encode(catalog))
    }

    private data class CatalogMeta(
        val version: Int,
        val entryCount: Int,
        val source: String,
        val chunkIds: List<String>,
    )

    companion object {
        const val COLLECTION = "engineCatalog"
        const val META_DOC_ID = "meta"
        private val json = Json { ignoreUnknownKeys = true }
    }
}
