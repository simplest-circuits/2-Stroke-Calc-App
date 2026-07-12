package com.simplestsoft.twostrokecalc.domain.model.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeviceSyncPayload(
    @SerialName("applicationId") val applicationId: String? = null,
    @SerialName("appVersionName") val appVersionName: String? = null,
    @SerialName("appVersionCode") val appVersionCode: String? = null,
    @SerialName("buildType") val buildType: String? = null,
    @SerialName("deviceModel") val deviceModel: String? = null,
    @SerialName("deviceManufacturer") val deviceManufacturer: String? = null,
    @SerialName("deviceBrand") val deviceBrand: String? = null,
    @SerialName("deviceProduct") val deviceProduct: String? = null,
    @SerialName("androidVersion") val androidVersion: String? = null,
    @SerialName("androidSdk") val androidSdk: String? = null,
    @SerialName("locale") val locale: String? = null,
    @SerialName("screenWidthPx") val screenWidthPx: String? = null,
    @SerialName("screenHeightPx") val screenHeightPx: String? = null,
    @SerialName("screenDensityDpi") val screenDensityDpi: String? = null,
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

@Serializable
data class DeviceInfoSyncRequest(
    @SerialName("userId") val userId: String,
    @SerialName("device") val device: DeviceSyncPayload,
)

@Serializable
data class AdminUserDeviceDto(
    @SerialName("applicationId") val applicationId: String? = null,
    @SerialName("appVersionName") val appVersionName: String? = null,
    @SerialName("appVersionCode") val appVersionCode: String? = null,
    @SerialName("buildType") val buildType: String? = null,
    @SerialName("deviceModel") val deviceModel: String? = null,
    @SerialName("deviceManufacturer") val deviceManufacturer: String? = null,
    @SerialName("deviceBrand") val deviceBrand: String? = null,
    @SerialName("deviceProduct") val deviceProduct: String? = null,
    @SerialName("androidVersion") val androidVersion: String? = null,
    @SerialName("androidSdk") val androidSdk: String? = null,
    @SerialName("locale") val locale: String? = null,
    @SerialName("screenWidthPx") val screenWidthPx: String? = null,
    @SerialName("screenHeightPx") val screenHeightPx: String? = null,
    @SerialName("screenDensityDpi") val screenDensityDpi: String? = null,
    @SerialName("pushEnabled") val pushEnabled: Boolean? = null,
    @SerialName("hasFcmToken") val hasFcmToken: Boolean? = null,
    @SerialName("updatedAt") val updatedAt: String? = null,
)

@Serializable
data class AdminUserProPurchaseDto(
    @SerialName("productId") val productId: String? = null,
    @SerialName("orderId") val orderId: String? = null,
    @SerialName("packageName") val packageName: String? = null,
    @SerialName("purchaseType") val purchaseType: Int? = null,
    @SerialName("verifiedAt") val verifiedAt: String? = null,
)

@Serializable
data class AdminUserVehicleDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String? = null,
    @SerialName("brand") val brand: String? = null,
    @SerialName("model") val model: String? = null,
    @SerialName("year") val year: String? = null,
    @SerialName("isActive") val isActive: Boolean? = true,
    @SerialName("currentOdometerKm") val currentOdometerKm: String? = null,
    @SerialName("currentOperatingHours") val currentOperatingHours: String? = null,
    @SerialName("updatedAtMs") val updatedAtMs: Long? = null,
)

