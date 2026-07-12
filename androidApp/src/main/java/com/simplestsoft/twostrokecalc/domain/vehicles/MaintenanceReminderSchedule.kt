package com.simplestsoft.twostrokecalc.domain.vehicles

import java.time.LocalDate
import java.time.temporal.ChronoUnit

object MaintenanceReminderSchedule {

    const val WINDOW_DAYS = 21L

    val DATE_MILESTONE_OFFSETS = listOf(-21, -7, -1, 0, 1, 7, 21)

    val KM_MILESTONE_OFFSETS = listOf(0, 1, 7, 21)

    fun isDateInWindow(dueDate: LocalDate, today: LocalDate): Boolean {
        val start = dueDate.minusDays(WINDOW_DAYS)
        val end = dueDate.plusDays(WINDOW_DAYS)
        return !today.isBefore(start) && !today.isAfter(end)
    }

    fun isKmInWindow(anchorDate: LocalDate, today: LocalDate): Boolean {
        val end = anchorDate.plusDays(WINDOW_DAYS)
        return !today.isBefore(anchorDate) && !today.isAfter(end)
    }

    /**
     * Returns the milestone offset when [today] matches one of [allowedOffsets],
     * where offset is days relative to [dueDate] (negative = before, positive = after).
     */
    fun milestoneOffsetForToday(
        dueDate: LocalDate,
        today: LocalDate,
        allowedOffsets: List<Int>,
    ): Int? {
        val offset = ChronoUnit.DAYS.between(dueDate, today).toInt()
        return offset.takeIf { it in allowedOffsets }
    }

    fun milestoneOffsetsFor(type: MaintenanceReminderType): List<Int> =
        if (type.isDateBased()) DATE_MILESTONE_OFFSETS else KM_MILESTONE_OFFSETS

    fun MaintenanceReminderType.isDateBased(): Boolean = when (this) {
        MaintenanceReminderType.NEXT_SERVICE_DATE,
        MaintenanceReminderType.TUV,
        MaintenanceReminderType.INSURANCE,
        -> true
        else -> false
    }
}
