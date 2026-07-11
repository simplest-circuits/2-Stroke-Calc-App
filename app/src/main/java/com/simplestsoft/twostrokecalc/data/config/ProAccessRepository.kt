package com.simplestsoft.twostrokecalc.data.config

import com.simplestsoft.twostrokecalc.data.preferences.PreferencesManager
import com.simplestsoft.twostrokecalc.data.remote.AppConfigApiService
import com.simplestsoft.twostrokecalc.domain.model.CalculatorId
import com.simplestsoft.twostrokecalc.domain.model.ProModuleId
import javax.inject.Inject
import javax.inject.Singleton
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

    fun canEditVehicles(): Boolean = hasVehiclesAccess()

    fun canEditCalculator(id: CalculatorId): Boolean = hasCalculatorAccess(id)

    fun isCalculatorReadOnly(id: CalculatorId): Boolean = isCalculatorLocked(id)

    fun isLocked(module: ProModuleId): Boolean = module in proModules && !hasAccess(module)

    fun isCalculatorLocked(id: CalculatorId): Boolean = isLocked(ProModuleId.fromCalculator(id))
}

@Singleton
class ProAccessRepository @Inject constructor(
    private val appConfigApiService: AppConfigApiService,
    private val preferencesManager: PreferencesManager,
) {
    private val _state = MutableStateFlow(ProAccessState())
    val state: StateFlow<ProAccessState> = _state.asStateFlow()

    suspend fun refresh() {
        val isPro = preferencesManager.getIsPro()
        val isAdmin = preferencesManager.getIsAdmin()
        _state.value = _state.value.copy(isPro = isPro, isAdmin = isAdmin)
        runCatching { appConfigApiService.getProModules() }
            .onSuccess { response ->
                val modules = parseProModules(response.modules)
                preferencesManager.setProModules(modules)
                _state.value = _state.value.copy(proModules = modules)
            }
            .onFailure {
                val cached = preferencesManager.getProModules()
                if (cached.isNotEmpty()) {
                    _state.value = _state.value.copy(proModules = cached)
                }
            }
    }

    fun applyUserEntitlements(isPro: Boolean, isAdmin: Boolean) {
        _state.value = _state.value.copy(isPro = isPro, isAdmin = isAdmin)
    }

    fun applyProModulesMap(map: Map<String, Boolean>) {
        val modules = parseProModules(map)
        _state.value = _state.value.copy(proModules = modules)
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
