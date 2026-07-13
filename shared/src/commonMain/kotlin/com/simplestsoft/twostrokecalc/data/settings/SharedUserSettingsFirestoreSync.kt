package com.simplestsoft.twostrokecalc.data.settings

import com.simplestsoft.twostrokecalc.data.auth.SharedAuthRepository
import com.simplestsoft.twostrokecalc.data.preferences.AppPreferencesStore
import com.simplestsoft.twostrokecalc.domain.model.Language
import com.simplestsoft.twostrokecalc.domain.model.LanguageMode
import com.simplestsoft.twostrokecalc.domain.model.NavStyle
import com.simplestsoft.twostrokecalc.domain.model.ThemeMode
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.DocumentSnapshot
import dev.gitlive.firebase.firestore.FieldValue
import dev.gitlive.firebase.firestore.firestore

class SharedUserSettingsFirestoreSync(
    private val preferencesStore: AppPreferencesStore,
) {
    suspend fun syncOnLogin(uid: String) {
        val doc = userDocument(uid).get()
        if (!doc.exists || !hasStoredSettings(doc)) {
            pushAllFromLocal(uid)
            return
        }
        loadFromDocument(doc)
    }

    suspend fun persistWelcomeCompleted(done: Boolean) {
        persistFields(mapOf(FIELD_WELCOME_COMPLETED to done))
    }

    suspend fun persistNotificationsEnabled(enabled: Boolean) {
        persistFields(mapOf(FIELD_NOTIFICATIONS_ENABLED to enabled))
    }

    suspend fun persistTheme(mode: ThemeMode) {
        persistFields(mapOf(FIELD_THEME_MODE to mode.name))
    }

    suspend fun persistLanguageMode(mode: LanguageMode) {
        persistFields(
            mapOf(
                FIELD_LANGUAGE_MODE to mode.name,
                FIELD_LANGUAGE to mode.resolveLanguage().code,
            ),
        )
    }

    suspend fun persistNavStyle(style: NavStyle) {
        persistFields(mapOf(FIELD_NAV_STYLE to style.name))
    }

    suspend fun persistDisplayName(name: String) {
        persistFields(mapOf(FIELD_DISPLAY_NAME to name))
    }

    private suspend fun pushAllFromLocal(uid: String) {
        val prefs = preferencesStore.readUserPreferences()
        val payload = linkedMapOf<String, Any>(
            FIELD_THEME_MODE to prefs.themeMode.name,
            FIELD_LANGUAGE_MODE to prefs.languageMode.name,
            FIELD_LANGUAGE to prefs.language.code,
            FIELD_NAV_STYLE to prefs.navStyle.name,
            FIELD_WELCOME_COMPLETED to prefs.welcomeCompleted,
            FIELD_NOTIFICATIONS_ENABLED to prefs.notificationsEnabled,
            FIELD_SETTINGS_UPDATED_AT to FieldValue.serverTimestamp,
        )
        prefs.displayName?.takeIf { it.isNotBlank() }?.let { payload[FIELD_DISPLAY_NAME] = it }
        runCatching {
            userDocument(uid).set(payload, merge = true)
        }
    }

    private fun loadFromDocument(doc: DocumentSnapshot) {
        doc.get<String>(FIELD_THEME_MODE)?.let { value ->
            runCatching { ThemeMode.valueOf(value) }.getOrNull()
                ?.let { preferencesStore.setThemeMode(it) }
        }
        doc.get<String>(FIELD_LANGUAGE_MODE)?.let { value ->
            runCatching { LanguageMode.valueOf(value) }.getOrNull()
                ?.let { preferencesStore.setLanguageMode(it) }
        } ?: doc.get<String>(FIELD_LANGUAGE)?.let { code ->
            languageModeFromCode(code)?.let { preferencesStore.setLanguageMode(it) }
        }
        doc.get<String>(FIELD_NAV_STYLE)?.let { value ->
            runCatching { NavStyle.valueOf(value) }.getOrNull()
                ?.let { preferencesStore.setNavStyle(it) }
        }
        if (doc.contains(FIELD_WELCOME_COMPLETED)) {
            preferencesStore.setWelcomeCompleted(doc.get<Boolean>(FIELD_WELCOME_COMPLETED) == true)
        }
        if (doc.contains(FIELD_NOTIFICATIONS_ENABLED)) {
            preferencesStore.setNotificationsEnabled(doc.get<Boolean>(FIELD_NOTIFICATIONS_ENABLED) == true)
        }
        doc.get<String>(FIELD_DISPLAY_NAME)?.takeIf { it.isNotBlank() }
            ?.let { preferencesStore.setDisplayName(it) }
    }

    private suspend fun persistFields(fields: Map<String, Any>) {
        val uid = Firebase.auth.currentUser?.uid ?: return
        val payload = fields.toMutableMap()
        payload[FIELD_SETTINGS_UPDATED_AT] = FieldValue.serverTimestamp
        runCatching {
            userDocument(uid).set(payload, merge = true)
        }
    }

    private fun userDocument(uid: String) =
        Firebase.firestore.collection(SharedAuthRepository.USERS_COLLECTION).document(uid)

    private fun hasStoredSettings(doc: DocumentSnapshot): Boolean =
        doc.contains(FIELD_THEME_MODE) ||
            doc.contains(FIELD_LANGUAGE_MODE) ||
            doc.contains(FIELD_LANGUAGE) ||
            doc.contains(FIELD_NAV_STYLE) ||
            doc.contains(FIELD_WELCOME_COMPLETED) ||
            doc.contains(FIELD_NOTIFICATIONS_ENABLED)

    private fun languageModeFromCode(code: String): LanguageMode? =
        when (Language.entries.find { it.code == code }) {
            Language.GERMAN -> LanguageMode.GERMAN
            Language.ENGLISH -> LanguageMode.ENGLISH
            Language.SPANISH -> LanguageMode.SPANISH
            Language.PORTUGUESE -> LanguageMode.PORTUGUESE
            Language.SWEDISH -> LanguageMode.SWEDISH
            Language.DANISH -> LanguageMode.DANISH
            Language.NORWEGIAN -> LanguageMode.NORWEGIAN
            else -> null
        }

    private companion object {
        const val FIELD_THEME_MODE = "themeMode"
        const val FIELD_LANGUAGE_MODE = "languageMode"
        const val FIELD_LANGUAGE = "language"
        const val FIELD_NAV_STYLE = "navStyle"
        const val FIELD_WELCOME_COMPLETED = "welcomeCompleted"
        const val FIELD_NOTIFICATIONS_ENABLED = "notificationsEnabled"
        const val FIELD_DISPLAY_NAME = "displayName"
        const val FIELD_SETTINGS_UPDATED_AT = "settingsUpdatedAt"
    }
}
