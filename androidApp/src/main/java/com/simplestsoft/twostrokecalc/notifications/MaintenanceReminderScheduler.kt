package com.simplestsoft.twostrokecalc.notifications

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MaintenanceReminderScheduler @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val workManager = WorkManager.getInstance(context)

    fun schedulePeriodicChecks() {
        val initialDelayMs = millisUntilNextMorning(DEFAULT_CHECK_HOUR)
        val request = PeriodicWorkRequestBuilder<MaintenanceReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelayMs, TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    fun runCheckSoon() {
        val request = OneTimeWorkRequestBuilder<MaintenanceReminderWorker>().build()
        workManager.enqueueUniqueWork(
            IMMEDIATE_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    private fun millisUntilNextMorning(hour: Int): Long {
        val now = LocalDateTime.now()
        var nextRun = now.with(LocalTime.of(hour, 0))
        if (!nextRun.isAfter(now)) {
            nextRun = nextRun.plusDays(1)
        }
        return Duration.between(now, nextRun).toMillis()
    }

    companion object {
        private const val PERIODIC_WORK_NAME = "vehicle_maintenance_periodic"
        private const val IMMEDIATE_WORK_NAME = "vehicle_maintenance_immediate"
        private const val DEFAULT_CHECK_HOUR = 8
    }
}
