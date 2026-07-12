package com.simplestsoft.twostrokecalc.data.vehicles

import com.simplestsoft.twostrokecalc.domain.model.Vehicle
import kotlinx.serialization.json.Json

internal object VehicleJsonCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(vehicles: List<Vehicle>): String = json.encodeToString(vehicles)

    fun decode(raw: String?): List<Vehicle> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            json.decodeFromString<List<Vehicle>>(raw).map { it.withLegacyMigration() }
        }.getOrDefault(emptyList())
    }
}
