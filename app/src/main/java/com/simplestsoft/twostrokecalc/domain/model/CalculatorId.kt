package com.simplestsoft.twostrokecalc.domain.model

enum class CalculatorId {
    TIMING,
    IGNITION,
    DC_CABLE,
    FLUID,
    FUEL_MIX,
    CARB_JET,
    COUNTERWEIGHT,
    COMPRESSION,
    MEAN_PRESSURE,
    SQUISH_BAND,
    GEAR,
    EXHAUST,
    PORT_AREA,
    VARIATOR_WEIGHT,
    DYNO_INERTIA,
    VEHICLE_DYNAMICS,
    ;

    companion object {
        const val LEGACY_FLUID_NAME = "ELECTROLYTE"

        fun fromName(name: String): CalculatorId? = when (name) {
            LEGACY_FLUID_NAME -> FLUID
            else -> entries.firstOrNull { it.name == name }
        }

        fun normalizeAvailabilityMap(map: Map<String, Boolean>): Map<String, Boolean> {
            if (!map.containsKey(LEGACY_FLUID_NAME)) return map
            val result = map.toMutableMap()
            if (!result.containsKey(FLUID.name)) {
                result[FLUID.name] = result.remove(LEGACY_FLUID_NAME) ?: true
            }
            return result
        }
    }
}
