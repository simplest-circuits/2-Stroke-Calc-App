package com.simplestsoft.twostrokecalc.data.vehicles

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.simplestsoft.twostrokecalc.domain.model.Vehicle

internal object VehicleFirestoreMapper {

    private val mapType = object : TypeToken<Map<String, Any>>() {}.type

    fun toMap(gson: Gson, vehicle: Vehicle): Map<String, Any> {
        val json = gson.toJson(vehicle)
        @Suppress("UNCHECKED_CAST")
        return gson.fromJson<Map<String, Any>>(json, mapType)
    }

    fun fromMap(gson: Gson, data: Map<String, Any?>): Vehicle? {
        val json = gson.toJson(data)
        return runCatching {
            gson.fromJson(json, Vehicle::class.java).withLegacyMigration()
        }.getOrNull()
    }
}
