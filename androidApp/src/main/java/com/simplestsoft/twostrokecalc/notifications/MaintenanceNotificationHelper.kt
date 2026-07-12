package com.simplestsoft.twostrokecalc.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.simplestsoft.twostrokecalc.MainActivity
import com.simplestsoft.twostrokecalc.R
import com.simplestsoft.twostrokecalc.domain.vehicles.MaintenanceDueAlert
import com.simplestsoft.twostrokecalc.domain.vehicles.MaintenanceReminderSchedule
import com.simplestsoft.twostrokecalc.domain.vehicles.MaintenanceReminderType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MaintenanceNotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val notificationManager = NotificationManagerCompat.from(context)

    fun areNotificationsEnabled(): Boolean = notificationManager.areNotificationsEnabled()

    fun canPost(): Boolean {
        if (!areNotificationsEnabled()) return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun showMaintenanceDue(alert: MaintenanceDueAlert, milestoneOffset: Int) {
        if (!canPost()) return

        ensureChannel()

        val title = notificationTitle(alert)
        val timing = milestoneTimingText(milestoneOffset)
        val body = notificationBody(alert, timing)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_VEHICLE_ID, alert.vehicleId)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId(alert.reminderKey, milestoneOffset),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val confirmIntent = Intent(context, MaintenanceReminderConfirmReceiver::class.java).apply {
            action = MaintenanceReminderConfirmReceiver.ACTION_CONFIRM
            putExtra(MaintenanceReminderConfirmReceiver.EXTRA_REMINDER_KEY, alert.reminderKey)
        }
        val confirmPendingIntent = PendingIntent.getBroadcast(
            context,
            "${alert.reminderKey}|confirm|$milestoneOffset".hashCode(),
            confirmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_maintenance)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(contentPendingIntent)
            .addAction(
                0,
                context.getString(R.string.vehicles_notification_confirm_action),
                confirmPendingIntent,
            )
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(alert.reminderKey, milestoneOffset, notification)
    }

    fun cancelAllForReminder(reminderKey: String) {
        MaintenanceReminderSchedule.DATE_MILESTONE_OFFSETS.forEach { offset ->
            notificationManager.cancel(reminderKey, offset)
        }
        MaintenanceReminderSchedule.KM_MILESTONE_OFFSETS.forEach { offset ->
            notificationManager.cancel(reminderKey, offset)
        }
    }

    private fun notificationId(reminderKey: String, milestoneOffset: Int): Int =
        "$reminderKey|$milestoneOffset".hashCode()

    private fun notificationTitle(alert: MaintenanceDueAlert): String = when (alert.type) {
        MaintenanceReminderType.NEXT_SERVICE_DATE ->
            context.getString(R.string.vehicles_notification_service_date_title)
        MaintenanceReminderType.NEXT_SERVICE_KM ->
            context.getString(R.string.vehicles_notification_service_km_title)
        MaintenanceReminderType.TUV ->
            context.getString(R.string.vehicles_notification_tuv_title)
        MaintenanceReminderType.INSURANCE ->
            context.getString(R.string.vehicles_notification_insurance_title)
        MaintenanceReminderType.OIL_INTERVAL ->
            context.getString(R.string.vehicles_notification_oil_title)
        MaintenanceReminderType.SPARK_INTERVAL ->
            context.getString(R.string.vehicles_notification_spark_title)
        MaintenanceReminderType.VARIATOR_INTERVAL ->
            context.getString(R.string.vehicles_notification_variator_title)
        MaintenanceReminderType.BRAKE_INTERVAL ->
            context.getString(R.string.vehicles_notification_brake_title)
        MaintenanceReminderType.TIRE_INTERVAL ->
            context.getString(R.string.vehicles_notification_tire_title)
    }

    private fun notificationBody(alert: MaintenanceDueAlert, timing: String): String {
        val detailSuffix = when (alert.type) {
            MaintenanceReminderType.NEXT_SERVICE_KM,
            MaintenanceReminderType.OIL_INTERVAL,
            MaintenanceReminderType.SPARK_INTERVAL,
            MaintenanceReminderType.VARIATOR_INTERVAL,
            MaintenanceReminderType.BRAKE_INTERVAL,
            MaintenanceReminderType.TIRE_INTERVAL,
            -> " ${alert.detail} km"
            else -> " ${alert.detail}"
        }
        return context.getString(
            R.string.vehicles_notification_body_with_timing,
            alert.vehicleTitle,
            detailSuffix.trim(),
            timing,
        )
    }

    private fun milestoneTimingText(offset: Int): String = when (offset) {
        -21 -> context.getString(R.string.vehicles_notification_timing_weeks_before_3)
        -7 -> context.getString(R.string.vehicles_notification_timing_week_before_1)
        -1 -> context.getString(R.string.vehicles_notification_timing_day_before_1)
        0 -> context.getString(R.string.vehicles_notification_timing_today)
        1 -> context.getString(R.string.vehicles_notification_timing_day_after_1)
        7 -> context.getString(R.string.vehicles_notification_timing_week_after_1)
        21 -> context.getString(R.string.vehicles_notification_timing_weeks_after_3)
        else -> context.getString(R.string.vehicles_notification_timing_today)
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.vehicles_notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.vehicles_notification_channel_desc)
        }
        context.getSystemService(NotificationManager::class.java)
            ?.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "vehicle_maintenance"
    }
}
