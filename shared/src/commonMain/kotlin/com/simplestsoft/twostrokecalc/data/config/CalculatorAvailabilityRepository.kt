package com.simplestsoft.twostrokecalc.data.config

import com.simplestsoft.twostrokecalc.data.preferences.AppPreferencesStore
import com.simplestsoft.twostrokecalc.domain.calculatorDisplayOrder
import com.simplestsoft.twostrokecalc.domain.model.CalculatorId
import com.simplestsoft.twostrokecalc.domain.model.remote.AppConfigApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CalculatorAvailabilityRepository(
    private val appConfigApi: AppConfigApi,
    private val preferencesStore: AppPreferencesStore,
) {
    private val _enabledCalculators = MutableStateFlow(CalculatorId.entries.toSet())
    val enabledCalculators: StateFlow<Set<CalculatorId>> = _enabledCalculators.asStateFlow()

    fun visibleDisplayOrder(): List<CalculatorId> =
        calculatorDisplayOrder.filter { it in _enabledCalculators.value }

    fun isEnabled(id: CalculatorId): Boolean = id in _enabledCalculators.value

    suspend fun refresh() {
        val cachedDisabled = preferencesStore.getDisabledCalculators()
        if (cachedDisabled.isNotEmpty()) {
            _enabledCalculators.value = CalculatorId.entries.filter { it !in cachedDisabled }.toSet()
        }
        runCatching { appConfigApi.getCalculatorAvailability() }
            .onSuccess { response ->
                val enabled = parseEnabled(response.calculators)
                preferencesStore.setDisabledCalculators(enabled.disabledIds())
                _enabledCalculators.value = enabled
            }
    }

    fun applyAvailabilityMap(map: Map<String, Boolean>) {
        val enabled = parseEnabled(map)
        _enabledCalculators.value = enabled
    }

    private fun parseEnabled(map: Map<String, Boolean>): Set<CalculatorId> {
        val normalized = CalculatorId.normalizeAvailabilityMap(map)
        return CalculatorId.entries.filter { id -> normalized[id.name] != false }.toSet()
    }

    private fun Set<CalculatorId>.disabledIds(): Set<CalculatorId> =
        CalculatorId.entries.filter { it !in this }.toSet()
}
