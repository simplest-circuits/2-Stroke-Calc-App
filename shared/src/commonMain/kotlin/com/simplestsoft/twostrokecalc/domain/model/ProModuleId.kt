package com.simplestsoft.twostrokecalc.domain.model

enum class ProModuleId {
    VEHICLES,
    TIMING,
    PORT_AREA,
    IGNITION,
    DC_CABLE,
    FLUID,
    COMPRESSION,
    SQUISH_BAND,
    MEAN_PRESSURE,
    GEAR,
    EXHAUST,
    COUNTERWEIGHT,
    VARIATOR_WEIGHT,
    FUEL_MIX,
    CARB_JET,
    DYNO_INERTIA,
    VEHICLE_DYNAMICS,
    ;

    fun toCalculator(): CalculatorId? = if (this == VEHICLES) null else CalculatorId.valueOf(name)

    companion object {
        val defaultProModules: Set<ProModuleId> = setOf(
            VEHICLES,
            TIMING,
            DC_CABLE,
            FLUID,
            GEAR,
        )

        fun fromCalculator(id: CalculatorId): ProModuleId = valueOf(id.name)

        fun fromName(name: String): ProModuleId? = when (name) {
            CalculatorId.LEGACY_FLUID_NAME -> FLUID
            else -> entries.firstOrNull { it.name == name }
        }
    }
}
