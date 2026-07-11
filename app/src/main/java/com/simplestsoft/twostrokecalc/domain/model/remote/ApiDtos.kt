package com.simplestsoft.twostrokecalc.domain.model.remote

import com.google.gson.annotations.SerializedName

data class DeviceSyncPayload(
    @SerializedName("applicationId") val applicationId: String? = null,
    @SerializedName("appVersionName") val appVersionName: String? = null,
    @SerializedName("appVersionCode") val appVersionCode: String? = null,
    @SerializedName("buildType") val buildType: String? = null,
    @SerializedName("deviceModel") val deviceModel: String? = null,
    @SerializedName("deviceManufacturer") val deviceManufacturer: String? = null,
    @SerializedName("deviceBrand") val deviceBrand: String? = null,
    @SerializedName("deviceProduct") val deviceProduct: String? = null,
    @SerializedName("androidVersion") val androidVersion: String? = null,
    @SerializedName("androidSdk") val androidSdk: String? = null,
    @SerializedName("locale") val locale: String? = null,
    @SerializedName("screenWidthPx") val screenWidthPx: String? = null,
    @SerializedName("screenHeightPx") val screenHeightPx: String? = null,
    @SerializedName("screenDensityDpi") val screenDensityDpi: String? = null,
) {
    companion object {
        fun fromMap(fields: Map<String, String>): DeviceSyncPayload = DeviceSyncPayload(
            applicationId = fields["applicationId"],
            appVersionName = fields["appVersionName"],
            appVersionCode = fields["appVersionCode"],
            buildType = fields["buildType"],
            deviceModel = fields["deviceModel"],
            deviceManufacturer = fields["deviceManufacturer"],
            deviceBrand = fields["deviceBrand"],
            deviceProduct = fields["deviceProduct"],
            androidVersion = fields["androidVersion"],
            androidSdk = fields["androidSdk"],
            locale = fields["locale"],
            screenWidthPx = fields["screenWidthPx"],
            screenHeightPx = fields["screenHeightPx"],
            screenDensityDpi = fields["screenDensityDpi"],
        )
    }
}

data class DeviceInfoSyncRequest(
    @SerializedName("userId") val userId: String,
    @SerializedName("device") val device: DeviceSyncPayload,
)

data class AdminUserDeviceDto(
    @SerializedName("applicationId") val applicationId: String? = null,
    @SerializedName("appVersionName") val appVersionName: String? = null,
    @SerializedName("appVersionCode") val appVersionCode: String? = null,
    @SerializedName("buildType") val buildType: String? = null,
    @SerializedName("deviceModel") val deviceModel: String? = null,
    @SerializedName("deviceManufacturer") val deviceManufacturer: String? = null,
    @SerializedName("deviceBrand") val deviceBrand: String? = null,
    @SerializedName("deviceProduct") val deviceProduct: String? = null,
    @SerializedName("androidVersion") val androidVersion: String? = null,
    @SerializedName("androidSdk") val androidSdk: String? = null,
    @SerializedName("locale") val locale: String? = null,
    @SerializedName("screenWidthPx") val screenWidthPx: String? = null,
    @SerializedName("screenHeightPx") val screenHeightPx: String? = null,
    @SerializedName("screenDensityDpi") val screenDensityDpi: String? = null,
    @SerializedName("updatedAt") val updatedAt: String? = null,
)

data class AdminUserDto(
    @SerializedName("id") val id: String,
    @SerializedName("email") val email: String? = null,
    @SerializedName("displayName") val displayName: String? = null,
    @SerializedName("role") val role: String? = null,
    @SerializedName("active") val active: Boolean? = true,
    @SerializedName("banned") val banned: Boolean? = false,
    @SerializedName("isPro") val isPro: Boolean? = false,
    @SerializedName("vehicleCount") val vehicleCount: Int? = 0,
    @SerializedName("createdAt") val createdAt: String? = null,
    @SerializedName("lastSignInAt") val lastSignInAt: String? = null,
    @SerializedName("device") val device: AdminUserDeviceDto? = null,
)

data class AdminUsersResponse(
    @SerializedName("users") val users: List<AdminUserDto> = emptyList(),
)

data class AdminUserBanRequest(
    @SerializedName("banned") val banned: Boolean,
)

data class AdminUserActiveRequest(
    @SerializedName("active") val active: Boolean,
)

data class AdminUserRoleRequest(
    @SerializedName("role") val role: String,
)

data class AdminPasswordResetResponse(
    @SerializedName("message") val message: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("resetLink") val resetLink: String? = null,
)

data class AdminStatisticsResponse(
    @SerializedName("totalUsers") val totalUsers: Int = 0,
    @SerializedName("activeSessions") val activeSessions: Int = 0,
    @SerializedName("newToday") val newToday: Int = 0,
    @SerializedName("storageUsageMb") val storageUsageMb: Double? = null,
    @SerializedName("adminUsers") val adminUsers: Int = 0,
    @SerializedName("bannedUsers") val bannedUsers: Int = 0,
    @SerializedName("inactiveUsers") val inactiveUsers: Int = 0,
)

data class AdminSettingsDto(
    @SerializedName("maintenanceMode") val maintenanceMode: Boolean = false,
    @SerializedName("debugMode") val debugMode: Boolean = false,
    @SerializedName("emailNotifications") val emailNotifications: Boolean = false,
    @SerializedName("calculatorAvailability") val calculatorAvailability: Map<String, Boolean> = emptyMap(),
    @SerializedName("proModules") val proModules: Map<String, Boolean> = emptyMap(),
)

data class CalculatorAvailabilityResponse(
    @SerializedName("calculators") val calculators: Map<String, Boolean> = emptyMap(),
)

data class ProModulesResponse(
    @SerializedName("modules") val modules: Map<String, Boolean> = emptyMap(),
)

data class AdminUserProRequest(
    @SerializedName("isPro") val isPro: Boolean,
)

data class VerifyProPurchaseRequest(
    @SerializedName("purchaseToken") val purchaseToken: String,
    @SerializedName("productId") val productId: String,
    @SerializedName("packageName") val packageName: String,
)

data class VerifyProPurchaseResponse(
    @SerializedName("isPro") val isPro: Boolean = false,
)

data class AdminPushNotificationRequest(
    @SerializedName("title") val title: String,
    @SerializedName("body") val body: String,
    @SerializedName("type") val type: String? = null,
)

data class AdminPushNotificationResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("sentCount") val sentCount: Int = 0,
    @SerializedName("failureCount") val failureCount: Int = 0,
    @SerializedName("uniqueTokenCount") val uniqueTokenCount: Int = 0,
    @SerializedName("message") val message: String? = null,
)
