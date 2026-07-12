package com.simplestsoft.twostrokecalc.data.vehicles

import com.simplestsoft.twostrokecalc.domain.model.Vehicle
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject

object VehicleFirestoreMapper {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun toMap(vehicle: Vehicle): Map<String, Any?> =
        json.encodeToJsonElement(Vehicle.serializer(), vehicle)
            .jsonObject
            .mapValues { (_, value) -> jsonElementToValue(value) }

    fun fromMap(data: Map<String, Any?>): Vehicle? {
        val jsonObject = buildJsonObject {
            data.forEach { (key, value) ->
                put(key, valueToJsonElement(value))
            }
        }
        return runCatching {
            json.decodeFromJsonElement(Vehicle.serializer(), jsonObject).withLegacyMigration()
        }.getOrNull()
    }

    private fun jsonElementToValue(element: JsonElement): Any? = when (element) {
        JsonNull -> null
        is JsonPrimitive -> when {
            element.isString -> element.content
            element.content.equals("true", ignoreCase = true) -> true
            element.content.equals("false", ignoreCase = true) -> false
            element.content.contains('.') -> element.content.toDoubleOrNull() ?: element.content
            else -> element.content.toLongOrNull() ?: element.content
        }
        is JsonObject -> element.mapValues { (_, value) -> jsonElementToValue(value) }
        is JsonArray -> element.map { jsonElementToValue(it) }
    }

    @Suppress("UNCHECKED_CAST")
    private fun valueToJsonElement(value: Any?): JsonElement = when (value) {
        null -> JsonNull
        is JsonElement -> value
        is Boolean -> JsonPrimitive(value)
        is Number -> JsonPrimitive(value.toString())
        is String -> JsonPrimitive(value)
        is Map<*, *> -> buildJsonObject {
            (value as Map<String, Any?>).forEach { (key, nested) ->
                put(key, valueToJsonElement(nested))
            }
        }
        is List<*> -> JsonArray(value.map { valueToJsonElement(it) })
        else -> JsonPrimitive(value.toString())
    }
}
