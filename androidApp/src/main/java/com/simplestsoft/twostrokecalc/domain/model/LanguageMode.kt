package com.simplestsoft.twostrokecalc.domain.model

enum class LanguageMode {
    SYSTEM,
    GERMAN,
    ENGLISH,
    SPANISH,
    PORTUGUESE,
    ;

    fun resolveLanguage(): Language = when (this) {
        SYSTEM -> Language.fromSystemLocale()
        GERMAN -> Language.GERMAN
        ENGLISH -> Language.ENGLISH
        SPANISH -> Language.SPANISH
        PORTUGUESE -> Language.PORTUGUESE
    }
}
