package com.simplestsoft.twostrokecalc.data.sync

import com.simplestsoft.twostrokecalc.domain.model.remote.AccountApi
import com.simplestsoft.twostrokecalc.domain.model.remote.DeviceInfoSyncRequest
import com.simplestsoft.twostrokecalc.domain.model.remote.DeviceSyncPayload
import com.simplestsoft.twostrokecalc.logging.ErrorLogMetadataCollector
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountSyncService @Inject constructor(
    private val accountApi: AccountApi,
    private val errorLogMetadataCollector: ErrorLogMetadataCollector,
) {

    suspend fun pushDeviceInfo(userId: String) {
        val device = currentDevicePayload()
        runCatching {
            accountApi.syncDeviceInfo(DeviceInfoSyncRequest(userId = userId, device = device))
        }
    }

    private fun currentDevicePayload(): DeviceSyncPayload =
        DeviceSyncPayload.fromMap(errorLogMetadataCollector.collectDeviceFields())
}
