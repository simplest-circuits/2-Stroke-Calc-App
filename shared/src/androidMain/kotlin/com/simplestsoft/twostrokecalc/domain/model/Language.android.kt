package com.simplestsoft.twostrokecalc.domain.model

import java.util.Locale

actual fun systemLanguage(): Language =
    when (Locale.getDefault().language.lowercase()) {
        "de" -> Language.GERMAN
        "en" -> Language.ENGLISH
        "es" -> Language.SPANISH
        "pt" -> Language.PORTUGUESE
        "sv" -> Language.SWEDISH
        "da" -> Language.DANISH
        "nb", "no" -> Language.NORWEGIAN
        else -> Language.ENGLISH
    }

actual fun isAppSystemLanguageSupported(): Boolean =
    Language.supportedSystemLanguageCodes.contains(Locale.getDefault().language.lowercase())
