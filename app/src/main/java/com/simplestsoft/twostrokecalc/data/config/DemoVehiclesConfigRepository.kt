package com.simplestsoft.twostrokecalc.data.config

import com.simplestsoft.twostrokecalc.data.preferences.PreferencesManager
import com.simplestsoft.twostrokecalc.data.remote.AppConfigApiService
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class DemoVehiclesConfigRepository @Inject constructor(
    private val appConfigApiService: AppConfigApiService,
    private val preferencesManager: PreferencesManager,
) {
    private val _enabled = MutableStateFlow<Boolean?>(null)
    val enabled: StateFlow<Boolean?> = _enabled.asStateFlow()

    suspend fun refresh() {
        val cached = preferencesManager.getDemoVehiclesEnabled()
        _enabled.value = cached
        runCatching { appConfigApiService.getDemoVehiclesConfig() }
            .onSuccess { response ->
                preferencesManager.setDemoVehiclesEnabled(response.enabled)
                _enabled.value = response.enabled
            }
    }

    suspend fun applyEnabledFlag(enabled: Boolean) {
        preferencesManager.setDemoVehiclesEnabled(enabled)
        _enabled.value = enabled
    }
}
