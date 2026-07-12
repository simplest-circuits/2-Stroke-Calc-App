package com.simplestsoft.twostrokecalc.domain.model

import java.util.Locale

actual fun systemLanguage(): Language =
    when (Locale.getDefault().language.lowercase()) {
        "de" -> Language.GERMAN
        "es" -> Language.SPANISH
        "pt" -> Language.PORTUGUESE
        else -> Language.ENGLISH
    }
