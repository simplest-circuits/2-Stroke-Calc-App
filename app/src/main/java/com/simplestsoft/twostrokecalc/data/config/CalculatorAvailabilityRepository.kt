package com.simplestsoft.twostrokecalc.data.config

import com.simplestsoft.twostrokecalc.data.preferences.PreferencesManager
import com.simplestsoft.twostrokecalc.data.remote.AppConfigApiService
import com.simplestsoft.twostrokecalc.domain.model.CalculatorId
import com.simplestsoft.twostrokecalc.ui.calculator.calculatorDisplayOrder
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class CalculatorAvailabilityRepository @Inject constructor(
    private val appConfigApiService: AppConfigApiService,
    private val preferencesManager: PreferencesManager,
) {
    private val _enabledCalculators = MutableStateFlow(CalculatorId.entries.toSet())
    val enabledCalculators: StateFlow<Set<CalculatorId>> = _enabledCalculators.asStateFlow()

    fun visibleDisplayOrder(): List<CalculatorId> =
        calculatorDisplayOrder.filter { it in _enabledCalculators.value }

    fun isEnabled(id: CalculatorId): Boolean = id in _enabledCalculators.value

    suspend fun refresh() {
        val cachedDisabled = preferencesManager.getDisabledCalculators()
        if (cachedDisabled.isNotEmpty()) {
            _enabledCalculators.value = CalculatorId.entries.filter { it !in cachedDisabled }.toSet()
        }
        runCatching { appConfigApiService.getCalculatorAvailability() }
            .onSuccess { response ->
                val enabled = parseEnabled(response.calculators)
                preferencesManager.setDisabledCalculators(enabled.disabledIds())
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
