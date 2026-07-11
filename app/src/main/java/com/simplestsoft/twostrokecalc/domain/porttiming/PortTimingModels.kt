package com.simplestsoft.twostrokecalc.domain.porttiming

enum class IntakeSystem {
    ROTARY_VALVE,
    REED_VALVE,
}

data class PortTimingInput(
    val strokeMm: Double = 57.0,
    val connectingRodMm: Double = 110.0,
    val exhaustPortMm: Double = 36.2,
    val transferPortMm: Double = 47.5,
    val intakeOpenMm: Double = 44.2,
    val intakeCloseMm: Double = 15.9,
    val pistonDeckMm: Double = -0.5,
    val intakeSystem: IntakeSystem = IntakeSystem.ROTARY_VALVE,
    val roundResults: Boolean = false,
)

data class ChannelTiming(
    val openBeforeBdc: Double,
    val duration: Double,
)

data class IntakeTiming(
    val openBeforeTdc: Double,
    val closeAfterTdc: Double,
    val duration: Double,
)

data class PortTimingResult(
    val exhaust: ChannelTiming?,
    val transfer: ChannelTiming?,
    val blowdown: Double?,
    val intake: IntakeTiming?,
)
