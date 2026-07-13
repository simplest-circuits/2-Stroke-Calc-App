package com.simplestsoft.twostrokecalc.domain.model

enum class Language(val code: String) {
    ENGLISH("en"),
    GERMAN("de"),
    SPANISH("es"),
    PORTUGUESE("pt"),
    SWEDISH("sv"),
    DANISH("da"),
    NORWEGIAN("nb"),
    ;

    companion object {
        fun fromCode(code: String): Language? = entries.find { it.code.equals(code, ignoreCase = true) }
    }
}

expect fun systemLanguage(): Language
