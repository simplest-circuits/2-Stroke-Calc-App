package com.simplestsoft.twostrokecalc.domain.model.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.isSuccess

class AccountApi(
    private val client: HttpClient,
) {
    suspend fun ensureProfile(): Boolean =
        client.post("user/ensure-profile").status.isSuccess()

    suspend fun syncDeviceInfo(body: DeviceInfoSyncRequest): Boolean =
        client.post("user/device-info") {
            setBody(body)
        }.status.isSuccess()

    suspend fun verifyProPurchase(body: VerifyProPurchaseRequest): VerifyProPurchaseResponse =
        client.post("user/verify-pro-purchase") {
            setBody(body)
        }.body()

    suspend fun deleteOwnAccount(): Boolean =
        client.delete("user/account").status.isSuccess()
}
