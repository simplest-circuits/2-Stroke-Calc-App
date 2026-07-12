package com.simplestsoft.twostrokecalc.domain.calculation

import kotlin.math.roundToInt

object FuelMixCalculator {

    const val DEFAULT_FUEL_LITERS = 5.0

    const val MIN_FUEL_LITERS = 0.5
    const val MAX_FUEL_LITERS = 20.0
    const val FUEL_STEP_LITERS = 0.1

    val fuelSliderSteps: Int
        get() = ((MAX_FUEL_LITERS - MIN_FUEL_LITERS) / FUEL_STEP_LITERS).roundToInt()

    const val MIN_OIL_ML = 5.0
    const val MAX_OIL_ML = 800.0
    const val OIL_STEP_ML = 1.0

    val oilSliderSteps: Int
        get() = ((MAX_OIL_ML - MIN_OIL_ML) / OIL_STEP_ML).roundToInt()

    fun snapFuelLiters(fuelLiters: Double): Double {
        val stepIndex = ((fuelLiters - MIN_FUEL_LITERS) / FUEL_STEP_LITERS).roundToInt()
        return (MIN_FUEL_LITERS + stepIndex * FUEL_STEP_LITERS)
            .coerceIn(MIN_FUEL_LITERS, MAX_FUEL_LITERS)
    }

    fun snapOilMl(oilMl: Double): Double {
        val stepIndex = ((oilMl - MIN_OIL_ML) / OIL_STEP_ML).roundToInt()
        return (MIN_OIL_ML + stepIndex * OIL_STEP_ML)
            .coerceIn(MIN_OIL_ML, MAX_OIL_ML)
    }
    /**
     * Calculates required two-stroke oil in milliliters for a given fuel amount
     * at ratio 1:[ratioPartsFuel] (e.g. 1:50 → ratioPartsFuel = 50).
     */
    fun oilMillilitersFromFuelLiters(fuelLiters: Double, ratioPartsFuel: Int): Double? {
        if (fuelLiters <= 0.0 || ratioPartsFuel <= 0) return null
        return fuelLiters * 1000.0 / ratioPartsFuel
    }

    /**
     * Calculates required fuel in liters for a given oil amount
     * at ratio 1:[ratioPartsFuel].
     */
    fun fuelLitersFromOilMilliliters(oilMilliliters: Double, ratioPartsFuel: Int): Double? {
        if (oilMilliliters <= 0.0 || ratioPartsFuel <= 0) return null
        return oilMilliliters * ratioPartsFuel / 1000.0
    }
}
