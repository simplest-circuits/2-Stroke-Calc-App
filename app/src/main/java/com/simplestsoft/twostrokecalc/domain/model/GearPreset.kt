package com.simplestsoft.twostrokecalc.domain.model

import com.simplestsoft.twostrokecalc.R

enum class GearPreset {
    VESPA_PX_200,
    VESPA_PX_125,
    VESPA_PX_80,
    CUSTOM,
}

data class GearPresetStage(
    val secondaryPinion: Int,
    val secondaryGear: Int,
)

data class GearPresetValues(
    val primaryPinion: Int,
    val primaryGear: Int,
    val stages: List<GearPresetStage>,
)

fun GearPreset.labelRes(): Int = when (this) {
    GearPreset.VESPA_PX_200 -> R.string.gear_preset_px_200
    GearPreset.VESPA_PX_125 -> R.string.gear_preset_px_125
    GearPreset.VESPA_PX_80 -> R.string.gear_preset_px_80
    GearPreset.CUSTOM -> R.string.gear_preset_custom
}

fun GearPreset.values(): GearPresetValues? = when (this) {
    GearPreset.VESPA_PX_200 -> GearPresetValues(
        primaryPinion = 23,
        primaryGear = 64,
        stages = listOf(
            GearPresetStage(12, 57),
            GearPresetStage(13, 42),
            GearPresetStage(17, 38),
            GearPresetStage(21, 35),
        ),
    )
    GearPreset.VESPA_PX_125 -> GearPresetValues(
        primaryPinion = 21,
        primaryGear = 68,
        stages = listOf(
            GearPresetStage(12, 58),
            GearPresetStage(13, 42),
            GearPresetStage(17, 38),
            GearPresetStage(21, 36),
        ),
    )
    GearPreset.VESPA_PX_80 -> GearPresetValues(
        primaryPinion = 20,
        primaryGear = 68,
        stages = listOf(
            GearPresetStage(10, 59),
            GearPresetStage(14, 55),
            GearPresetStage(19, 50),
            GearPresetStage(23, 47),
        ),
    )
    GearPreset.CUSTOM -> null
}
