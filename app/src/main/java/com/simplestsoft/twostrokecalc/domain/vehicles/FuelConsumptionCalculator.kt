package com.simplestsoft.twostrokecalc.domain.vehicles

import com.simplestsoft.twostrokecalc.domain.model.FuelLogEntry

enum class FuelCounterUnit {
    KILOMETERS,
    OPERATING_HOURS,
}

data class FuelConsumptionSegment(
    val entryId: String,
    val dateLabel: String,
    val consumption: Double,
    val unit: FuelCounterUnit,
)

data class FuelConsumptionSummary(
    val averageConsumption: Double?,
    val unit: FuelCounterUnit?,
    val segments: List<FuelConsumptionSegment>,
)

object FuelConsumptionCalculator {

    fun entryCounterValue(entry: FuelLogEntry): Pair<Double, FuelCounterUnit>? {
        val km = parseCounter(entry.odometerKm)
        val hours = parseCounter(entry.operatingHours)
        return when {
            km != null && hours == null -> km to FuelCounterUnit.KILOMETERS
            hours != null && km == null -> hours to FuelCounterUnit.OPERATING_HOURS
            else -> null
        }
    }

    fun summarize(entries: List<FuelLogEntry>): FuelConsumptionSummary {
        val parsed = entries.mapNotNull { entry ->
            val liters = parseCounter(entry.liters)?.takeIf { it > 0 } ?: return@mapNotNull null
            val counter = entryCounterValue(entry) ?: return@mapNotNull null
            ParsedEntry(entry, liters, counter.first, counter.second)
        }

        if (parsed.size < 2) {
            return FuelConsumptionSummary(
                averageConsumption = null,
                unit = parsed.firstOrNull()?.unit,
                segments = emptyList(),
            )
        }

        val unit = parsed.first().unit
        val sorted = parsed
            .filter { it.unit == unit }
            .sortedBy { it.counter }

        val segments = buildList {
            for (index in 1 until sorted.size) {
                val previous = sorted[index - 1]
                val current = sorted[index]
                val delta = current.counter - previous.counter
                if (delta <= 0.0) continue
                val consumption = when (unit) {
                    FuelCounterUnit.KILOMETERS -> (current.liters / delta) * 100.0
                    FuelCounterUnit.OPERATING_HOURS -> current.liters / delta
                }
                add(
                    FuelConsumptionSegment(
                        entryId = current.entry.id,
                        dateLabel = current.entry.date.ifBlank { "#$index" },
                        consumption = consumption,
                        unit = unit,
                    ),
                )
            }
        }

        val average = if (segments.isNotEmpty()) {
            val totalDelta = sorted.last().counter - sorted.first().counter
            val totalLiters = sorted.drop(1).sumOf { it.liters }
            if (totalDelta > 0.0) {
                when (unit) {
                    FuelCounterUnit.KILOMETERS -> (totalLiters / totalDelta) * 100.0
                    FuelCounterUnit.OPERATING_HOURS -> totalLiters / totalDelta
                }
            } else {
                null
            }
        } else {
            null
        }

        return FuelConsumptionSummary(
            averageConsumption = average,
            unit = unit,
            segments = segments,
        )
    }

    private data class ParsedEntry(
        val entry: FuelLogEntry,
        val liters: Double,
        val counter: Double,
        val unit: FuelCounterUnit,
    )

    private fun parseCounter(value: String): Double? {
        val normalized = value.trim().replace(',', '.')
        if (normalized.isEmpty()) return null
        return normalized.toDoubleOrNull()?.takeIf { it >= 0.0 }
    }
}
