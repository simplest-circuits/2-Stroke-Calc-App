package com.simplestsoft.twostrokecalc.ui.admin

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.simplestsoft.twostrokecalc.R
import com.simplestsoft.twostrokecalc.data.config.CalculatorAvailabilityRepository
import com.simplestsoft.twostrokecalc.data.config.ProAccessRepository
import com.simplestsoft.twostrokecalc.data.preferences.PreferencesManager
import com.simplestsoft.twostrokecalc.data.remote.AdminApiService
import com.simplestsoft.twostrokecalc.data.vehicles.VehicleRepository
import com.simplestsoft.twostrokecalc.domain.model.CalculatorId
import com.simplestsoft.twostrokecalc.domain.model.ProModuleId
import com.simplestsoft.twostrokecalc.domain.model.remote.AdminPushNotificationRequest
import com.simplestsoft.twostrokecalc.domain.model.remote.AdminSettingsDto
import com.simplestsoft.twostrokecalc.domain.model.remote.AdminUserActiveRequest
import com.simplestsoft.twostrokecalc.domain.model.remote.AdminUserBanRequest
import com.simplestsoft.twostrokecalc.domain.model.remote.AdminUserDto
import com.simplestsoft.twostrokecalc.domain.model.remote.AdminUserProRequest
import com.simplestsoft.twostrokecalc.domain.model.remote.AdminUserRoleRequest
import com.simplestsoft.twostrokecalc.domain.model.remote.AdminStatisticsResponse
import com.simplestsoft.twostrokecalc.ui.util.NetworkExceptionMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

data class AdminUiState(
    val loading: Boolean = false,
    val users: List<AdminUserDto> = emptyList(),
    val statistics: AdminStatisticsResponse? = null,
    val settings: AdminSettingsDto? = null,
    val error: String? = null,
    val message: String? = null,
)

