package com.simplestsoft.twostrokecalc.domain.model

import com.simplestsoft.twostrokecalc.R

fun GearPreset.labelRes(): Int = when (this) {
    GearPreset.VESPA_PX_200 -> R.string.gear_preset_px_200
    GearPreset.VESPA_PX_125 -> R.string.gear_preset_px_125
    GearPreset.VESPA_PX_80 -> R.string.gear_preset_px_80
    GearPreset.CUSTOM -> R.string.gear_preset_custom
}
