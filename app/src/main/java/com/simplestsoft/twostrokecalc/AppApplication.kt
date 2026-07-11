package com.simplestsoft.twostrokecalc

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.simplestsoft.twostrokecalc.notifications.MaintenanceReminderScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class AppApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var maintenanceReminderScheduler: MaintenanceReminderScheduler

    override fun onCreate() {
        super.onCreate()
        maintenanceReminderScheduler.schedulePeriodicChecks()
        maintenanceReminderScheduler.runCheckSoon()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
