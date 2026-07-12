package com.simplestsoft.twostrokecalc.domain.model

enum class FuelMixPreset(val ratio: Int) {
    STANDARD(50),
    OLDER(40),
    CLASSIC(33),
    RACING(25),
    SYNTHETIC(100),
}