@Serializable
data class AdminUserDto(
    @SerialName("id") val id: String,
    @SerialName("email") val email: String? = null,
    @SerialName("displayName") val displayName: String? = null,
    @SerialName("role") val role: String? = null,
    @SerialName("active") val active: Boolean? = true,
    @SerialName("banned") val banned: Boolean? = false,
    @SerialName("isPro") val isPro: Boolean? = false,
    @SerialName("phoneNumber") val phoneNumber: String? = null,
    @SerialName("emailVerified") val emailVerified: Boolean? = false,
    @SerialName("authDisabled") val authDisabled: Boolean? = false,
    @SerialName("providerIds") val providerIds: List<String>? = emptyList(),
    @SerialName("vehicleCount") val vehicleCount: Int? = 0,
    @SerialName("vehicles") val vehicles: List<AdminUserVehicleDto>? = emptyList(),
    @SerialName("createdAt") val createdAt: String? = null,
    @SerialName("lastSignInAt") val lastSignInAt: String? = null,
    @SerialName("lastRefreshAt") val lastRefreshAt: String? = null,
    @SerialName("updatedAt") val updatedAt: String? = null,
    @SerialName("lastPasswordResetAt") val lastPasswordResetAt: String? = null,
    @SerialName("bannedAt") val bannedAt: String? = null,
    @SerialName("proPurchase") val proPurchase: AdminUserProPurchaseDto? = null,
    @SerialName("device") val device: AdminUserDeviceDto? = null,
)

@Serializable
data class AdminUsersResponse(
    @SerialName("users") val users: List<AdminUserDto> = emptyList(),
)

@Serializable
data class AdminUserBanRequest(
    @SerialName("banned") val banned: Boolean,
)

@Serializable
data class AdminUserActiveRequest(
    @SerialName("active") val active: Boolean,
)

@Serializable
data class AdminUserRoleRequest(
    @SerialName("role") val role: String,
)

@Serializable
data class AdminPasswordResetResponse(
    @SerialName("message") val message: String? = null,
    @SerialName("email") val email: String? = null,
    @SerialName("resetLink") val resetLink: String? = null,
)

@Serializable
data class AdminStatisticsResponse(
    @SerialName("totalUsers") val totalUsers: Int = 0,
    @SerialName("activeSessions") val activeSessions: Int = 0,
    @SerialName("newToday") val newToday: Int = 0,
    @SerialName("storageUsageMb") val storageUsageMb: Double? = null,
    @SerialName("adminUsers") val adminUsers: Int = 0,
    @SerialName("bannedUsers") val bannedUsers: Int = 0,
    @SerialName("inactiveUsers") val inactiveUsers: Int = 0,
)

@Serializable
data class AdminSettingsDto(
    @SerialName("maintenanceMode") val maintenanceMode: Boolean = false,
    @SerialName("debugMode") val debugMode: Boolean = false,
    @SerialName("emailNotifications") val emailNotifications: Boolean = false,
    @SerialName("demoVehiclesEnabled") val demoVehiclesEnabled: Boolean = true,
    @SerialName("calculatorAvailability") val calculatorAvailability: Map<String, Boolean> = emptyMap(),
    @SerialName("proModules") val proModules: Map<String, Boolean> = emptyMap(),
)

@Serializable
data class CalculatorAvailabilityResponse(
    @SerialName("calculators") val calculators: Map<String, Boolean> = emptyMap(),
)

@Serializable
data class ProModulesResponse(
    @SerialName("modules") val modules: Map<String, Boolean> = emptyMap(),
)

@Serializable
data class DemoVehiclesConfigResponse(
    @SerialName("enabled") val enabled: Boolean = true,
)

@Serializable
data class AdminUserProRequest(
    @SerialName("isPro") val isPro: Boolean,
)

@Serializable
data class VerifyProPurchaseRequest(
    @SerialName("purchaseToken") val purchaseToken: String,
    @SerialName("productId") val productId: String,
    @SerialName("packageName") val packageName: String,
)

@Serializable
data class VerifyProPurchaseResponse(
    @SerialName("isPro") val isPro: Boolean = false,
)

@Serializable
data class AdminPushNotificationRequest(
    @SerialName("title") val title: String,
    @SerialName("body") val body: String,
    @SerialName("type") val type: String? = null,
)

@Serializable
data class AdminPushNotificationResponse(
    @SerialName("success") val success: Boolean = false,
    @SerialName("sentCount") val sentCount: Int = 0,
    @SerialName("failureCount") val failureCount: Int = 0,
    @SerialName("uniqueTokenCount") val uniqueTokenCount: Int = 0,
    @SerialName("message") val message: String? = null,
)
