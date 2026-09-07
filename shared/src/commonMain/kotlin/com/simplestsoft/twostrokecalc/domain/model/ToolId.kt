package com.simplestsoft.twostrokecalc.domain.model

enum class ToolId {
    RPM_TACHOMETER,
    PORT_TIMING_ASSIST,
    ANGLE_METER,
    VIBRATION_ANALYZER,
    GPS_DYNO,
    COMMUNITY_SETUPS,
    ;

    companion object {
        fun fromName(name: String): ToolId? =
            entries.firstOrNull { it.name == name }

        fun normalizeAvailabilityMap(map: Map<String, Boolean>): Map<String, Boolean> = map
    }
}
