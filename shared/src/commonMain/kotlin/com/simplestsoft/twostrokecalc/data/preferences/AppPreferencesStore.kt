package com.simplestsoft.twostrokecalc.data.preferences

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import com.simplestsoft.twostrokecalc.domain.model.CalculatorId
import com.simplestsoft.twostrokecalc.domain.model.LanguageMode
import com.simplestsoft.twostrokecalc.domain.model.NavStyle
import com.simplestsoft.twostrokecalc.domain.model.ThemeMode
import com.simplestsoft.twostrokecalc.domain.model.UserPreferences
import com.simplestsoft.twostrokecalc.platform.createSettings

class AppPreferencesStore(
    private val settings: Settings = createSettings("app_prefs"),
) {
    fun getUserId(): String? = settings.getStringOrNull(KEY_USER_ID)

    fun setUserId(userId: String?) {
        if (userId == null) settings.remove(KEY_USER_ID) else settings[KEY_USER_ID] = userId
    }

    fun getIsAdmin(): Boolean = settings.getBoolean(KEY_IS_ADMIN, false)

    fun setIsAdmin(isAdmin: Boolean) {
        settings[KEY_IS_ADMIN] = isAdmin
    }

    fun getIsPro(): Boolean = settings.getBoolean(KEY_IS_PRO, false)

    fun setIsPro(isPro: Boolean) {
        settings[KEY_IS_PRO] = isPro
    }

    fun getProModulesRaw(): String? = settings.getStringOrNull(KEY_PRO_MODULES)

    fun setProModulesRaw(value: String?) {
        if (value.isNullOrEmpty()) settings.remove(KEY_PRO_MODULES) else settings[KEY_PRO_MODULES] = value
    }

    fun getDemoVehiclesEnabled(): Boolean = settings.getBoolean(KEY_DEMO_VEHICLES_ENABLED, true)

    fun setDemoVehiclesEnabled(enabled: Boolean) {
        settings[KEY_DEMO_VEHICLES_ENABLED] = enabled
    }

    fun getDisplayName(): String? = settings.getStringOrNull(KEY_DISPLAY_NAME)

    fun setDisplayName(name: String?) {
        if (name.isNullOrBlank()) settings.remove(KEY_DISPLAY_NAME) else settings[KEY_DISPLAY_NAME] = name
        markSettingsLocallyModified()
    }

    fun getAccountEmail(): String? = settings.getStringOrNull(KEY_ACCOUNT_EMAIL)

    fun setAccountEmail(email: String?) {
        if (email.isNullOrBlank()) settings.remove(KEY_ACCOUNT_EMAIL) else settings[KEY_ACCOUNT_EMAIL] = email
    }

    fun getString(key: String): String? = settings.getStringOrNull(key)

    fun putString(key: String, value: String?) {
        if (value.isNullOrEmpty()) settings.remove(key) else settings[key] = value
    }

    fun getThemeMode(): ThemeMode = settings.getStringOrNull(KEY_THEME)?.let {
        runCatching { ThemeMode.valueOf(it) }.getOrNull()
    } ?: ThemeMode.SYSTEM

    fun setThemeMode(mode: ThemeMode) {
        settings[KEY_THEME] = mode.name
        markSettingsLocallyModified()
    }

    fun getLanguageMode(): LanguageMode = settings.getStringOrNull(KEY_LANGUAGE)?.let {
        runCatching { LanguageMode.valueOf(it) }.getOrNull()
    } ?: LanguageMode.SYSTEM

    fun setLanguageMode(mode: LanguageMode) {
        settings[KEY_LANGUAGE] = mode.name
        markSettingsLocallyModified()
    }

    fun getNavStyle(): NavStyle = settings.getStringOrNull(KEY_NAV_STYLE)?.let {
        runCatching { NavStyle.valueOf(it) }.getOrNull()
    } ?: NavStyle.BOTTOM_BAR

    fun setNavStyle(style: NavStyle) {
        settings[KEY_NAV_STYLE] = style.name
        markSettingsLocallyModified()
    }

    fun getWelcomeCompleted(): Boolean = settings.getBoolean(KEY_WELCOME_DONE, false)

    fun setWelcomeCompleted(done: Boolean) {
        settings[KEY_WELCOME_DONE] = done
        markSettingsLocallyModified()
    }

    fun getWalkthroughCompleted(): Boolean = settings.getBoolean(KEY_WALKTHROUGH_DONE, false)

    fun setWalkthroughCompleted(done: Boolean) {
        settings[KEY_WALKTHROUGH_DONE] = done
    }

    fun getNotificationsEnabled(): Boolean = settings.getBoolean(KEY_NOTIFICATIONS, false)

    fun setNotificationsEnabled(enabled: Boolean) {
        settings[KEY_NOTIFICATIONS] = enabled
        markSettingsLocallyModified()
    }

    fun getFirstInstallPermissionsCompleted(): Boolean =
        settings.getBoolean(KEY_FIRST_INSTALL_PERMISSIONS, false)

    fun setFirstInstallPermissionsCompleted(completed: Boolean) {
        settings[KEY_FIRST_INSTALL_PERMISSIONS] = completed
    }

    fun readUserPreferences(): UserPreferences {
        val languageMode = getLanguageMode()
        return UserPreferences(
            themeMode = getThemeMode(),
            languageMode = languageMode,
            language = languageMode.resolveLanguage(),
            navStyle = getNavStyle(),
        welcomeCompleted = getWelcomeCompleted(),
        walkthroughCompleted = getWalkthroughCompleted(),
        notificationsEnabled = getNotificationsEnabled(),
        firstInstallPermissionsCompleted = getFirstInstallPermissionsCompleted(),
        userId = getUserId(),
        isAdmin = getIsAdmin(),
        isPro = getIsPro(),
        displayName = getDisplayName(),
        accountEmail = getAccountEmail(),
        )
    }

    fun clearUserScoped() {
        settings.remove(KEY_USER_ID)
        settings.remove(KEY_IS_ADMIN)
        settings.remove(KEY_IS_PRO)
        settings.remove(KEY_DISPLAY_NAME)
        settings.remove(KEY_ACCOUNT_EMAIL)
    }

    fun getDisabledCalculators(): Set<CalculatorId> {
        val raw = settings.getStringOrNull(KEY_DISABLED_CALCULATORS) ?: return emptySet()
        if (raw.isBlank()) return emptySet()
        return raw.split(",")
            .mapNotNull { token ->
                val name = token.trim()
                if (name.isEmpty()) null else CalculatorId.fromName(name)
            }
            .toSet()
    }

    fun setDisabledCalculators(ids: Set<CalculatorId>) {
        val value = ids.joinToString(",") { it.name }
        if (value.isEmpty()) settings.remove(KEY_DISABLED_CALCULATORS) else settings[KEY_DISABLED_CALCULATORS] = value
    }

    fun getDisabledTools(): Set<com.simplestsoft.twostrokecalc.domain.model.ToolId> {
        val raw = settings.getStringOrNull(KEY_DISABLED_TOOLS) ?: return emptySet()
        if (raw.isBlank()) return emptySet()
        return raw.split(",")
            .mapNotNull { token ->
                val name = token.trim()
                if (name.isEmpty()) null else com.simplestsoft.twostrokecalc.domain.model.ToolId.fromName(name)
            }
            .toSet()
    }

    fun setDisabledTools(ids: Set<com.simplestsoft.twostrokecalc.domain.model.ToolId>) {
        val value = ids.joinToString(",") { it.name }
        if (value.isEmpty()) settings.remove(KEY_DISABLED_TOOLS) else settings[KEY_DISABLED_TOOLS] = value
    }

    fun getToolSessionsJson(): String? = settings.getStringOrNull(KEY_TOOL_SESSIONS_JSON)

    fun setToolSessionsJson(value: String?) {
        if (value.isNullOrEmpty()) settings.remove(KEY_TOOL_SESSIONS_JSON) else settings[KEY_TOOL_SESSIONS_JSON] = value
    }

    fun getVehicleCatalogVersion(): Int = settings.getInt(KEY_VEHICLE_CATALOG_VERSION, 0)

    fun setVehicleCatalogVersion(version: Int) {
        settings[KEY_VEHICLE_CATALOG_VERSION] = version
    }

    fun getVehicleCatalogJson(): String? = settings.getStringOrNull(KEY_VEHICLE_CATALOG_JSON)

    fun setVehicleCatalogJson(value: String?) {
        if (value.isNullOrEmpty()) settings.remove(KEY_VEHICLE_CATALOG_JSON) else settings[KEY_VEHICLE_CATALOG_JSON] = value
    }

    fun getEngineCatalogVersion(): Int = settings.getInt(KEY_ENGINE_CATALOG_VERSION, 0)

    fun setEngineCatalogVersion(version: Int) {
        settings[KEY_ENGINE_CATALOG_VERSION] = version
    }

    fun getEngineCatalogJson(): String? = settings.getStringOrNull(KEY_ENGINE_CATALOG_JSON)

    fun setEngineCatalogJson(value: String?) {
        if (value.isNullOrEmpty()) settings.remove(KEY_ENGINE_CATALOG_JSON) else settings[KEY_ENGINE_CATALOG_JSON] = value
    }

    fun getSettingsUpdatedAt(): Long = settings.getLong(KEY_SETTINGS_UPDATED_AT, 0L)

    fun setSettingsUpdatedAt(timestamp: Long) {
        settings[KEY_SETTINGS_UPDATED_AT] = timestamp
    }

    fun applySettingsFromCloud(
        themeMode: ThemeMode? = null,
        languageMode: LanguageMode? = null,
        navStyle: NavStyle? = null,
        welcomeCompleted: Boolean? = null,
        notificationsEnabled: Boolean? = null,
        displayName: String? = null,
        updatedAt: Long,
    ) {
        themeMode?.let { settings[KEY_THEME] = it.name }
        languageMode?.let { settings[KEY_LANGUAGE] = it.name }
        navStyle?.let { settings[KEY_NAV_STYLE] = it.name }
        welcomeCompleted?.let { settings[KEY_WELCOME_DONE] = it }
        notificationsEnabled?.let { settings[KEY_NOTIFICATIONS] = it }
        displayName?.takeIf { it.isNotBlank() }?.let { settings[KEY_DISPLAY_NAME] = it }
        settings[KEY_SETTINGS_UPDATED_AT] = updatedAt
    }

    private fun markSettingsLocallyModified() {
        setSettingsUpdatedAt(System.currentTimeMillis())
    }

    private companion object {
        const val KEY_USER_ID = "user_id"
        const val KEY_IS_ADMIN = "is_admin"
        const val KEY_IS_PRO = "is_pro"
        const val KEY_PRO_MODULES = "pro_modules"
        const val KEY_DEMO_VEHICLES_ENABLED = "demo_vehicles_enabled"
        const val KEY_DISPLAY_NAME = "display_name"
        const val KEY_ACCOUNT_EMAIL = "account_email"
        const val KEY_DISABLED_CALCULATORS = "disabled_calculators"
        const val KEY_DISABLED_TOOLS = "disabled_tools"
        const val KEY_TOOL_SESSIONS_JSON = "tool_sessions_json"
        const val KEY_VEHICLE_CATALOG_VERSION = "vehicle_catalog_version"
        const val KEY_VEHICLE_CATALOG_JSON = "vehicle_catalog_json"
        const val KEY_ENGINE_CATALOG_VERSION = "engine_catalog_version"
        const val KEY_ENGINE_CATALOG_JSON = "engine_catalog_json"
        const val KEY_THEME = "theme_mode"
        const val KEY_LANGUAGE = "language_mode"
        const val KEY_NAV_STYLE = "nav_style"
        const val KEY_WELCOME_DONE = "welcome_completed"
        const val KEY_WALKTHROUGH_DONE = "walkthrough_completed"
        const val KEY_NOTIFICATIONS = "notifications_enabled"
        const val KEY_FIRST_INSTALL_PERMISSIONS = "first_install_permissions_completed"
        const val KEY_SETTINGS_UPDATED_AT = "settings_updated_at"
    }
}
