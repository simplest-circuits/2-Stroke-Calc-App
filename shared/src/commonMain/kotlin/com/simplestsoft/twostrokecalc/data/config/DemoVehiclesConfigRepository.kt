package com.simplestsoft.twostrokecalc.data.config

import com.simplestsoft.twostrokecalc.data.preferences.AppPreferencesStore
import com.simplestsoft.twostrokecalc.domain.model.remote.AppConfigApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DemoVehiclesConfigRepository(
    private val appConfigApi: AppConfigApi,
    private val preferencesStore: AppPreferencesStore,
) {
    private val _enabled = MutableStateFlow(true)
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    suspend fun refresh() {
        _enabled.value = preferencesStore.getDemoVehiclesEnabled()
        runCatching { appConfigApi.getDemoVehiclesConfig() }
            .onSuccess { response ->
                preferencesStore.setDemoVehiclesEnabled(response.enabled)
                _enabled.value = response.enabled
            }
    }

    fun applyEnabled(value: Boolean) {
        _enabled.value = value
    }

    suspend fun applyEnabledFlag(enabled: Boolean) {
        preferencesStore.setDemoVehiclesEnabled(enabled)
        _enabled.value = enabled
    }
}
