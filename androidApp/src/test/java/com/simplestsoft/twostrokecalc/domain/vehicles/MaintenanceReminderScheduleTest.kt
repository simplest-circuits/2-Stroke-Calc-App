package com.simplestsoft.twostrokecalc.domain.vehicles

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MaintenanceReminderScheduleTest {

    private val dueDate = LocalDate.of(2026, 6, 25)

    @Test
    fun `date window spans three weeks before and after`() {
        assertFalse(MaintenanceReminderSchedule.isDateInWindow(dueDate, dueDate.minusDays(22)))
        assertTrue(MaintenanceReminderSchedule.isDateInWindow(dueDate, dueDate.minusDays(21)))
        assertTrue(MaintenanceReminderSchedule.isDateInWindow(dueDate, dueDate.plusDays(21)))
        assertFalse(MaintenanceReminderSchedule.isDateInWindow(dueDate, dueDate.plusDays(22)))
    }

    @Test
    fun `all date milestones are detected on the correct day`() {
        val offsets = listOf(-21, -7, -1, 0, 1, 7, 21)
        offsets.forEach { offset ->
            val today = dueDate.plusDays(offset.toLong())
            val milestone = MaintenanceReminderSchedule.milestoneOffsetForToday(
                dueDate = dueDate,
                today = today,
                allowedOffsets = MaintenanceReminderSchedule.DATE_MILESTONE_OFFSETS,
            )
            assertEquals(offset, milestone)
        }
    }

    @Test
    fun `non-milestone days return null`() {
        val today = dueDate.minusDays(10)
        val milestone = MaintenanceReminderSchedule.milestoneOffsetForToday(
            dueDate = dueDate,
            today = today,
            allowedOffsets = MaintenanceReminderSchedule.DATE_MILESTONE_OFFSETS,
        )
        assertNull(milestone)
    }

    @Test
    fun `km window starts on anchor date and ends after three weeks`() {
        val anchor = LocalDate.of(2026, 6, 1)
        assertFalse(MaintenanceReminderSchedule.isKmInWindow(anchor, anchor.minusDays(1)))
        assertTrue(MaintenanceReminderSchedule.isKmInWindow(anchor, anchor))
        assertTrue(MaintenanceReminderSchedule.isKmInWindow(anchor, anchor.plusDays(21)))
        assertFalse(MaintenanceReminderSchedule.isKmInWindow(anchor, anchor.plusDays(22)))
    }
}
