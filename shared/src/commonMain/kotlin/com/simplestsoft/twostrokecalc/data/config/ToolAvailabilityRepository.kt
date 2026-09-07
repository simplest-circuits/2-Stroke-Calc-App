package com.simplestsoft.twostrokecalc.data.config

import com.simplestsoft.twostrokecalc.data.preferences.AppPreferencesStore
import com.simplestsoft.twostrokecalc.domain.model.ToolId
import com.simplestsoft.twostrokecalc.domain.model.remote.AppConfigApi
import com.simplestsoft.twostrokecalc.domain.toolDisplayOrder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ToolAvailabilityRepository(
    private val appConfigApi: AppConfigApi,
    private val preferencesStore: AppPreferencesStore,
) {
    private val _enabledTools = MutableStateFlow(ToolId.entries.toSet())
    val enabledTools: StateFlow<Set<ToolId>> = _enabledTools.asStateFlow()

    fun visibleDisplayOrder(): List<ToolId> =
        toolDisplayOrder.filter { it in _enabledTools.value }

    fun isEnabled(id: ToolId): Boolean = id in _enabledTools.value

    suspend fun refresh() {
        val cachedDisabled = preferencesStore.getDisabledTools()
        if (cachedDisabled.isNotEmpty()) {
            _enabledTools.value = ToolId.entries.filter { it !in cachedDisabled }.toSet()
        }
        runCatching { appConfigApi.getToolAvailability() }
            .onSuccess { response ->
                val enabled = parseEnabled(response.tools)
                preferencesStore.setDisabledTools(enabled.disabledIds())
                _enabledTools.value = enabled
            }
    }

    fun applyAvailabilityMap(map: Map<String, Boolean>) {
        val enabled = parseEnabled(map)
        _enabledTools.value = enabled
        preferencesStore.setDisabledTools(enabled.disabledIds())
    }

    private fun parseEnabled(map: Map<String, Boolean>): Set<ToolId> {
        if (map.isEmpty()) return ToolId.entries.toSet()
        val normalized = ToolId.normalizeAvailabilityMap(map)
        return ToolId.entries.filter { id -> normalized[id.name] != false }.toSet()
    }

    private fun Set<ToolId>.disabledIds(): Set<ToolId> =
        ToolId.entries.filter { it !in this }.toSet()
}
