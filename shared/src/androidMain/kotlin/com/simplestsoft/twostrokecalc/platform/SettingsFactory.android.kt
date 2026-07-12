package com.simplestsoft.twostrokecalc.platform

import android.content.Context
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings

private lateinit var appContext: Context

fun initAndroidSettings(context: Context) {
    appContext = context.applicationContext
}

actual fun createSettings(name: String): Settings =
    SharedPreferencesSettings(appContext.getSharedPreferences(name, Context.MODE_PRIVATE))
