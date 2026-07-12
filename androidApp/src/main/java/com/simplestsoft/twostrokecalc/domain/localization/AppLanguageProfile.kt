package com.simplestsoft.twostrokecalc.domain.localization

import androidx.core.os.LocaleListCompat
import com.simplestsoft.twostrokecalc.domain.model.Language
import com.simplestsoft.twostrokecalc.domain.model.LanguageMode
import java.util.Locale

data class AppLanguageProfile(
    val language: Language,
) {
    val storageCode: String get() = language.code

    val localeTag: String get() = language.code

    val javaLocale: Locale
        get() = when (language) {
            Language.GERMAN -> Locale.GERMANY
            Language.ENGLISH -> Locale.US
            Language.SPANISH -> Locale.forLanguageTag("es")
            Language.PORTUGUESE -> Locale.forLanguageTag("pt")
        }

    val localeList: LocaleListCompat
        get() = LocaleListCompat.forLanguageTags(localeTag)

    companion object {
        val default: AppLanguageProfile = from(LanguageMode.SYSTEM.resolveLanguage())

        fun from(language: Language): AppLanguageProfile = AppLanguageProfile(language)
    }
}
