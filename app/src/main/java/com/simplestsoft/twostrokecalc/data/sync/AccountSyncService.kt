package com.simplestsoft.twostrokecalc.data.sync

import com.simplestsoft.twostrokecalc.data.remote.AccountApiService
import com.simplestsoft.twostrokecalc.domain.model.remote.DeviceInfoSyncRequest
import com.simplestsoft.twostrokecalc.domain.model.remote.DeviceSyncPayload
import com.simplestsoft.twostrokecalc.logging.ErrorLogMetadataCollector
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountSyncService @Inject constructor(
    private val accountApiService: AccountApiService,
    private val errorLogMetadataCollector: ErrorLogMetadataCollector,
) {

    suspend fun pushDeviceInfo(userId: String) {
        val device = currentDevicePayload()
        runCatching {
            accountApiService.syncDeviceInfo(DeviceInfoSyncRequest(userId = userId, device = device))
        }
    }

    private fun currentDevicePayload(): DeviceSyncPayload =
        DeviceSyncPayload.fromMap(errorLogMetadataCollector.collectDeviceFields())
}
