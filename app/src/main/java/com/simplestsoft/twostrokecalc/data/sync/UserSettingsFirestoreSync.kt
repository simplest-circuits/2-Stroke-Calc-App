package com.simplestsoft.twostrokecalc.data.sync

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.simplestsoft.twostrokecalc.data.auth.AuthRepository
import com.simplestsoft.twostrokecalc.data.preferences.PreferencesManager
import com.simplestsoft.twostrokecalc.domain.model.Language
import com.simplestsoft.twostrokecalc.domain.model.LanguageMode
import com.simplestsoft.twostrokecalc.domain.model.NavStyle
import com.simplestsoft.twostrokecalc.domain.model.ThemeMode
import com.simplestsoft.twostrokecalc.logging.ErrorLogger
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await

@Singleton
class UserSettingsFirestoreSync @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
    private val preferencesManager: PreferencesManager,
    private val errorLogger: ErrorLogger,
) {

    suspend fun syncOnLogin(uid: String) {
        val doc = userDocument(uid).get().await()
        if (!doc.exists() || !hasStoredSettings(doc)) {
            pushAllFromLocal(uid)
            return
        }
        loadFromDocument(doc)
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

    suspend fun persistWelcomeCompleted(done: Boolean) {
        persistFields(mapOf(FIELD_WELCOME_COMPLETED to done))
    }

    suspend fun persistNotificationsEnabled(enabled: Boolean) {
        persistFields(mapOf(FIELD_NOTIFICATIONS_ENABLED to enabled))
    }

    suspend fun persistDisplayName(name: String) {
        persistFields(mapOf(FIELD_DISPLAY_NAME to name))
    }

    private suspend fun pushAllFromLocal(uid: String) {
        val prefs = preferencesManager.preferencesFlow.first()
        val payload = linkedMapOf<String, Any>(
            FIELD_THEME_MODE to prefs.themeMode.name,
            FIELD_LANGUAGE_MODE to prefs.languageMode.name,
            FIELD_LANGUAGE to prefs.language.code,
            FIELD_NAV_STYLE to prefs.navStyle.name,
            FIELD_WELCOME_COMPLETED to prefs.welcomeCompleted,
            FIELD_NOTIFICATIONS_ENABLED to prefs.notificationsEnabled,
            FIELD_SETTINGS_UPDATED_AT to FieldValue.serverTimestamp(),
        )
        prefs.displayName?.takeIf { it.isNotBlank() }?.let { payload[FIELD_DISPLAY_NAME] = it }
        runCatching {
            userDocument(uid).set(payload, SetOptions.merge()).await()
        }.onFailure { errorLogger.log("UserSettingsFirestoreSync", "pushAllFromLocal", it) }
    }

    private suspend fun loadFromDocument(doc: DocumentSnapshot) {
        doc.getString(FIELD_THEME_MODE)?.let { value ->
            runCatching { ThemeMode.valueOf(value) }.getOrNull()
                ?.let { preferencesManager.setThemeMode(it) }
        }
        doc.getString(FIELD_LANGUAGE_MODE)?.let { value ->
            runCatching { LanguageMode.valueOf(value) }.getOrNull()
                ?.let { preferencesManager.setLanguageMode(it) }
        } ?: doc.getString(FIELD_LANGUAGE)?.let { code ->
            languageModeFromCode(code)?.let { preferencesManager.setLanguageMode(it) }
        }
        doc.getString(FIELD_NAV_STYLE)?.let { value ->
            runCatching { NavStyle.valueOf(value) }.getOrNull()
                ?.let { preferencesManager.setNavStyle(it) }
        }
        if (doc.contains(FIELD_WELCOME_COMPLETED)) {
            preferencesManager.setWelcomeCompleted(doc.getBoolean(FIELD_WELCOME_COMPLETED) == true)
        }
        if (doc.contains(FIELD_NOTIFICATIONS_ENABLED)) {
            preferencesManager.setNotificationsEnabled(doc.getBoolean(FIELD_NOTIFICATIONS_ENABLED) == true)
        }
        doc.getString(FIELD_DISPLAY_NAME)?.takeIf { it.isNotBlank() }
            ?.let { preferencesManager.setDisplayName(it) }
    }

    private suspend fun persistFields(fields: Map<String, Any>) {
        val uid = firebaseAuth.currentUser?.uid ?: return
        val payload = fields.toMutableMap()
        payload[FIELD_SETTINGS_UPDATED_AT] = FieldValue.serverTimestamp()
        runCatching {
            userDocument(uid).set(payload, SetOptions.merge()).await()
        }.onFailure { errorLogger.log("UserSettingsFirestoreSync", "persistFields", it) }
    }

    private fun userDocument(uid: String) =
        firestore.collection(AuthRepository.USERS_COLLECTION).document(uid)

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
            else -> null
        }

    companion object {
        private const val FIELD_THEME_MODE = "themeMode"
        private const val FIELD_LANGUAGE_MODE = "languageMode"
        private const val FIELD_LANGUAGE = "language"
        private const val FIELD_NAV_STYLE = "navStyle"
        private const val FIELD_WELCOME_COMPLETED = "welcomeCompleted"
        private const val FIELD_NOTIFICATIONS_ENABLED = "notificationsEnabled"
        private const val FIELD_DISPLAY_NAME = "displayName"
        private const val FIELD_SETTINGS_UPDATED_AT = "settingsUpdatedAt"
    }
}
