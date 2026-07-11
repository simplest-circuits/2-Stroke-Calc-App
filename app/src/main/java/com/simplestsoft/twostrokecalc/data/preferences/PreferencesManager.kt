package com.simplestsoft.twostrokecalc.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.simplestsoft.twostrokecalc.domain.model.CalculatorId
import com.simplestsoft.twostrokecalc.domain.model.LanguageMode
import com.simplestsoft.twostrokecalc.domain.model.NavStyle
import com.simplestsoft.twostrokecalc.domain.model.ProModuleId
import com.simplestsoft.twostrokecalc.domain.model.ThemeMode
import com.simplestsoft.twostrokecalc.domain.model.UserPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_prefs")

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val dataStore get() = context.dataStore

    private object Keys {
        val THEME = stringPreferencesKey("theme_mode")
        val LANGUAGE_MODE = stringPreferencesKey("language_mode")
        val NAV_STYLE = stringPreferencesKey("nav_style")
        val WELCOME_DONE = booleanPreferencesKey("welcome_completed")
        val NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
        val FIRST_INSTALL_PERMISSIONS_COMPLETED = booleanPreferencesKey("first_install_permissions_completed")
        val USER_ID = stringPreferencesKey("user_id")
        val IS_ADMIN = booleanPreferencesKey("is_admin")
        val IS_PRO = booleanPreferencesKey("is_pro")
        val PRO_MODULES = stringPreferencesKey("pro_modules")
        val DEMO_VEHICLES_ENABLED = booleanPreferencesKey("demo_vehicles_enabled")
        val DISPLAY_NAME = stringPreferencesKey("display_name")
        val ACCOUNT_EMAIL = stringPreferencesKey("account_email")
        val SETTINGS_MAIL_LAST_SUBMIT_MS = longPreferencesKey("settings_mail_last_submit_ms")
        val DISABLED_CALCULATORS = stringPreferencesKey("disabled_calculators")
        val VEHICLE_CATALOG_VERSION = intPreferencesKey("vehicle_catalog_version")
    }

    companion object {
        const val SETTINGS_MAIL_COOLDOWN_MS: Long = 3 * 60 * 1000L
    }

    val preferencesFlow: Flow<UserPreferences> = dataStore.data.map { prefs ->
        val languageMode = parseLanguageMode(prefs)
        UserPreferences(
            themeMode = prefs[Keys.THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            languageMode = languageMode,
            language = languageMode.resolveLanguage(),
            navStyle = prefs[Keys.NAV_STYLE]?.let { runCatching { NavStyle.valueOf(it) }.getOrNull() }
                ?: NavStyle.BOTTOM_BAR,
            welcomeCompleted = prefs[Keys.WELCOME_DONE] ?: false,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS] ?: false,
            firstInstallPermissionsCompleted = prefs[Keys.FIRST_INSTALL_PERMISSIONS_COMPLETED] ?: false,
            userId = prefs[Keys.USER_ID],
            isAdmin = prefs[Keys.IS_ADMIN] ?: false,
            isPro = prefs[Keys.IS_PRO] ?: false,
            displayName = prefs[Keys.DISPLAY_NAME],
            accountEmail = prefs[Keys.ACCOUNT_EMAIL],
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[Keys.THEME] = mode.name }
    }

    suspend fun setLanguageMode(mode: LanguageMode) {
        dataStore.edit { it[Keys.LANGUAGE_MODE] = mode.name }
    }

    suspend fun setNavStyle(style: NavStyle) {
        dataStore.edit { it[Keys.NAV_STYLE] = style.name }
    }

    suspend fun setWelcomeCompleted(done: Boolean) {
        dataStore.edit { it[Keys.WELCOME_DONE] = done }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.NOTIFICATIONS] = enabled }
    }

    suspend fun setFirstInstallPermissionsCompleted(completed: Boolean) {
        dataStore.edit { it[Keys.FIRST_INSTALL_PERMISSIONS_COMPLETED] = completed }
    }

    suspend fun readAccountEmail(): String? = dataStore.data.first()[Keys.ACCOUNT_EMAIL]

    suspend fun setUserId(userId: String?) {
        dataStore.edit {
            if (userId == null) it.remove(Keys.USER_ID) else it[Keys.USER_ID] = userId
        }
    }

    suspend fun setIsAdmin(isAdmin: Boolean) {
        dataStore.edit { it[Keys.IS_ADMIN] = isAdmin }
    }

    suspend fun getIsAdmin(): Boolean = dataStore.data.first()[Keys.IS_ADMIN] ?: false

    suspend fun setIsPro(isPro: Boolean) {
        dataStore.edit { it[Keys.IS_PRO] = isPro }
    }

    suspend fun getIsPro(): Boolean = dataStore.data.first()[Keys.IS_PRO] ?: false

    suspend fun setProModules(modules: Set<ProModuleId>) {
        val value = modules.joinToString(",") { it.name }
        dataStore.edit {
            if (value.isEmpty()) {
                it.remove(Keys.PRO_MODULES)
            } else {
                it[Keys.PRO_MODULES] = value
            }
        }
    }

    suspend fun getProModules(): Set<ProModuleId> {
        val raw = dataStore.data.first()[Keys.PRO_MODULES] ?: return emptySet()
        if (raw.isBlank()) return emptySet()
        return raw.split(",")
            .mapNotNull { token ->
                val name = token.trim()
                if (name.isEmpty()) null else ProModuleId.fromName(name)
            }
            .toSet()
    }

    suspend fun setDemoVehiclesEnabled(enabled: Boolean) {
        dataStore.edit { it[Keys.DEMO_VEHICLES_ENABLED] = enabled }
    }

    suspend fun getDemoVehiclesEnabled(): Boolean =
        dataStore.data.first()[Keys.DEMO_VEHICLES_ENABLED] ?: true

    suspend fun setDisplayName(name: String?) {
        dataStore.edit {
            if (name.isNullOrBlank()) it.remove(Keys.DISPLAY_NAME) else it[Keys.DISPLAY_NAME] = name
        }
    }

    suspend fun setAccountEmail(email: String?) {
        dataStore.edit {
            if (email.isNullOrBlank()) it.remove(Keys.ACCOUNT_EMAIL) else it[Keys.ACCOUNT_EMAIL] = email
        }
    }

    suspend fun clearUserScoped() {
        dataStore.edit {
            it.remove(Keys.USER_ID)
            it.remove(Keys.IS_ADMIN)
            it.remove(Keys.IS_PRO)
            it.remove(Keys.DISPLAY_NAME)
            it.remove(Keys.ACCOUNT_EMAIL)
        }
    }

    suspend fun getSettingsMailCooldownRemainingMs(): Long {
        val lastSubmit = dataStore.data.first()[Keys.SETTINGS_MAIL_LAST_SUBMIT_MS] ?: 0L
        val elapsed = System.currentTimeMillis() - lastSubmit
        return (SETTINGS_MAIL_COOLDOWN_MS - elapsed).coerceAtLeast(0L)
    }

    suspend fun recordSettingsMailSubmit() {
        dataStore.edit { it[Keys.SETTINGS_MAIL_LAST_SUBMIT_MS] = System.currentTimeMillis() }
    }

    suspend fun getDisabledCalculators(): Set<CalculatorId> {
        val raw = dataStore.data.first()[Keys.DISABLED_CALCULATORS] ?: return emptySet()
        if (raw.isBlank()) return emptySet()
        return raw.split(",")
            .mapNotNull { token ->
                val name = token.trim()
                if (name.isEmpty()) null else CalculatorId.fromName(name)
            }
            .toSet()
    }

    suspend fun setDisabledCalculators(ids: Set<CalculatorId>) {
        val value = ids.joinToString(",") { it.name }
        dataStore.edit {
            if (value.isEmpty()) {
                it.remove(Keys.DISABLED_CALCULATORS)
            } else {
                it[Keys.DISABLED_CALCULATORS] = value
            }
        }
    }

    suspend fun getVehicleCatalogVersion(): Int =
        dataStore.data.first()[Keys.VEHICLE_CATALOG_VERSION] ?: 0

    suspend fun setVehicleCatalogVersion(version: Int) {
        dataStore.edit { it[Keys.VEHICLE_CATALOG_VERSION] = version }
    }

    private fun parseLanguageMode(prefs: Preferences): LanguageMode =
        prefs[Keys.LANGUAGE_MODE]?.let { runCatching { LanguageMode.valueOf(it) }.getOrNull() }
            ?: LanguageMode.SYSTEM
}
