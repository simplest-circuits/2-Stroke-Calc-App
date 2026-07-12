package com.simplestsoft.twostrokecalc.domain.model

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

enum class LanguageMode {
    SYSTEM,
    GERMAN,
    ENGLISH,
    ;

    fun resolveLanguage(): String = when (this) {
        GERMAN -> "de"
        ENGLISH -> "en"
        SYSTEM -> "de"
    }
}

enum class NavStyle {
    BOTTOM_BAR,
    DRAWER,
}

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val languageMode: LanguageMode = LanguageMode.SYSTEM,
    val navStyle: NavStyle = NavStyle.BOTTOM_BAR,
    val welcomeCompleted: Boolean = false,
    val walkthroughCompleted: Boolean = false,
    val notificationsEnabled: Boolean = false,
    val firstInstallPermissionsCompleted: Boolean = false,
    val userId: String? = null,
    val isAdmin: Boolean = false,
    val isPro: Boolean = false,
    val displayName: String? = null,
    val accountEmail: String? = null,
)
