package com.simplestsoft.twostrokecalc.di

import com.simplestsoft.twostrokecalc.data.auth.SharedAuthRepository
import com.simplestsoft.twostrokecalc.data.config.CalculatorAvailabilityRepository
import com.simplestsoft.twostrokecalc.data.config.DemoVehiclesConfigRepository
import com.simplestsoft.twostrokecalc.data.config.ProAccessRepository
import com.simplestsoft.twostrokecalc.data.preferences.AppPreferencesStore
import com.simplestsoft.twostrokecalc.data.vehicles.SharedVehicleCatalogRepository
import com.simplestsoft.twostrokecalc.data.vehicles.SharedVehicleRepository
import com.simplestsoft.twostrokecalc.domain.model.CalculatorId
import com.simplestsoft.twostrokecalc.domain.model.FuelLogEntry
import com.simplestsoft.twostrokecalc.domain.model.LanguageMode
import com.simplestsoft.twostrokecalc.domain.model.MaintenanceEntry
import com.simplestsoft.twostrokecalc.domain.model.NavStyle
import com.simplestsoft.twostrokecalc.domain.model.ProModuleId
import com.simplestsoft.twostrokecalc.domain.model.ThemeMode
import com.simplestsoft.twostrokecalc.domain.model.Vehicle
import com.simplestsoft.twostrokecalc.domain.model.VehicleCatalogEntry
import com.simplestsoft.twostrokecalc.domain.model.remote.AccountApi
import com.simplestsoft.twostrokecalc.domain.model.remote.AdminApi
import com.simplestsoft.twostrokecalc.domain.model.remote.AdminPushNotificationRequest
import com.simplestsoft.twostrokecalc.domain.model.remote.AdminSettingsDto
import com.simplestsoft.twostrokecalc.domain.model.remote.AdminUserActiveRequest
import com.simplestsoft.twostrokecalc.domain.model.remote.AdminUserBanRequest
import com.simplestsoft.twostrokecalc.domain.model.remote.AdminUserDto
import com.simplestsoft.twostrokecalc.domain.model.remote.AdminUserProRequest
import com.simplestsoft.twostrokecalc.domain.model.remote.AdminUserRoleRequest
import com.simplestsoft.twostrokecalc.domain.model.remote.VerifyProPurchaseRequest
import com.simplestsoft.twostrokecalc.domain.vehicles.VehicleCatalogMapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin

data class IosSessionSnapshot(
    val isAuthenticated: Boolean,
    val email: String,
    val displayName: String,
    val isAdmin: Boolean,
    val isPro: Boolean,
    val disabledCalculatorNames: List<String>,
    val readOnlyModuleNames: List<String>,
)

data class IosAdminStateSnapshot(
    val loading: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val users: List<AdminUserDto> = emptyList(),
    val settings: AdminSettingsDto? = null,
    val totalUsers: Int = 0,
    val activeSessions: Int = 0,
)

private val iosScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
private var authListenerJob: Job? = null
private var authCallback: ((IosSessionSnapshot) -> Unit)? = null

fun startSharedKoin() {
    if (GlobalContext.getOrNull() == null) {
        startKoin {
            modules(sharedKoinModules())
        }
    }
}

private inline fun <reified T> koinGet(): T = GlobalContext.get().get()

fun sharedAuthRepository(): SharedAuthRepository = koinGet()
fun sharedVehicleRepository(): SharedVehicleRepository = koinGet()
fun sharedVehicleCatalogRepository(): SharedVehicleCatalogRepository = koinGet()
fun appPreferencesStore(): AppPreferencesStore = koinGet()
fun proAccessRepository(): ProAccessRepository = koinGet()
fun calculatorAvailabilityRepository(): CalculatorAvailabilityRepository = koinGet()
fun demoVehiclesConfigRepository(): DemoVehiclesConfigRepository = koinGet()
fun accountApi(): AccountApi = koinGet()
fun adminApi(): AdminApi = koinGet()

fun iosSetAuthListener(callback: ((IosSessionSnapshot) -> Unit)?) {
    authCallback = callback
    authListenerJob?.cancel()
    if (callback == null) return
    authListenerJob = sharedAuthRepository().authState
        .onEach { iosScope.launch { callback(iosRefreshSession()) } }
        .launchIn(iosScope)
}

