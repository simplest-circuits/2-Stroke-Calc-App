package com.simplestsoft.twostrokecalc.data.community

import com.simplestsoft.twostrokecalc.domain.model.community.EngineCatalogFile
import com.simplestsoft.twostrokecalc.domain.model.community.EngineFamilyEntry
import com.simplestsoft.twostrokecalc.domain.model.community.EngineOrientation
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SharedEngineCatalogRepository(
    private val firestoreSync: EngineCatalogFirestoreSync,
) {
    private val mutex = Mutex()
    private var catalog: EngineCatalogFile = firestoreSync.loadCacheFile() ?: EngineCatalogFile()

    fun entryCount(): Int = catalog.entries.size

    fun catalogVersion(): Int = catalog.version

    fun allEntries(): List<EngineFamilyEntry> = catalog.entries

    fun manufacturers(): List<String> = catalog.entries
        .map { it.manufacturer }
        .distinct()
        .sorted()

    fun findById(id: String): EngineFamilyEntry? =
        catalog.entries.firstOrNull { it.id == id }

    fun search(query: String, limit: Int = 40): List<EngineFamilyEntry> {
        val q = query.trim()
        if (q.isEmpty()) return catalog.entries.take(limit)
        return catalog.entries.filter { it.matchesQuery(q) }.take(limit)
    }

    fun filter(
        manufacturer: String? = null,
        orientation: EngineOrientation? = null,
        query: String = "",
    ): List<EngineFamilyEntry> = catalog.entries.filter { entry ->
        (manufacturer.isNullOrBlank() || entry.manufacturer.equals(manufacturer, ignoreCase = true)) &&
            (orientation == null || entry.orientation == orientation) &&
            (query.isBlank() || entry.matchesQuery(query))
    }

    suspend fun ensureSynced() {
        val updated = firestoreSync.syncIfNeeded() ?: return
        mutex.withLock {
            if (updated.entries.isNotEmpty()) {
                catalog = updated
            }
        }
    }

    fun seedFromBundledJson(raw: String, onlyIfEmpty: Boolean = false) {
        val seeded = firestoreSync.seedFromJson(raw, onlyIfNewerOrEmpty = !onlyIfEmpty)
        if (seeded.entries.isNotEmpty()) {
            catalog = seeded
        }
    }
}
