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
    SPANISH,
    PORTUGUESE,
    ;

    fun resolveLanguage(): Language = when (this) {
        SYSTEM -> systemLanguage()
        GERMAN -> Language.GERMAN
        ENGLISH -> Language.ENGLISH
        SPANISH -> Language.SPANISH
        PORTUGUESE -> Language.PORTUGUESE
    }
}

enum class NavStyle {
    BOTTOM_BAR,
    DRAWER,
}

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val languageMode: LanguageMode = LanguageMode.SYSTEM,
    val language: Language = LanguageMode.SYSTEM.resolveLanguage(),
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
