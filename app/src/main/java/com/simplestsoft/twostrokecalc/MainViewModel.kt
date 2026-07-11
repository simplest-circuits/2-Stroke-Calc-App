package com.simplestsoft.twostrokecalc

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.simplestsoft.twostrokecalc.data.auth.AuthRepository
import com.simplestsoft.twostrokecalc.data.billing.BillingRepository
import com.simplestsoft.twostrokecalc.data.config.CalculatorAvailabilityRepository
import com.simplestsoft.twostrokecalc.data.config.DemoVehiclesConfigRepository
import com.simplestsoft.twostrokecalc.data.config.ProAccessRepository
import com.simplestsoft.twostrokecalc.data.preferences.PreferencesManager
import com.simplestsoft.twostrokecalc.data.sync.AccountSyncService
import com.simplestsoft.twostrokecalc.data.sync.UserSettingsFirestoreSync
import com.simplestsoft.twostrokecalc.data.vehicles.VehicleCatalogRepository
import com.simplestsoft.twostrokecalc.data.vehicles.VehicleRepository
import com.simplestsoft.twostrokecalc.domain.model.LanguageMode
import com.simplestsoft.twostrokecalc.domain.model.NavStyle
import com.simplestsoft.twostrokecalc.domain.model.ThemeMode
import com.simplestsoft.twostrokecalc.domain.model.UserPreferences
import com.simplestsoft.twostrokecalc.logging.ErrorLogger
import com.simplestsoft.twostrokecalc.notifications.MaintenanceNotificationHelper
import com.simplestsoft.twostrokecalc.ui.permissions.FirstInstallPermissionsPolicy
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await

