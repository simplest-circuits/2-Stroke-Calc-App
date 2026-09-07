package com.simplestsoft.twostrokecalc.ui.tools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simplestsoft.twostrokecalc.data.config.ProAccessRepository
import com.simplestsoft.twostrokecalc.data.config.ToolAvailabilityRepository
import com.simplestsoft.twostrokecalc.data.tools.ToolSessionRepository
import com.simplestsoft.twostrokecalc.domain.model.ToolId
import com.simplestsoft.twostrokecalc.domain.model.ToolMeasurementSession
import com.simplestsoft.twostrokecalc.domain.toolDisplayOrder
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class ToolsViewModel @Inject constructor(
    private val toolAvailabilityRepository: ToolAvailabilityRepository,
    private val proAccessRepository: ProAccessRepository,
    private val toolSessionRepository: ToolSessionRepository,
    private val toolSessionFirestoreSync: com.simplestsoft.twostrokecalc.data.tools.ToolSessionFirestoreSync,
) : ViewModel() {

    val enabledTools: StateFlow<Set<ToolId>> = toolAvailabilityRepository.enabledTools

    val visibleDisplayOrder: StateFlow<List<ToolId>> = combine(
        enabledTools,
        proAccessRepository.state,
    ) { enabled, _ ->
        toolDisplayOrder.filter { it in enabled }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        toolAvailabilityRepository.visibleDisplayOrder(),
    )

    val readOnlyTools: StateFlow<Set<ToolId>> = combine(
        enabledTools,
        proAccessRepository.state,
    ) { enabled, proAccess ->
        enabled.filter { proAccess.isToolReadOnly(it) }.toSet()
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptySet(),
    )

    val sessions: StateFlow<List<ToolMeasurementSession>> = toolSessionRepository.sessions

    init {
        viewModelScope.launch {
            toolAvailabilityRepository.refresh()
            runCatching { toolSessionFirestoreSync.pullAndMerge() }
        }
    }

    fun isEnabled(id: ToolId): Boolean = toolAvailabilityRepository.isEnabled(id)

    fun canOpen(id: ToolId): Boolean = isEnabled(id)

    fun canSave(id: ToolId): Boolean =
        isEnabled(id) && proAccessRepository.state.value.canSaveToolSession(id)

    fun isReadOnly(id: ToolId): Boolean =
        isEnabled(id) && proAccessRepository.state.value.isToolReadOnly(id)

    fun sessionsForTool(toolId: ToolId): List<ToolMeasurementSession> =
        toolSessionRepository.sessionsForTool(toolId)

    fun sessionsForVehicle(vehicleId: String): List<ToolMeasurementSession> =
        toolSessionRepository.sessionsForVehicle(vehicleId)

    fun saveSession(session: ToolMeasurementSession) {
        toolSessionRepository.save(session)
        viewModelScope.launch {
            runCatching { toolSessionFirestoreSync.pushAll() }
        }
    }

    fun deleteSession(id: String) {
        toolSessionRepository.delete(id)
        viewModelScope.launch {
            runCatching { toolSessionFirestoreSync.pushAll() }
        }
    }
}
