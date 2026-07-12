package com.simplestsoft.twostrokecalc

import android.app.Application
import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.simplestsoft.twostrokecalc.di.sharedKoinModules
import com.simplestsoft.twostrokecalc.notifications.MaintenanceReminderScheduler
import com.simplestsoft.twostrokecalc.platform.initAndroidSettings
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin

@HiltAndroidApp
class AppApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var maintenanceReminderScheduler: MaintenanceReminderScheduler

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        initAndroidSettings(this)
        if (GlobalContext.getOrNull() == null) {
            startKoin {
                androidContext(this@AppApplication)
                modules(sharedKoinModules())
            }
        }
    }

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
