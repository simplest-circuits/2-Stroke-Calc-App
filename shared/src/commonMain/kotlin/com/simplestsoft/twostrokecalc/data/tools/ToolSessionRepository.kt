package com.simplestsoft.twostrokecalc.data.tools

import com.simplestsoft.twostrokecalc.data.preferences.AppPreferencesStore
import com.simplestsoft.twostrokecalc.domain.model.ToolId
import com.simplestsoft.twostrokecalc.domain.model.ToolMeasurementSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ToolSessionRepository(
    private val preferencesStore: AppPreferencesStore,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val _sessions = MutableStateFlow(loadSessions())
    val sessions: StateFlow<List<ToolMeasurementSession>> = _sessions.asStateFlow()

    fun sessionsForTool(toolId: ToolId): List<ToolMeasurementSession> =
        _sessions.value.filter { it.toolId == toolId }.sortedByDescending { it.createdAt }

    fun sessionsForVehicle(vehicleId: String): List<ToolMeasurementSession> =
        _sessions.value.filter { it.vehicleId == vehicleId }.sortedByDescending { it.createdAt }

    fun getById(id: String): ToolMeasurementSession? =
        _sessions.value.firstOrNull { it.id == id }

    fun save(session: ToolMeasurementSession) {
        val updated = _sessions.value
            .filterNot { it.id == session.id }
            .plus(session)
            .sortedByDescending { it.createdAt }
        persist(updated)
    }

    fun delete(id: String) {
        val updated = _sessions.value.filterNot { it.id == id }
        persist(updated)
    }

    fun replaceAll(sessions: List<ToolMeasurementSession>) {
        persist(sessions.sortedByDescending { it.createdAt })
    }

    private fun persist(sessions: List<ToolMeasurementSession>) {
        _sessions.value = sessions
        preferencesStore.setToolSessionsJson(json.encodeToString(sessions))
    }

    private fun loadSessions(): List<ToolMeasurementSession> {
        val raw = preferencesStore.getToolSessionsJson() ?: return emptyList()
        return runCatching {
            json.decodeFromString<List<ToolMeasurementSession>>(raw)
        }.getOrDefault(emptyList())
    }
}