suspend fun iosRefreshSession(): IosSessionSnapshot {
    val auth = sharedAuthRepository()
    val prefs = appPreferencesStore()
    val proRepo = proAccessRepository()
    val calcRepo = calculatorAvailabilityRepository()

    proRepo.refresh()
    calcRepo.refresh()
    demoVehiclesConfigRepository().refresh()

    val currentUser = auth.authState.first()
    if (currentUser == null) {
        return IosSessionSnapshot(
            isAuthenticated = false,
            email = prefs.getAccountEmail().orEmpty(),
            displayName = prefs.getDisplayName().orEmpty(),
            isAdmin = prefs.getIsAdmin(),
            isPro = prefs.getIsPro(),
            disabledCalculatorNames = disabledNames(calcRepo),
            readOnlyModuleNames = proRepo.state.value.proModules.map { it.name },
        )
    }

    auth.ensureFirestoreUserProfile()
    auth.loadUserRoleFromFirestore()
    proRepo.refresh()

    val proState = proRepo.state.value
    return IosSessionSnapshot(
        isAuthenticated = true,
        email = currentUser.email.orEmpty(),
        displayName = currentUser.displayName ?: prefs.getDisplayName().orEmpty(),
        isAdmin = proState.isAdmin,
        isPro = proState.isPro,
        disabledCalculatorNames = disabledNames(calcRepo),
        readOnlyModuleNames = proState.proModules.map { it.name },
    )
}

private fun disabledNames(calcRepo: CalculatorAvailabilityRepository): List<String> {
    val enabled = calcRepo.enabledCalculators.value
    return CalculatorId.entries.filter { it !in enabled }.map { it.name }
}

suspend fun iosSignIn(email: String, password: String): String? =
    sharedAuthRepository().signInWithEmail(email, password).exceptionOrNull()?.message

suspend fun iosRegister(email: String, password: String, displayName: String?): String? =
    sharedAuthRepository().register(email, password, displayName).exceptionOrNull()?.message

suspend fun iosSendPasswordReset(email: String): String? =
    sharedAuthRepository().sendPasswordReset(email).exceptionOrNull()?.message

suspend fun iosSignInWithGoogle(idToken: String): String? =
    sharedAuthRepository().signInWithGoogleIdToken(idToken).exceptionOrNull()?.message

suspend fun iosSignOut() {
    sharedAuthRepository().signOut()
}

suspend fun iosLoadPreferencesTheme(): String = appPreferencesStore().getThemeMode().name
suspend fun iosLoadPreferencesLanguage(): String = appPreferencesStore().getLanguageMode().name
suspend fun iosLoadPreferencesNavStyle(): String = appPreferencesStore().getNavStyle().name
suspend fun iosLoadWelcomeCompleted(): Boolean = appPreferencesStore().getWelcomeCompleted()
suspend fun iosLoadWalkthroughCompleted(): Boolean = appPreferencesStore().getWalkthroughCompleted()
suspend fun iosLoadNotificationsEnabled(): Boolean = appPreferencesStore().getNotificationsEnabled()
suspend fun iosLoadFirstInstallPermissionsCompleted(): Boolean =
    appPreferencesStore().getFirstInstallPermissionsCompleted()

fun iosSaveTheme(mode: String) {
    runCatching { ThemeMode.valueOf(mode) }.getOrNull()?.let { appPreferencesStore().setThemeMode(it) }
}

fun iosSaveLanguage(mode: String) {
    runCatching { LanguageMode.valueOf(mode) }.getOrNull()?.let { appPreferencesStore().setLanguageMode(it) }
}

fun iosSaveNavStyle(style: String) {
    runCatching { NavStyle.valueOf(style) }.getOrNull()?.let { appPreferencesStore().setNavStyle(it) }
}

fun iosSaveWelcomeCompleted(done: Boolean) = appPreferencesStore().setWelcomeCompleted(done)
fun iosSaveWalkthroughCompleted(done: Boolean) = appPreferencesStore().setWalkthroughCompleted(done)
fun iosSaveNotificationsEnabled(enabled: Boolean) = appPreferencesStore().setNotificationsEnabled(enabled)
fun iosSaveFirstInstallPermissionsCompleted(done: Boolean) =
    appPreferencesStore().setFirstInstallPermissionsCompleted(done)

suspend fun iosRefreshVehicles(): List<Vehicle> {
    val repo = sharedVehicleRepository()
    val canEdit = proAccessRepository().state.value.canEditVehicles()
    repo.reconcileDemoVehicles(
        demoVehiclesEnabled = appPreferencesStore().getDemoVehiclesEnabled(),
        canEditVehicles = canEdit,
    )
    return repo.getVehicles()
}

suspend fun iosSaveVehicle(vehicle: Vehicle) {
    sharedVehicleRepository().saveVehicle(vehicle)
}

suspend fun iosDeleteVehicle(vehicleId: String) {
    sharedVehicleRepository().deleteVehicle(vehicleId)
}

suspend fun iosVerifyProPurchase(purchaseToken: String, productId: String, packageName: String): Boolean {
    val response = accountApi().verifyProPurchase(
        VerifyProPurchaseRequest(
            purchaseToken = purchaseToken,
            productId = productId,
            packageName = packageName,
        ),
    )
    if (response.isPro) {
        proAccessRepository().setPro(true)
    }
    return response.isPro
}

