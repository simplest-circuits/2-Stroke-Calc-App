package com.simplestsoft.twostrokecalc.data.config

import com.simplestsoft.twostrokecalc.data.preferences.AppPreferencesStore
import com.simplestsoft.twostrokecalc.domain.model.CalculatorId
import com.simplestsoft.twostrokecalc.domain.model.ProModuleId
import com.simplestsoft.twostrokecalc.domain.model.ToolId
import com.simplestsoft.twostrokecalc.domain.model.remote.AppConfigApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ProAccessState(
    val isPro: Boolean = false,
    val isAdmin: Boolean = false,
    val proModules: Set<ProModuleId> = ProModuleId.defaultProModules,
) {
    fun hasAccess(module: ProModuleId): Boolean =
        isAdmin || isPro || module !in proModules

    fun hasCalculatorAccess(id: CalculatorId): Boolean =
        hasAccess(ProModuleId.fromCalculator(id))

    fun hasVehiclesAccess(): Boolean = hasAccess(ProModuleId.VEHICLES)

    fun hasToolAccess(id: ToolId): Boolean {
        val module = ProModuleId.fromTool(id) ?: return true
        return hasAccess(module)
    }

    fun canSaveToolSession(id: ToolId): Boolean = hasToolAccess(id)

    fun canEditVehicles(): Boolean = hasVehiclesAccess()

    fun canEditCalculator(id: CalculatorId): Boolean = hasCalculatorAccess(id)

    fun isCalculatorReadOnly(id: CalculatorId): Boolean = isCalculatorLocked(id)

    fun isToolReadOnly(id: ToolId): Boolean {
        val module = ProModuleId.fromTool(id) ?: return false
        return isLocked(module)
    }

    fun isLocked(module: ProModuleId): Boolean = module in proModules && !hasAccess(module)

    fun isCalculatorLocked(id: CalculatorId): Boolean = isLocked(ProModuleId.fromCalculator(id))
}

class ProAccessRepository(
    private val appConfigApi: AppConfigApi,
    private val preferencesStore: AppPreferencesStore,
) {
    private val _state = MutableStateFlow(ProAccessState())
    val state: StateFlow<ProAccessState> = _state.asStateFlow()

    suspend fun refresh() {
        val isPro = preferencesStore.getIsPro()
        val isAdmin = preferencesStore.getIsAdmin()
        _state.value = _state.value.copy(isPro = isPro, isAdmin = isAdmin)
        runCatching { appConfigApi.getProModules() }
            .onSuccess { response ->
                val modules = parseProModules(response.modules)
                preferencesStore.setProModulesRaw(modules.joinToString(",") { it.name })
                _state.value = _state.value.copy(proModules = modules)
            }
            .onFailure {
                val cached = readCachedProModules()
                if (cached.isNotEmpty()) {
                    _state.value = _state.value.copy(proModules = cached)
                }
            }
    }

    fun applyUserEntitlements(isPro: Boolean, isAdmin: Boolean) {
        preferencesStore.setIsPro(isPro)
        preferencesStore.setIsAdmin(isAdmin)
        _state.value = _state.value.copy(isPro = isPro, isAdmin = isAdmin)
    }

    fun setPro(isPro: Boolean) {
        preferencesStore.setIsPro(isPro)
        _state.value = _state.value.copy(isPro = isPro)
    }

    fun applyProModulesMap(map: Map<String, Boolean>) {
        val modules = parseProModules(map)
        _state.value = _state.value.copy(proModules = modules)
    }

    private fun readCachedProModules(): Set<ProModuleId> {
        val raw = preferencesStore.getProModulesRaw() ?: return emptySet()
        if (raw.isBlank()) return emptySet()
        return raw.split(",")
            .mapNotNull { token ->
                val name = token.trim()
                if (name.isEmpty()) null else ProModuleId.fromName(name)
            }
            .toSet()
    }

    private fun parseProModules(map: Map<String, Boolean>): Set<ProModuleId> {
        val normalized = CalculatorId.normalizeAvailabilityMap(map)
        return if (normalized.isEmpty()) {
            ProModuleId.defaultProModules
        } else {
            normalized.entries
                .mapNotNull { (key, enabled) ->
                    if (enabled) ProModuleId.fromName(key) else null
                }
                .toSet()
        }
    }
}
