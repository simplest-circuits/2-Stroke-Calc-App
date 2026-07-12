package com.simplestsoft.twostrokecalc.domain.model.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.isSuccess

class AdminApi(
    private val client: HttpClient,
) {
    suspend fun listUsers(): AdminUsersResponse =
        client.get("admin/users").body()

    suspend fun statistics(): AdminStatisticsResponse =
        client.get("admin/statistics").body()

    suspend fun resetUserPassword(userId: String): AdminPasswordResetResponse =
        client.post("admin/users/$userId/reset-password").body()

    suspend fun setUserBanned(userId: String, body: AdminUserBanRequest): Boolean =
        client.post("admin/users/$userId/ban") {
            setBody(body)
        }.status.isSuccess()

    suspend fun setUserActive(userId: String, body: AdminUserActiveRequest): Boolean =
        client.post("admin/users/$userId/activate") {
            setBody(body)
        }.status.isSuccess()

    suspend fun deleteUser(userId: String): Boolean =
        client.delete("admin/users/$userId").status.isSuccess()

    suspend fun updateUserRole(userId: String, body: AdminUserRoleRequest): Boolean =
        client.post("admin/users/$userId/update-role") {
            setBody(body)
        }.status.isSuccess()

    suspend fun setUserPro(userId: String, body: AdminUserProRequest): Boolean =
        client.post("admin/users/$userId/pro") {
            setBody(body)
        }.status.isSuccess()

    suspend fun getSettings(): AdminSettingsDto =
        client.get("admin/settings").body()

    suspend fun updateSettings(body: AdminSettingsDto): Boolean =
        client.put("admin/settings") {
            setBody(body)
        }.status.isSuccess()

    suspend fun sendPushNotification(body: AdminPushNotificationRequest): AdminPushNotificationResponse =
        client.post("admin/notifications/push-all") {
            setBody(body)
        }.body()
}
