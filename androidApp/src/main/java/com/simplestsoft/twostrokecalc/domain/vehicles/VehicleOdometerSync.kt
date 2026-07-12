package com.simplestsoft.twostrokecalc.domain.vehicles

import com.simplestsoft.twostrokecalc.domain.model.FuelLogEntry

data class FuelLogCounterSync(
    val currentOdometerKm: String,
    val currentOperatingHours: String,
)

object VehicleOdometerSync {

    /** Erhöht Kilometerstand und Betriebsstunden in den Stammdaten auf die höchsten Tankbuch-Werte. */
    fun syncFromFuelLog(
        currentOdometerKm: String,
        currentOperatingHours: String,
        fuelLog: List<FuelLogEntry>,
    ): FuelLogCounterSync = FuelLogCounterSync(
        currentOdometerKm = syncCounter(currentOdometerKm, fuelLog) { it.odometerKm },
        currentOperatingHours = syncCounter(currentOperatingHours, fuelLog) { it.operatingHours },
    )

    private fun syncCounter(
        currentValue: String,
        fuelLog: List<FuelLogEntry>,
        selector: (FuelLogEntry) -> String,
    ): String {
        val maxFuelValue = fuelLog
            .mapNotNull { entry ->
                parseCounter(selector(entry))?.let { value -> value to selector(entry).trim() }
            }
            .maxByOrNull { it.first }
            ?.second

        if (maxFuelValue.isNullOrBlank()) return currentValue

        val current = parseCounter(currentValue)
        val maxFuel = parseCounter(maxFuelValue) ?: return currentValue

        return if (current == null || maxFuel > current) {
            maxFuelValue
        } else {
            currentValue
        }
    }

    private fun parseCounter(raw: String): Double? =
        raw.trim()
            .replace(",", ".")
            .takeIf { it.isNotBlank() }
            ?.toDoubleOrNull()
}
