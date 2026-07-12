package com.simplestsoft.twostrokecalc.data.vehicles

import com.simplestsoft.twostrokecalc.domain.model.VehicleCatalogEntry
import com.simplestsoft.twostrokecalc.domain.model.VehicleCatalogFile
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SharedVehicleCatalogRepository(
    private val firestoreSync: SharedVehicleCatalogFirestoreSync,
) {
    private val mutex = Mutex()
    private var catalog: VehicleCatalogFile = firestoreSync.loadCacheFile() ?: VehicleCatalogFile()

    fun entryCount(): Int = catalog.entries.size

    fun allEntries(): List<VehicleCatalogEntry> = catalog.entries

    fun brands(): List<String> = catalog.entries
        .map { it.brand }
        .distinct()
        .sorted()

    fun modelsForBrand(brand: String): List<String> = catalog.entries
        .filter { it.brand == brand }
        .map { it.model }
        .distinct()
        .sorted()

    fun yearsForBrandModel(brand: String, model: String): List<Int> {
        val entries = catalog.entries.filter { it.brand == brand && it.model == model }
        if (entries.isEmpty()) return emptyList()
        val minYear = entries.minOf { it.yearFrom }
        val maxYear = entries.maxOf { it.yearTo }
        return (minYear..maxYear).filter { year -> entries.any { it.matchesYear(year) } }
    }

    fun variantsFor(brand: String, model: String, year: Int): List<VehicleCatalogEntry> =
        catalog.entries.filter { entry ->
            entry.brand == brand && entry.model == model && entry.matchesYear(year)
        }

    fun findById(id: String): VehicleCatalogEntry? =
        catalog.entries.firstOrNull { it.id == id }

    fun search(query: String, limit: Int = 30): List<VehicleCatalogEntry> {
        val q = query.trim().lowercase()
        if (q.length < 2) return emptyList()
        return catalog.entries.filter { entry ->
            entry.brand.lowercase().contains(q) ||
                entry.model.lowercase().contains(q) ||
                entry.variant.lowercase().contains(q) ||
                entry.frameCode.lowercase().contains(q) ||
                entry.category.lowercase().contains(q)
        }.take(limit)
    }

    suspend fun ensureSynced() {
        val updated = firestoreSync.syncIfNeeded() ?: return
        mutex.withLock {
            if (updated.entries.isNotEmpty()) {
                catalog = updated
            }
        }
    }

    fun seedFromBundledJson(raw: String) {
        val seeded = firestoreSync.seedFromJson(raw)
        if (seeded.entries.isNotEmpty()) {
            catalog = seeded
        }
    }
}