data class MainUiState(
    val isAuthenticated: Boolean = false,
    val preferences: UserPreferences = UserPreferences(),
    val isAdmin: Boolean = false,
    val showFirstInstallPermissionsScreen: Boolean = false,
    val showWelcome: Boolean = false,
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val preferencesManager: PreferencesManager,
    private val vehicleRepository: VehicleRepository,
    private val vehicleCatalogRepository: VehicleCatalogRepository,
    private val accountSyncService: AccountSyncService,
    private val calculatorAvailabilityRepository: CalculatorAvailabilityRepository,
    private val demoVehiclesConfigRepository: DemoVehiclesConfigRepository,
    private val proAccessRepository: ProAccessRepository,
    private val billingRepository: BillingRepository,
    private val userSettingsFirestoreSync: UserSettingsFirestoreSync,
    private val firebaseAuth: FirebaseAuth,
    private val errorLogger: ErrorLogger,
    private val notificationHelper: MaintenanceNotificationHelper,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState = _uiState.asStateFlow()
    val proAccessState = proAccessRepository.state

    private var syncCompletedForUid: String? = null
    private var deviceInfoSyncedForUid: String? = null
    private val loginSyncMutex = Mutex()
    private var firstInstallPermissionsEvaluated = false

    init {
        viewModelScope.launch {
            runCatching { calculatorAvailabilityRepository.refresh() }
                .onFailure { errorLogger.log("MainViewModel", "refreshCalculatorAvailability", it) }
            runCatching { proAccessRepository.refresh() }
                .onFailure { errorLogger.log("MainViewModel", "refreshProAccess", it) }
            runCatching { demoVehiclesConfigRepository.refresh() }
                .onFailure { errorLogger.log("MainViewModel", "refreshDemoVehiclesConfig", it) }
            billingRepository.start()
            runCatching { vehicleCatalogRepository.ensureSynced() }
                .onFailure { errorLogger.log("MainViewModel", "syncVehicleCatalog", it) }
        }
        viewModelScope.launch {
            combine(
                proAccessRepository.state,
                demoVehiclesConfigRepository.enabled.filterNotNull(),
            ) { proAccess, demoEnabled ->
                runCatching {
                    vehicleRepository.reconcileDemoVehicles(
                        demoVehiclesEnabled = demoEnabled,
                        canEditVehicles = proAccess.canEditVehicles(),
                    )
                }.onFailure { errorLogger.log("MainViewModel", "reconcileDemoVehicles", it) }
            }.collect { }
        }
        viewModelScope.launch {
            preferencesManager.preferencesFlow.onEach { prefs ->
                proAccessRepository.applyUserEntitlements(prefs.isPro, prefs.isAdmin)
            }.launchIn(this)
            combine(
                authRepository.authState,
                preferencesManager.preferencesFlow,
            ) { user, prefs ->
                MainUiState(
                    isAuthenticated = user != null,
                    preferences = prefs,
                    isAdmin = prefs.isAdmin,
                    showWelcome = !prefs.welcomeCompleted,
                )
            }.collect { state ->
                if (!state.isAuthenticated) {
                    syncCompletedForUid = null
                    deviceInfoSyncedForUid = null
                    vehicleRepository.resetSyncState()
                }
                _uiState.update { current ->
                    state.copy(
                        showFirstInstallPermissionsScreen = current.showFirstInstallPermissionsScreen,
                    )
                }
                if (!state.preferences.firstInstallPermissionsCompleted && !firstInstallPermissionsEvaluated) {
                    evaluateFirstInstallPermissions(state.preferences)
                }
                if (state.isAuthenticated) {
                    syncDeviceInfoOnAppStart()
                    postLoginSync()
                }
            }
        }
    }

    private fun syncDeviceInfoOnAppStart() {
        val uid = firebaseAuth.currentUser?.uid ?: return
        if (deviceInfoSyncedForUid == uid) return
        deviceInfoSyncedForUid = uid
        viewModelScope.launch {
            runCatching { accountSyncService.pushDeviceInfo(uid) }
                .onFailure { errorLogger.log("MainViewModel", "syncDeviceInfoOnAppStart", it) }
        }
    }

    private fun postLoginSync() {
        val uid = firebaseAuth.currentUser?.uid ?: return
        viewModelScope.launch {
            loginSyncMutex.withLock {
                if (syncCompletedForUid == uid) return@withLock
                runCatching { authRepository.ensureFirestoreUserProfile() }
                    .onFailure { errorLogger.log("MainViewModel", "ensureFirestoreUserProfile", it) }
                runCatching { authRepository.loadUserRoleFromFirestore() }
                    .onFailure { errorLogger.log("MainViewModel", "loadUserRoleFromFirestore", it) }
                runCatching { userSettingsFirestoreSync.syncOnLogin(uid) }
                    .onFailure { errorLogger.log("MainViewModel", "syncUserSettingsOnLogin", it) }
                runCatching { proAccessRepository.refresh() }
                    .onFailure { errorLogger.log("MainViewModel", "refreshProAccessAfterLogin", it) }
                billingRepository.restorePurchases()
                runCatching { vehicleRepository.syncOnLogin() }
                    .onFailure { errorLogger.log("MainViewModel", "vehicleSyncOnLogin", it) }
                if (firebaseAuth.currentUser?.uid == uid) {
                    syncCompletedForUid = uid
                }
            }
        }
    }

    fun completeWelcome() {
        viewModelScope.launch {
            preferencesManager.setWelcomeCompleted(true)
            userSettingsFirestoreSync.persistWelcomeCompleted(true)
        }
    }

    fun completeFirstInstallPermissions() {
        viewModelScope.launch {
            preferencesManager.setFirstInstallPermissionsCompleted(true)
            _uiState.update {
                it.copy(showFirstInstallPermissionsScreen = false)
            }
        }
    }

    private suspend fun evaluateFirstInstallPermissions(prefs: UserPreferences) {
        firstInstallPermissionsEvaluated = true
        val canPostNotifications = notificationHelper.canPost()
        val flowNeeded = FirstInstallPermissionsPolicy.isFlowNeeded(canPostNotifications)
        if (!flowNeeded) {
            if (canPostNotifications && !prefs.notificationsEnabled) {
                preferencesManager.setNotificationsEnabled(true)
                userSettingsFirestoreSync.persistNotificationsEnabled(true)
            }
            preferencesManager.setFirstInstallPermissionsCompleted(true)
            _uiState.update {
                it.copy(showFirstInstallPermissionsScreen = false)
            }
            return
        }
        _uiState.update {
            it.copy(showFirstInstallPermissionsScreen = true)
        }
    }

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch {
            preferencesManager.setThemeMode(mode)
            userSettingsFirestoreSync.persistTheme(mode)
        }
    }

    fun refreshSystemLanguage() {
        _uiState.update { state ->
            if (state.preferences.languageMode != LanguageMode.SYSTEM) return@update state
            val language = LanguageMode.SYSTEM.resolveLanguage()
            if (state.preferences.language == language) state
            else state.copy(preferences = state.preferences.copy(language = language))
        }
    }

    fun setLanguageMode(mode: LanguageMode) {
        viewModelScope.launch {
            preferencesManager.setLanguageMode(mode)
            _uiState.update { state ->
                state.copy(
                    preferences = state.preferences.copy(
                        languageMode = mode,
                        language = mode.resolveLanguage(),
                    ),
                )
            }
            userSettingsFirestoreSync.persistLanguageMode(mode)
        }
    }

    fun setNavStyle(style: NavStyle) {
        viewModelScope.launch {
            preferencesManager.setNavStyle(style)
            userSettingsFirestoreSync.persistNavStyle(style)
        }
    }

    fun updateDisplayName(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            preferencesManager.setDisplayName(trimmed)
            runCatching {
                firebaseAuth.currentUser?.updateProfile(
                    UserProfileChangeRequest.Builder()
                        .setDisplayName(trimmed)
                        .build(),
                )?.await()
            }.onFailure { errorLogger.log("MainViewModel", "updateDisplayName.firebaseProfile", it) }
            runCatching { userSettingsFirestoreSync.persistDisplayName(trimmed) }
                .onFailure { errorLogger.log("MainViewModel", "updateDisplayName.firestore", it) }
        }
    }

    suspend fun getSettingsMailCooldownRemainingMs(): Long =
        preferencesManager.getSettingsMailCooldownRemainingMs()

    suspend fun recordSettingsMailSubmit() =
        preferencesManager.recordSettingsMailSubmit()
}
