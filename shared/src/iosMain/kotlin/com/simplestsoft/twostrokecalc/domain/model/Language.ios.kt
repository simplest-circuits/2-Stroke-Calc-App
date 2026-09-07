package com.simplestsoft.twostrokecalc.domain.model

import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.languageCode

actual fun systemLanguage(): Language {
    val code = NSLocale.currentLocale.languageCode?.lowercase() ?: return Language.ENGLISH
    return when (code) {
        "de" -> Language.GERMAN
        "es" -> Language.SPANISH
        "pt" -> Language.PORTUGUESE
        "sv" -> Language.SWEDISH
        "da" -> Language.DANISH
        "nb", "no" -> Language.NORWEGIAN
        "en" -> Language.ENGLISH
        else -> Language.ENGLISH
    }
}

actual fun isAppSystemLanguageSupported(): Boolean {
    val code = NSLocale.currentLocale.languageCode?.lowercase() ?: return false
    return Language.supportedSystemLanguageCodes.contains(code)
}
