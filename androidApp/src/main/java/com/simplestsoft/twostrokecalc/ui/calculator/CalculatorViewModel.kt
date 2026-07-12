package com.simplestsoft.twostrokecalc.ui.calculator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simplestsoft.twostrokecalc.data.config.CalculatorAvailabilityRepository
import com.simplestsoft.twostrokecalc.data.config.ProAccessRepository
import com.simplestsoft.twostrokecalc.domain.model.CalculatorId
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class CalculatorViewModel @Inject constructor(
    private val calculatorAvailabilityRepository: CalculatorAvailabilityRepository,
    private val proAccessRepository: ProAccessRepository,
) : ViewModel() {

    val enabledCalculators: StateFlow<Set<CalculatorId>> =
        calculatorAvailabilityRepository.enabledCalculators

    val visibleDisplayOrder: StateFlow<List<CalculatorId>> = combine(
        enabledCalculators,
        proAccessRepository.state,
    ) { enabled, _ ->
        calculatorDisplayOrder.filter { it in enabled }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        calculatorAvailabilityRepository.visibleDisplayOrder(),
    )

    val readOnlyCalculators: StateFlow<Set<CalculatorId>> = combine(
        enabledCalculators,
        proAccessRepository.state,
    ) { enabled, proAccess ->
        enabled.filter { proAccess.isCalculatorReadOnly(it) }.toSet()
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptySet(),
    )

    init {
        viewModelScope.launch { calculatorAvailabilityRepository.refresh() }
    }

    fun isEnabled(id: CalculatorId): Boolean = calculatorAvailabilityRepository.isEnabled(id)

    fun canOpen(id: CalculatorId): Boolean = isEnabled(id)

    fun isReadOnly(id: CalculatorId): Boolean =
        isEnabled(id) && proAccessRepository.state.value.isCalculatorReadOnly(id)

    fun canEdit(id: CalculatorId): Boolean =
        isEnabled(id) && proAccessRepository.state.value.canEditCalculator(id)
}
