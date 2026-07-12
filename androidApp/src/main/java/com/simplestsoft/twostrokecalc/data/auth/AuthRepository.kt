package com.simplestsoft.twostrokecalc.data.auth

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.simplestsoft.twostrokecalc.R
import com.simplestsoft.twostrokecalc.data.preferences.AppPreferencesStore
import com.simplestsoft.twostrokecalc.data.preferences.PreferencesManager
import com.simplestsoft.twostrokecalc.domain.model.UserRole
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach

@Singleton
class AuthRepository @Inject constructor(
    private val sharedAuth: SharedAuthRepository,
    private val preferencesManager: PreferencesManager,
    private val preferencesStore: AppPreferencesStore,
    @ApplicationContext private val appContext: Context,
) {
    val authState: Flow<AuthUserInfo?> = sharedAuth.authState.onEach { user ->
        if (user != null) {
            syncPreferencesFromShared(user)
        }
    }

    suspend fun signInWithEmail(email: String, password: String): Result<Unit> =
        sharedAuth.signInWithEmail(email, password).onSuccess {
            sharedAuth.currentUserId()?.let { syncPreferencesAfterAuth(it) }
        }

    suspend fun register(email: String, password: String, displayName: String?): Result<Unit> =
        sharedAuth.register(email, password, displayName).onSuccess {
            sharedAuth.currentUserId()?.let { syncPreferencesAfterAuth(it) }
        }

    suspend fun sendPasswordReset(email: String): Result<Unit> =
        sharedAuth.sendPasswordReset(email)

    suspend fun signInWithGoogle(activityContext: Context): Result<Unit> =
        runCatching {
            val serverClientId = activityContext.getString(R.string.default_web_client_id)
            val credentialManager = CredentialManager.create(activityContext)
            val result = runCatching {
                credentialManager.getCredential(
                    activityContext,
                    buildSignInWithGoogleRequest(serverClientId),
                )
            }.getOrElse { primaryError ->
                if (primaryError is GetCredentialCancellationException) throw primaryError
                credentialManager.getCredential(
                    activityContext,
                    buildGoogleCredentialRequest(serverClientId, filterByAuthorizedAccounts = false),
                )
            }
            val credential = result.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                sharedAuth.signInWithGoogleIdToken(googleIdTokenCredential.idToken).getOrThrow()
                sharedAuth.currentUserId()?.let { syncPreferencesAfterAuth(it) }
            } else {
                error("Unsupported credential type")
            }
        }

    suspend fun signOut() {
        sharedAuth.signOut()
        runCatching {
            CredentialManager.create(appContext).clearCredentialState(ClearCredentialStateRequest())
        }
        preferencesManager.clearUserScoped()
    }

    suspend fun updatePassword(currentPassword: String, newPassword: String): Result<Unit> =
        sharedAuth.updatePassword(currentPassword, newPassword)

    suspend fun ensureFirestoreUserProfile() {
        sharedAuth.ensureFirestoreUserProfile()
        sharedAuth.currentUserId()?.let { syncPreferencesAfterAuth(it) }
    }

    suspend fun loadUserRoleFromFirestore(): UserRole {
        val role = sharedAuth.loadUserRoleFromFirestore()
        val isAdmin = preferencesStore.getIsAdmin()
        val isPro = preferencesStore.getIsPro()
        preferencesManager.setIsAdmin(isAdmin)
        preferencesManager.setIsPro(isPro)
        return role
    }

    private suspend fun syncPreferencesAfterAuth(uid: String) {
        preferencesManager.setUserId(uid)
        preferencesStore.getAccountEmail()?.let { preferencesManager.setAccountEmail(it) }
        preferencesStore.getDisplayName()?.let { preferencesManager.setDisplayName(it) }
        preferencesManager.setIsAdmin(preferencesStore.getIsAdmin())
        preferencesManager.setIsPro(preferencesStore.getIsPro())
    }

    private suspend fun syncPreferencesFromShared(user: AuthUserInfo) {
        preferencesManager.setUserId(user.uid)
        user.email?.let { preferencesManager.setAccountEmail(it) }
        user.displayName?.let { preferencesManager.setDisplayName(it) }
    }

    private fun buildGoogleCredentialRequest(
        serverClientId: String,
        filterByAuthorizedAccounts: Boolean,
    ): GetCredentialRequest {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(filterByAuthorizedAccounts)
            .setServerClientId(serverClientId)
            .build()
        return GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
    }

    private fun buildSignInWithGoogleRequest(serverClientId: String): GetCredentialRequest {
        val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(serverClientId).build()
        return GetCredentialRequest.Builder()
            .addCredentialOption(signInWithGoogleOption)
            .build()
    }
}
