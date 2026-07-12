package com.simplestsoft.twostrokecalc.notifications

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.simplestsoft.twostrokecalc.data.vehicles.MaintenanceReminderStateStore
import com.simplestsoft.twostrokecalc.data.vehicles.VehicleRepository
import com.simplestsoft.twostrokecalc.domain.vehicles.MaintenanceDueAlert
import com.simplestsoft.twostrokecalc.domain.vehicles.MaintenanceDueEvaluator
import com.simplestsoft.twostrokecalc.domain.vehicles.MaintenanceReminderSchedule
import com.simplestsoft.twostrokecalc.domain.vehicles.MaintenanceReminderSchedule.isDateBased
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate

@HiltWorker
class MaintenanceReminderWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val vehicleRepository: VehicleRepository,
    private val dueEvaluator: MaintenanceDueEvaluator,
    private val stateStore: MaintenanceReminderStateStore,
    private val notificationHelper: MaintenanceNotificationHelper,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        if (!notificationHelper.canPost()) {
            return Result.success()
        }

        val today = LocalDate.now()
        val vehicles = vehicleRepository.getVehicles()
        val alerts = vehicles.flatMap { dueEvaluator.evaluate(it, today) }
        val activeKeys = alerts.map { it.reminderKey }.toSet()
        stateStore.syncActiveKeys(activeKeys)

        alerts.forEach { alert ->
            if (stateStore.isConfirmed(alert.reminderKey)) return@forEach

            val dueDate = resolveDueDate(alert, today) ?: return@forEach
            val allowedOffsets = MaintenanceReminderSchedule.milestoneOffsetsFor(alert.type)
            if (!isInReminderWindow(alert, dueDate, today)) return@forEach

            val milestone = MaintenanceReminderSchedule.milestoneOffsetForToday(
                dueDate = dueDate,
                today = today,
                allowedOffsets = allowedOffsets,
            ) ?: return@forEach

            if (stateStore.hasSentMilestone(alert.reminderKey, milestone)) return@forEach

            notificationHelper.showMaintenanceDue(alert, milestone)
            stateStore.markMilestoneSent(alert.reminderKey, milestone)
        }

        return Result.success()
    }

    private suspend fun resolveDueDate(
        alert: MaintenanceDueAlert,
        today: LocalDate,
    ): LocalDate? {
        if (alert.type.isDateBased()) {
            return alert.dueDate
        }
        return stateStore.getKmAnchor(alert.reminderKey) ?: run {
            stateStore.setKmAnchor(alert.reminderKey, today)
            today
        }
    }

    private fun isInReminderWindow(
        alert: MaintenanceDueAlert,
        dueDate: LocalDate,
        today: LocalDate,
    ): Boolean = if (alert.type.isDateBased()) {
        MaintenanceReminderSchedule.isDateInWindow(dueDate, today)
    } else {
        MaintenanceReminderSchedule.isKmInWindow(dueDate, today)
    }
}
