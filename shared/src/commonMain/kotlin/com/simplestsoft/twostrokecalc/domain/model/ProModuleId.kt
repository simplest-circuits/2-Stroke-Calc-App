package com.simplestsoft.twostrokecalc.domain.model

enum class ProModuleId {
    VEHICLES,
    TOOLS,
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
    STINGER,
    COUNTERWEIGHT,
    VARIATOR_WEIGHT,
    FUEL_MIX,
    CARB_JET,
    DYNO_INERTIA,
    VEHICLE_DYNAMICS,
    ;

    fun toCalculator(): CalculatorId? = when (this) {
        VEHICLES, TOOLS -> null
        else -> CalculatorId.valueOf(name)
    }

    companion object {
        val defaultProModules: Set<ProModuleId> = setOf(
            VEHICLES,
            TOOLS,
            TIMING,
            DC_CABLE,
            FLUID,
            GEAR,
            STINGER,
        )

        fun fromCalculator(id: CalculatorId): ProModuleId = valueOf(id.name)

        fun fromTool(id: ToolId): ProModuleId? = when (id) {
            ToolId.COMMUNITY_SETUPS -> null
            else -> TOOLS
        }

        fun fromName(name: String): ProModuleId? = when (name) {
            CalculatorId.LEGACY_FLUID_NAME -> FLUID
            else -> entries.firstOrNull { it.name == name }
        }
    }
}
