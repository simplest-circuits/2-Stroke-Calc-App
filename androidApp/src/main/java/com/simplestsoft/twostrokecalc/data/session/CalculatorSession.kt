package com.simplestsoft.twostrokecalc.data.session

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalculatorSession @Inject constructor() {
    var strokeMm: Double? = null
        private set
    var transferDurationDeg: Double? = null
        private set
    var exhaustDurationDeg: Double? = null
        private set

    fun updateFromPortTiming(
        strokeMm: Double,
        transferDurationDeg: Double,
        exhaustDurationDeg: Double,
    ) {
        this.strokeMm = strokeMm
        this.transferDurationDeg = transferDurationDeg
        this.exhaustDurationDeg = exhaustDurationDeg
    }

    fun hasTimingData(): Boolean =
        transferDurationDeg != null || exhaustDurationDeg != null
}
