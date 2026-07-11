package com.simplestsoft.twostrokecalc.domain.model

enum class LanguageMode {
    SYSTEM,
    GERMAN,
    ENGLISH,
    ;

    fun resolveLanguage(): Language = when (this) {
        SYSTEM -> Language.fromSystemLocale()
        GERMAN -> Language.GERMAN
        ENGLISH -> Language.ENGLISH
    }
}
