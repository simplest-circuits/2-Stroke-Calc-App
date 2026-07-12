package com.simplestsoft.twostrokecalc.data.vehicles

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.maintenanceReminderDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "maintenance_reminder_store",
)

private data class ReminderPersistedState(
    val confirmed: Boolean = false,
    val sentMilestones: Set<Int> = emptySet(),
    val kmAnchorDate: String? = null,
)

@Singleton
class MaintenanceReminderStateStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson,
) {
    private val dataStore get() = context.maintenanceReminderDataStore

    private object Keys {
        val STATES = stringPreferencesKey("reminder_states_json")
    }

    suspend fun isConfirmed(reminderKey: String): Boolean =
        loadStates()[reminderKey]?.confirmed == true

    suspend fun confirm(reminderKey: String) {
        updateState(reminderKey) { it.copy(confirmed = true) }
    }

    suspend fun hasSentMilestone(reminderKey: String, offset: Int): Boolean =
        loadStates()[reminderKey]?.sentMilestones?.contains(offset) == true

    suspend fun markMilestoneSent(reminderKey: String, offset: Int) {
        updateState(reminderKey) { state ->
            state.copy(sentMilestones = state.sentMilestones + offset)
        }
    }

    suspend fun getKmAnchor(reminderKey: String): LocalDate? {
        val raw = loadStates()[reminderKey]?.kmAnchorDate ?: return null
        return runCatching { LocalDate.parse(raw) }.getOrNull()
    }

    suspend fun setKmAnchor(reminderKey: String, date: LocalDate) {
        updateState(reminderKey) { it.copy(kmAnchorDate = date.toString()) }
    }

    suspend fun syncActiveKeys(activeKeys: Set<String>) {
        dataStore.edit { prefs ->
            val current = decodeStates(prefs[Keys.STATES])
            val pruned = current.filterKeys { it in activeKeys }
            if (pruned.size != current.size) {
                prefs[Keys.STATES] = encodeStates(pruned)
            }
        }
    }

    private suspend fun loadStates(): Map<String, ReminderPersistedState> =
        dataStore.data.map { decodeStates(it[Keys.STATES]) }.first()

    private suspend fun updateState(
        reminderKey: String,
        transform: (ReminderPersistedState) -> ReminderPersistedState,
    ) {
        dataStore.edit { prefs ->
            val current = decodeStates(prefs[Keys.STATES]).toMutableMap()
            val existing = current[reminderKey] ?: ReminderPersistedState()
            current[reminderKey] = transform(existing)
            prefs[Keys.STATES] = encodeStates(current)
        }
    }

    private fun decodeStates(json: String?): Map<String, ReminderPersistedState> {
        if (json.isNullOrBlank()) return emptyMap()
        return runCatching {
            val type = object : TypeToken<Map<String, ReminderPersistedState>>() {}.type
            gson.fromJson<Map<String, ReminderPersistedState>>(json, type)
        }.getOrDefault(emptyMap())
    }

    private fun encodeStates(states: Map<String, ReminderPersistedState>): String =
        gson.toJson(states)
}
