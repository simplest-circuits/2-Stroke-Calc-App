package com.simplestsoft.twostrokecalc.data.vehicles

import com.simplestsoft.twostrokecalc.domain.model.VehicleCatalogFile
import kotlinx.serialization.json.Json

object VehicleCatalogJsonCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun encode(catalog: VehicleCatalogFile): String = json.encodeToString(VehicleCatalogFile.serializer(), catalog)

    fun decode(raw: String?): VehicleCatalogFile {
        if (raw.isNullOrBlank()) return VehicleCatalogFile()
        return runCatching {
            json.decodeFromString(VehicleCatalogFile.serializer(), raw)
        }.getOrDefault(VehicleCatalogFile())
    }
}
