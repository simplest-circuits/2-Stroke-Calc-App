package com.simplestsoft.twostrokecalc.data.remote

import com.simplestsoft.twostrokecalc.domain.model.remote.DeviceInfoSyncRequest
import com.simplestsoft.twostrokecalc.domain.model.remote.VerifyProPurchaseRequest
import com.simplestsoft.twostrokecalc.domain.model.remote.VerifyProPurchaseResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.POST

interface AccountApiService {
    @POST("user/ensure-profile")
    suspend fun ensureProfile(): Response<Unit>

    @POST("user/device-info")
    suspend fun syncDeviceInfo(@Body body: DeviceInfoSyncRequest): Response<Unit>

    @POST("user/verify-pro-purchase")
    suspend fun verifyProPurchase(@Body body: VerifyProPurchaseRequest): VerifyProPurchaseResponse

    @DELETE("user/account")
    suspend fun deleteOwnAccount(): Response<Unit>
}