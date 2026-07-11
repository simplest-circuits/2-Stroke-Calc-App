package com.simplestsoft.twostrokecalc.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.simplestsoft.twostrokecalc.data.vehicles.MaintenanceReminderStateStore
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MaintenanceReminderConfirmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var stateStore: MaintenanceReminderStateStore

    @Inject
    lateinit var notificationHelper: MaintenanceNotificationHelper

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_CONFIRM) return
        val reminderKey = intent.getStringExtra(EXTRA_REMINDER_KEY) ?: return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                stateStore.confirm(reminderKey)
                notificationHelper.cancelAllForReminder(reminderKey)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_CONFIRM = "com.simplestsoft.twostrokecalc.CONFIRM_MAINTENANCE_REMINDER"
        const val EXTRA_REMINDER_KEY = "reminder_key"
    }
}
