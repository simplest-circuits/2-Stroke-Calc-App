package com.simplestsoft.twostrokecalc.data.community

import com.simplestsoft.twostrokecalc.domain.model.community.EngineCatalogFile
import kotlinx.serialization.json.Json

object EngineCatalogJsonCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun encode(catalog: EngineCatalogFile): String =
        json.encodeToString(EngineCatalogFile.serializer(), catalog)

    fun decode(raw: String?): EngineCatalogFile {
        if (raw.isNullOrBlank()) return EngineCatalogFile()
        return runCatching {
            json.decodeFromString(EngineCatalogFile.serializer(), raw)
        }.getOrDefault(EngineCatalogFile())
    }
}
