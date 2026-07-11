package com.simplestsoft.twostrokecalc.domain.model

import androidx.core.os.LocaleListCompat

enum class Language(val code: String) {
    ENGLISH("en"),
    GERMAN("de"),
    SPANISH("es"),
    PORTUGUESE("pt"),
    ;

    companion object {
        fun fromSystemLocale(): Language {
            val localeList = LocaleListCompat.getDefault()
            for (index in 0 until localeList.size()) {
                when (localeList[index]?.language?.lowercase()) {
                    "de" -> return GERMAN
                    "es" -> return SPANISH
                    "pt" -> return PORTUGUESE
                    "en" -> return ENGLISH
                }
            }
            return ENGLISH
        }
    }
}
