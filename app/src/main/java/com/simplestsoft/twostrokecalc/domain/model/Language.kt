package com.simplestsoft.twostrokecalc.domain.model

import androidx.core.os.LocaleListCompat

enum class Language(val code: String) {
    ENGLISH("en"),
    GERMAN("de"),
    ;

    companion object {
        fun fromSystemLocale(): Language {
            val localeList = LocaleListCompat.getDefault()
            for (index in 0 until localeList.size()) {
                if (localeList[index]?.language.equals("de", ignoreCase = true)) {
                    return GERMAN
                }
            }
            return ENGLISH
        }
    }
}
