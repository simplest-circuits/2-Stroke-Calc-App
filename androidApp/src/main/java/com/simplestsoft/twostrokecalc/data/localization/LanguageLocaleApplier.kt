package com.simplestsoft.twostrokecalc.data.localization

import androidx.appcompat.app.AppCompatDelegate
import com.simplestsoft.twostrokecalc.domain.localization.AppLanguageProfile
import com.simplestsoft.twostrokecalc.domain.model.Language
import com.simplestsoft.twostrokecalc.domain.model.LanguageMode
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LanguageLocaleApplier @Inject constructor() {
    fun apply(profile: AppLanguageProfile) {
        AppCompatDelegate.setApplicationLocales(profile.localeList)
    }

    fun apply(language: Language) {
        apply(AppLanguageProfile.from(language))
    }

    fun applyForMode(mode: LanguageMode) {
        apply(mode.resolveLanguage())
    }
}
