package com.simplestsoft.twostrokecalc.domain.model

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val languageMode: LanguageMode = LanguageMode.SYSTEM,
    val language: Language = LanguageMode.SYSTEM.resolveLanguage(),
    val navStyle: NavStyle = NavStyle.BOTTOM_BAR,
    val welcomeCompleted: Boolean = false,
    val notificationsEnabled: Boolean = false,
    val firstInstallPermissionsCompleted: Boolean = false,
    val userId: String? = null,
    val isAdmin: Boolean = false,
    val isPro: Boolean = false,
    val displayName: String? = null,
    val accountEmail: String? = null,
)
