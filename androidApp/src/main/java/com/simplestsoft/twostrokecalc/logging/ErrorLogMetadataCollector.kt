package com.simplestsoft.twostrokecalc.logging

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import com.google.firebase.auth.FirebaseAuth
import com.simplestsoft.twostrokecalc.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ErrorLogMetadataCollector @Inject constructor(
    @ApplicationContext private val context: Context,
    private val auth: FirebaseAuth,
) {
    private val cachedDeviceFields: Map<String, String> by lazy { collectDeviceFields() }

    fun collectUserFields(): Map<String, String> {
        val user = auth.currentUser ?: return emptyMap()
        return buildMap {
            put("uid", user.uid)
            user.email?.trim()?.takeIf { it.isNotEmpty() }?.let { put("userEmail", it) }
            user.displayName?.trim()?.takeIf { it.isNotEmpty() }?.let { put("userDisplayName", it) }
        }
    }

    fun collectDeviceFields(): Map<String, String> {
        val pm = context.packageManager
        val pkg = context.packageName
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(pkg, 0)
        }
        val displayMetrics = context.resources.displayMetrics
        return buildMap {
            put("applicationId", pkg)
            put("appVersionName", packageInfo.versionName ?: BuildConfig.VERSION_NAME)
            put("appVersionCode", PackageInfoCompat.getLongVersionCode(packageInfo).toString())
            put("buildType", BuildConfig.BUILD_TYPE)
            put("deviceModel", Build.MODEL)
            put("deviceManufacturer", Build.MANUFACTURER)
            put("deviceBrand", Build.BRAND)
            put("deviceProduct", Build.DEVICE)
            put("androidVersion", Build.VERSION.RELEASE)
            put("androidSdk", Build.VERSION.SDK_INT.toString())
            put("locale", Locale.getDefault().toLanguageTag())
            put("screenWidthPx", displayMetrics.widthPixels.toString())
            put("screenHeightPx", displayMetrics.heightPixels.toString())
            put("screenDensityDpi", displayMetrics.densityDpi.toString())
        }
    }

    fun collect(): Map<String, String> = buildMap {
        putAll(cachedDeviceFields)
        putAll(collectUserFields())
    }
}