fun iosSeedCatalogJson(raw: String) {
    sharedVehicleCatalogRepository().seedFromBundledJson(raw)
}

suspend fun iosEnsureCatalogSynced() {
    sharedVehicleCatalogRepository().ensureSynced()
}

fun iosCatalogBrands(): List<String> = sharedVehicleCatalogRepository().brands()

fun iosCatalogModels(brand: String): List<String> = sharedVehicleCatalogRepository().modelsForBrand(brand)

fun iosCatalogYears(brand: String, model: String): List<Int> =
    sharedVehicleCatalogRepository().yearsForBrandModel(brand, model)

fun iosCatalogVariants(brand: String, model: String, year: Int): List<VehicleCatalogEntry> =
    sharedVehicleCatalogRepository().variantsFor(brand, model, year)

fun iosCatalogSearch(query: String, limit: Int = 30): List<VehicleCatalogEntry> =
    sharedVehicleCatalogRepository().search(query, limit)

fun iosCatalogEntryCount(): Int = sharedVehicleCatalogRepository().entryCount()

fun iosVehicleFromCatalogEntry(
    entry: VehicleCatalogEntry,
    year: String,
    name: String,
    licensePlate: String,
    odometerKm: String,
): Vehicle = VehicleCatalogMapper.toVehicle(
    entry = entry,
    year = year,
    name = name,
    licensePlate = licensePlate,
    currentOdometerKm = odometerKm,
)

suspend fun iosAdminLoadState(): IosAdminStateSnapshot {
    val api = adminApi()
    return runCatching {
        val users = api.listUsers().users
        val stats = api.statistics()
        val settings = api.getSettings()
        IosAdminStateSnapshot(
            loading = false,
            users = users,
            settings = settings,
            totalUsers = stats.totalUsers,
            activeSessions = stats.activeSessions,
        )
    }.getOrElse { error ->
        IosAdminStateSnapshot(loading = false, error = error.message ?: "Admin-API Fehler")
    }
}

suspend fun iosAdminSaveSettings(settings: AdminSettingsDto): String? {
    return runCatching {
        val success = adminApi().updateSettings(settings)
        if (!success) error("Speichern fehlgeschlagen")
        calculatorAvailabilityRepository().applyAvailabilityMap(settings.calculatorAvailability)
        proAccessRepository().applyProModulesMap(settings.proModules)
        demoVehiclesConfigRepository().applyEnabledFlag(settings.demoVehiclesEnabled)
        appPreferencesStore().setDemoVehiclesEnabled(settings.demoVehiclesEnabled)
        appPreferencesStore().setDisabledCalculators(
            CalculatorId.entries.filter { settings.calculatorAvailability[it.name] == false }.toSet(),
        )
        null
    }.exceptionOrNull()?.message
}

suspend fun iosAdminSetUserBanned(userId: String, banned: Boolean): String? =
    runCatching {
        if (!adminApi().setUserBanned(userId, AdminUserBanRequest(banned))) error("Ban fehlgeschlagen")
    }.exceptionOrNull()?.message

suspend fun iosAdminSetUserActive(userId: String, active: Boolean): String? =
    runCatching {
        if (!adminApi().setUserActive(userId, AdminUserActiveRequest(active))) error("Aktivierung fehlgeschlagen")
    }.exceptionOrNull()?.message

suspend fun iosAdminSetUserRole(userId: String, role: String): String? =
    runCatching {
        if (!adminApi().updateUserRole(userId, AdminUserRoleRequest(role))) error("Rolle fehlgeschlagen")
    }.exceptionOrNull()?.message

suspend fun iosAdminSetUserPro(userId: String, isPro: Boolean): String? =
    runCatching {
        if (!adminApi().setUserPro(userId, AdminUserProRequest(isPro))) error("Pro-Status fehlgeschlagen")
    }.exceptionOrNull()?.message

suspend fun iosAdminSendPush(title: String, body: String): String? =
    runCatching {
        val response = adminApi().sendPushNotification(AdminPushNotificationRequest(title = title, body = body))
        if (response.uniqueTokenCount == 0) "Keine Geräte-Tokens registriert" else null
    }.exceptionOrNull()?.message

fun iosUpdateFuelLog(vehicle: Vehicle, entries: List<FuelLogEntry>): Vehicle =
    vehicle.copy(fuelLog = entries)

fun iosUpdateMaintenanceLog(vehicle: Vehicle, entries: List<MaintenanceEntry>): Vehicle =
    vehicle.copy(maintenanceLog = entries)

fun iosProModuleNames(): List<String> = ProModuleId.entries.map { it.name }