@HiltViewModel
class AdminViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val adminApiService: AdminApiService,
    private val preferencesManager: PreferencesManager,
    private val calculatorAvailabilityRepository: CalculatorAvailabilityRepository,
    private val proAccessRepository: ProAccessRepository,
    private val vehicleRepository: VehicleRepository,
    private val firebaseAuth: FirebaseAuth,
) : ViewModel() {

    private val _state = MutableStateFlow(AdminUiState())
    val state = _state.asStateFlow()

    fun loadUsers() {
        viewModelScope.launch {
            blockedAdminApiMessage()?.let { msg ->
                _state.update { it.copy(loading = false, error = msg) }
                return@launch
            }
            _state.update { it.copy(loading = true, error = null, message = null) }
            runCatching { adminApiService.listUsers() }
                .onSuccess { res -> _state.update { it.copy(loading = false, users = res.users) } }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            loading = false,
                            error = e.toAdminErrorMessage("Benutzer konnten nicht geladen werden"),
                        )
                    }
                }
        }
    }

    fun loadStatistics() {
        viewModelScope.launch {
            blockedAdminApiMessage()?.let { msg ->
                _state.update { it.copy(loading = false, error = msg) }
                return@launch
            }
            _state.update { it.copy(loading = true, error = null, message = null) }
            runCatching { adminApiService.statistics() }
                .onSuccess { res -> _state.update { it.copy(loading = false, statistics = res) } }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            loading = false,
                            error = e.toAdminErrorMessage("Statistiken konnten nicht geladen werden"),
                        )
                    }
                }
        }
    }

    fun loadSettings() {
        viewModelScope.launch {
            blockedAdminApiMessage()?.let { msg ->
                _state.update { it.copy(loading = false, error = msg) }
                return@launch
            }
            _state.update { it.copy(loading = true, error = null, message = null) }
            runCatching { adminApiService.getSettings() }
                .onSuccess { res -> _state.update { it.copy(loading = false, settings = res) } }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            loading = false,
                            error = e.toAdminErrorMessage("Einstellungen konnten nicht geladen werden"),
                        )
                    }
                }
        }
    }

    fun saveSettings(settings: AdminSettingsDto) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, message = null) }
            runCatching { adminApiService.updateSettings(settings) }
                .onSuccess {
                    calculatorAvailabilityRepository.applyAvailabilityMap(settings.calculatorAvailability)
                    proAccessRepository.applyProModulesMap(settings.proModules)
                    viewModelScope.launch {
                        preferencesManager.setDisabledCalculators(
                            CalculatorId.entries
                                .filter { settings.calculatorAvailability[it.name] == false }
                                .toSet(),
                        )
                        preferencesManager.setProModules(
                            settings.proModules.entries
                                .mapNotNull { (key, enabled) ->
                                    if (enabled) ProModuleId.fromName(key) else null
                                }
                                .toSet(),
                        )
                    }
                    _state.update {
                        it.copy(loading = false, settings = settings, message = "Einstellungen gespeichert")
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(loading = false, error = e.toAdminErrorMessage("Einstellungen konnten nicht gespeichert werden"))
                    }
                }
        }
    }

    fun sendPushNotification(title: String, body: String) {
        viewModelScope.launch {
            blockedAdminApiMessage()?.let { msg ->
                _state.update { it.copy(error = msg) }
                return@launch
            }
            val trimmedTitle = title.trim()
            val trimmedBody = body.trim()
            if (trimmedTitle.isEmpty() || trimmedBody.isEmpty()) {
                _state.update { it.copy(error = appContext.getString(R.string.admin_push_missing_fields)) }
                return@launch
            }
            _state.update { it.copy(loading = true, error = null, message = null) }
            runCatching {
                adminApiService.sendPushNotification(
                    AdminPushNotificationRequest(title = trimmedTitle, body = trimmedBody),
                )
            }.onSuccess { response ->
                val message = if (response.uniqueTokenCount == 0) {
                    appContext.getString(R.string.admin_push_no_tokens)
                } else {
                    appContext.getString(
                        R.string.admin_push_success,
                        response.sentCount,
                        response.uniqueTokenCount,
                    )
                }
                _state.update { it.copy(loading = false, message = message) }
            }.onFailure { e ->
                _state.update {
                    it.copy(
                        loading = false,
                        error = e.toAdminErrorMessage("Push-Benachrichtigung konnte nicht gesendet werden"),
                    )
                }
            }
        }
    }

    fun triggerWalkthroughForCurrentDevice() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, message = null) }
            runCatching { preferencesManager.setWelcomeCompleted(false) }
                .onSuccess {
                    _state.update {
                        it.copy(
                            loading = false,
                            message = "Walkthrough wurde für den nächsten App-Start aktiviert.",
                        )
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(loading = false, error = e.message ?: "Walkthrough konnte nicht aktiviert werden.")
                    }
                }
        }
    }

    fun seedDemoVehicles() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, message = null) }
            runCatching { vehicleRepository.seedAdminDemoVehicles() }
                .onSuccess { result ->
                    val message = when {
                        result.created == 2 -> appContext.getString(R.string.admin_demo_vehicles_created)
                        result.updated > 0 && result.created == 0 ->
                            appContext.getString(R.string.admin_demo_vehicles_updated)
                        else -> appContext.getString(
                            R.string.admin_demo_vehicles_mixed,
                            result.created,
                            result.updated,
                        )
                    }
                    _state.update { it.copy(loading = false, message = message) }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            loading = false,
                            error = e.message ?: appContext.getString(R.string.admin_demo_vehicles_error),
                        )
                    }
                }
        }
    }

    fun resetPassword(userId: String) = runUserAction("Passwort-Reset wurde erzeugt") {
        val response = adminApiService.resetUserPassword(userId)
        response.message ?: response.email?.let { "Passwort-Reset für $it erzeugt" }
    }

    fun setUserBanned(userId: String, banned: Boolean) = runUserAction(
        if (banned) "Benutzer gesperrt" else "Benutzer entsperrt",
    ) {
        adminApiService.setUserBanned(userId, AdminUserBanRequest(banned))
        null
    }

    fun setUserActive(userId: String, active: Boolean) = runUserAction(
        if (active) "Benutzer aktiviert" else "Benutzer deaktiviert",
    ) {
        adminApiService.setUserActive(userId, AdminUserActiveRequest(active))
        null
    }

    fun deleteUser(userId: String) = runUserAction("Benutzer gelöscht") {
        adminApiService.deleteUser(userId)
        null
    }

    fun updateRole(userId: String, role: String) = runUserAction("Rolle aktualisiert") {
        adminApiService.updateUserRole(userId, AdminUserRoleRequest(role))
        null
    }

    fun setUserPro(userId: String, isPro: Boolean) = runUserAction(
        if (isPro) "Pro-Version aktiviert" else "Pro-Version deaktiviert",
    ) {
        adminApiService.setUserPro(userId, AdminUserProRequest(isPro))
        if (userId == firebaseAuth.currentUser?.uid) {
            preferencesManager.setIsPro(isPro)
        }
        null
    }

    private fun runUserAction(
        successMessage: String,
        action: suspend () -> String?,
    ) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, message = null) }
            runCatching { action() }
                .onSuccess { message ->
                    runCatching { adminApiService.listUsers() }
                        .onSuccess { res ->
                            _state.update {
                                it.copy(
                                    loading = false,
                                    users = res.users,
                                    message = message ?: successMessage,
                                )
                            }
                        }
                        .onFailure { e ->
                            _state.update {
                                it.copy(
                                    loading = false,
                                    error = e.toAdminErrorMessage("Benutzer konnten nicht aktualisiert werden"),
                                )
                            }
                        }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            loading = false,
                            error = e.toAdminErrorMessage("Aktion konnte nicht ausgeführt werden"),
                        )
                    }
                }
        }
    }

    private fun blockedAdminApiMessage(): String? {
        if (firebaseAuth.currentUser != null) return null
        return appContext.getString(R.string.admin_requires_sign_in)
    }

    private fun Throwable.toAdminErrorMessage(defaultMessage: String): String {
        if ((this as? HttpException)?.code() == 401) {
            return appContext.getString(R.string.admin_session_unauthorized)
        }
        NetworkExceptionMessage.messageForThrowable(appContext, this)?.let { return it }
        return message?.takeIf { it.isNotBlank() } ?: defaultMessage
    }
}
