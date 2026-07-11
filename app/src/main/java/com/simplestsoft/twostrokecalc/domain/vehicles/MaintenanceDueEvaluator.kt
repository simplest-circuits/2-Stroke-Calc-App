package com.simplestsoft.twostrokecalc.domain.vehicles

import com.simplestsoft.twostrokecalc.domain.model.MaintenanceType
import com.simplestsoft.twostrokecalc.domain.model.Vehicle
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MaintenanceDueEvaluator @Inject constructor() {

    fun evaluate(vehicle: Vehicle, today: LocalDate = LocalDate.now()): List<MaintenanceDueAlert> {
        if (!vehicle.isActive) return emptyList()

        val title = vehicle.displayTitle()
        val schedule = vehicle.serviceSchedule
        val alerts = mutableListOf<MaintenanceDueAlert>()

        fun addDateAlert(
            type: MaintenanceReminderType,
            rawDate: String,
        ) {
            val dueDate = parseDate(rawDate) ?: return
            if (!MaintenanceReminderSchedule.isDateInWindow(dueDate, today)) return
            alerts += MaintenanceDueAlert(
                vehicleId = vehicle.id,
                vehicleTitle = title,
                reminderKey = reminderKey(vehicle.id, type, rawDate.trim()),
                type = type,
                detail = rawDate.trim(),
                dueDate = dueDate,
            )
        }

        fun addKmAlert(
            type: MaintenanceReminderType,
            dueKmRaw: String,
        ) {
            if (!isKmDue(vehicle.currentOdometerKm, dueKmRaw)) return
            alerts += MaintenanceDueAlert(
                vehicleId = vehicle.id,
                vehicleTitle = title,
                reminderKey = reminderKey(vehicle.id, type, dueKmRaw.trim()),
                type = type,
                detail = dueKmRaw.trim(),
            )
        }

        fun addIntervalAlert(
            type: MaintenanceReminderType,
            lastKmRaw: String,
            intervalKmRaw: String,
        ) {
            if (lastKmRaw.isBlank()) return
            val dueKm = intervalDueKm(lastKmRaw, intervalKmRaw) ?: return
            if (!isKmDue(vehicle.currentOdometerKm, dueKm)) return
            alerts += MaintenanceDueAlert(
                vehicleId = vehicle.id,
                vehicleTitle = title,
                reminderKey = reminderKey(vehicle.id, type, dueKm),
                type = type,
                detail = dueKm,
            )
        }

        addDateAlert(MaintenanceReminderType.NEXT_SERVICE_DATE, schedule.nextServiceDueDate)
        addKmAlert(MaintenanceReminderType.NEXT_SERVICE_KM, schedule.nextServiceDueKm)
        addDateAlert(MaintenanceReminderType.TUV, schedule.tuvInspectionDate)
        addDateAlert(MaintenanceReminderType.INSURANCE, schedule.insuranceExpiryDate)

        addIntervalAlert(
            MaintenanceReminderType.OIL_INTERVAL,
            lastServiceKm(vehicle, MaintenanceType.OIL_CHANGE, schedule.lastOilChangeKm),
            schedule.oilChangeIntervalKm,
        )
        addIntervalAlert(
            MaintenanceReminderType.SPARK_INTERVAL,
            lastServiceKm(vehicle, MaintenanceType.SPARK_PLUG, ""),
            schedule.sparkPlugIntervalKm,
        )
        if (vehicleCapabilities(vehicle).showVariatorMaintenance) {
            addIntervalAlert(
                MaintenanceReminderType.VARIATOR_INTERVAL,
                lastServiceKm(vehicle, MaintenanceType.VARIATOR, ""),
                schedule.variatorServiceIntervalKm,
            )
        }
        addIntervalAlert(
            MaintenanceReminderType.BRAKE_INTERVAL,
            lastServiceKm(vehicle, MaintenanceType.BRAKES, ""),
            schedule.brakeServiceIntervalKm,
        )
        addIntervalAlert(
            MaintenanceReminderType.TIRE_INTERVAL,
            lastServiceKm(vehicle, MaintenanceType.TIRES, ""),
            schedule.tireServiceIntervalKm,
        )

        return alerts
    }

    private fun lastServiceKm(
        vehicle: Vehicle,
        type: MaintenanceType,
        fallback: String,
    ): String {
        val fromLog = vehicle.maintenanceLog
            .asSequence()
            .filter { it.type == type }
            .mapNotNull { entry ->
                parseKm(entry.odometerKm)?.let { entry.odometerKm.trim() }
            }
            .maxByOrNull { parseKm(it) ?: 0.0 }
        return fromLog?.takeIf { it.isNotBlank() } ?: fallback.trim()
    }

    private fun isKmDue(currentKmRaw: String, dueKmRaw: String): Boolean {
        val current = parseKm(currentKmRaw) ?: return false
        val due = parseKm(dueKmRaw) ?: return false
        return current >= due
    }

    private fun intervalDueKm(lastKmRaw: String, intervalKmRaw: String): String? {
        val last = parseKm(lastKmRaw) ?: return null
        val interval = parseKm(intervalKmRaw) ?: return null
        if (interval <= 0.0) return null
        val due = last + interval
        return if (due % 1.0 == 0.0) {
            due.toLong().toString()
        } else {
            due.toString()
        }
    }

    private fun parseKm(raw: String): Double? =
        raw.trim()
            .replace(",", ".")
            .takeIf { it.isNotBlank() }
            ?.toDoubleOrNull()

    private fun parseDate(raw: String): LocalDate? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        DATE_FORMATTERS.forEach { formatter ->
            try {
                return LocalDate.parse(trimmed, formatter)
            } catch (_: DateTimeParseException) {
            }
        }
        return null
    }

    private fun reminderKey(vehicleId: String, type: MaintenanceReminderType, value: String): String =
        "$vehicleId|${type.name}|$value"

    companion object {
        private val DATE_FORMATTERS = listOf(
            DateTimeFormatter.ofPattern("d.M.uuuu"),
            DateTimeFormatter.ofPattern("dd.MM.uuuu"),
            DateTimeFormatter.ISO_LOCAL_DATE,
        )
    }
}
