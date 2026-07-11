package com.simplestsoft.twostrokecalc.data.remote

import com.simplestsoft.twostrokecalc.domain.model.remote.AdminPasswordResetResponse
import com.simplestsoft.twostrokecalc.domain.model.remote.AdminPushNotificationRequest
import com.simplestsoft.twostrokecalc.domain.model.remote.AdminPushNotificationResponse
import com.simplestsoft.twostrokecalc.domain.model.remote.AdminSettingsDto
import com.simplestsoft.twostrokecalc.domain.model.remote.AdminStatisticsResponse
import com.simplestsoft.twostrokecalc.domain.model.remote.AdminUserActiveRequest
import com.simplestsoft.twostrokecalc.domain.model.remote.AdminUserBanRequest
import com.simplestsoft.twostrokecalc.domain.model.remote.AdminUserProRequest
import com.simplestsoft.twostrokecalc.domain.model.remote.AdminUserRoleRequest
import com.simplestsoft.twostrokecalc.domain.model.remote.AdminUsersResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface AdminApiService {
    @GET("admin/users")
    suspend fun listUsers(): AdminUsersResponse

    @GET("admin/statistics")
    suspend fun statistics(): AdminStatisticsResponse

    @POST("admin/users/{userId}/reset-password")
    suspend fun resetUserPassword(@Path("userId") userId: String): AdminPasswordResetResponse

    @POST("admin/users/{userId}/ban")
    suspend fun setUserBanned(
        @Path("userId") userId: String,
        @Body body: AdminUserBanRequest,
    ): Response<Unit>

    @POST("admin/users/{userId}/activate")
    suspend fun setUserActive(
        @Path("userId") userId: String,
        @Body body: AdminUserActiveRequest,
    ): Response<Unit>

    @DELETE("admin/users/{userId}")
    suspend fun deleteUser(@Path("userId") userId: String): Response<Unit>

    @POST("admin/users/{userId}/update-role")
    suspend fun updateUserRole(
        @Path("userId") userId: String,
        @Body body: AdminUserRoleRequest,
    ): Response<Unit>

    @POST("admin/users/{userId}/pro")
    suspend fun setUserPro(
        @Path("userId") userId: String,
        @Body body: AdminUserProRequest,
    ): Response<Unit>

    @GET("admin/settings")
    suspend fun getSettings(): AdminSettingsDto

    @PUT("admin/settings")
    suspend fun updateSettings(@Body body: AdminSettingsDto): Response<Unit>

    @POST("admin/notifications/push-all")
    suspend fun sendPushNotification(@Body body: AdminPushNotificationRequest): AdminPushNotificationResponse
}
