package com.simplestsoft.twostrokecalc.domain.ignitiontiming

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object IgnitionTimingCalculator {

    /**
     * Kolbenabstand zum OT in mm:
     * sk = hr * (1 + pl/hr - cos(a) - sqrt((pl/hr)² - sin(a)²))
     *
     * hr = Hub/2, pl = Pleuellänge, a = Kurbelwinkel in Grad vor OT
     */
    fun degreesBeforeTdcToMm(
        strokeMm: Double,
        connectingRodMm: Double,
        degreesBeforeTdc: Double,
    ): Double? {
        if (strokeMm <= 0.0 || connectingRodMm <= 0.0) return null
        if (degreesBeforeTdc < 0.0 || degreesBeforeTdc > 180.0) return null
        if (degreesBeforeTdc == 0.0) return 0.0
        if (degreesBeforeTdc == 180.0) return strokeMm

        val sk = pistonDistanceToTdcMm(strokeMm, connectingRodMm, degreesBeforeTdc)
        return sk.takeIf { it in 0.0..strokeMm }
    }

    fun mmBeforeTdcToDegrees(
        strokeMm: Double,
        connectingRodMm: Double,
        mmBeforeTdc: Double,
    ): Double? {
        if (strokeMm <= 0.0 || connectingRodMm <= 0.0) return null
        if (mmBeforeTdc < 0.0 || mmBeforeTdc > strokeMm) return null
        if (mmBeforeTdc == 0.0) return 0.0
        if (mmBeforeTdc == strokeMm) return 180.0

        var low = 0.0
        var high = 180.0
        repeat(60) {
            val mid = (low + high) / 2.0
            val sk = pistonDistanceToTdcMm(strokeMm, connectingRodMm, mid)
            if (sk < mmBeforeTdc) {
                low = mid
            } else {
                high = mid
            }
        }
        return (low + high) / 2.0
    }

    internal fun pistonDistanceToTdcMm(
        strokeMm: Double,
        connectingRodMm: Double,
        degreesBeforeTdc: Double,
    ): Double {
        val hr = strokeMm / 2.0
        val pl = connectingRodMm
        val rodRatio = pl / hr
        val angleRad = degreesBeforeTdc * PI / 180.0
        val sinA = sin(angleRad)
        val discriminant = rodRatio * rodRatio - sinA * sinA
        return hr * (1.0 + rodRatio - cos(angleRad) - sqrt(discriminant))
    }
}
