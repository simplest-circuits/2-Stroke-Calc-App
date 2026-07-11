package com.simplestsoft.twostrokecalc.data.auth

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.simplestsoft.twostrokecalc.R
import com.simplestsoft.twostrokecalc.data.remote.AccountApiService
import com.simplestsoft.twostrokecalc.data.preferences.PreferencesManager
import com.simplestsoft.twostrokecalc.data.session.UserSession
import com.simplestsoft.twostrokecalc.domain.model.UserRole
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

@Singleton
class AuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val preferencesManager: PreferencesManager,
    private val userSession: UserSession,
    private val accountApiService: AccountApiService,
    @ApplicationContext private val appContext: Context,
) {

    val authState: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            userSession.setUserId(auth.currentUser?.uid)
            trySend(auth.currentUser)
        }
        firebaseAuth.addAuthStateListener(listener)
        userSession.setUserId(firebaseAuth.currentUser?.uid)
        trySend(firebaseAuth.currentUser)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    suspend fun signInWithEmail(email: String, password: String): Result<Unit> =
        runCatching {
            firebaseAuth.signInWithEmailAndPassword(email.trim(), password).await()
            onSignedIn(firebaseAuth.currentUser!!)
        }

    suspend fun register(email: String, password: String, displayName: String?): Result<Unit> =
        runCatching {
            firebaseAuth.createUserWithEmailAndPassword(email.trim(), password).await()
            val user = firebaseAuth.currentUser!!
            if (!displayName.isNullOrBlank()) {
                user.updateProfile(
                    UserProfileChangeRequest.Builder()
                        .setDisplayName(displayName.trim())
                        .build(),
                ).await()
            }
            onSignedIn(user)
        }

    suspend fun sendPasswordReset(email: String): Result<Unit> =
        runCatching {
            firebaseAuth.sendPasswordResetEmail(email.trim()).await()
        }

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
            signInWithGoogleCredential(result.credential)
        }

    private suspend fun signInWithGoogleCredential(credential: androidx.credentials.Credential) {
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
            firebaseAuth.signInWithCredential(firebaseCredential).await()
            onSignedIn(firebaseAuth.currentUser!!)
        } else {
            error("Unsupported credential type")
        }
    }

    suspend fun signOut() {
        firebaseAuth.signOut()
        runCatching {
            CredentialManager.create(appContext).clearCredentialState(ClearCredentialStateRequest())
        }
        userSession.setUserId(null)
        preferencesManager.clearUserScoped()
    }

    suspend fun updatePassword(currentPassword: String, newPassword: String): Result<Unit> =
        runCatching {
            val user = firebaseAuth.currentUser ?: error("Not signed in")
            val email = user.email ?: error("No email")
            val credential = EmailAuthProvider.getCredential(email, currentPassword)
            user.reauthenticate(credential).await()
            user.updatePassword(newPassword).await()
        }

    suspend fun ensureFirestoreUserProfile() {
        val user = firebaseAuth.currentUser ?: return
        persistUserSession(
            uid = user.uid,
            email = user.email,
            displayName = user.displayName,
            photoUrl = user.photoUrl?.toString(),
        )
        val snapshot = firestore.collection(USERS_COLLECTION).document(user.uid).get().await()
        if (!snapshot.exists()) {
            syncUserProfileViaBackend()
        }
    }

    suspend fun loadUserRoleFromFirestore(): UserRole {
        val user = firebaseAuth.currentUser ?: return UserRole.USER
        val snap = firestore.collection(USERS_COLLECTION).document(user.uid).get().await()
        val isAdmin = snap.getBoolean("isAdmin") == true
        val isPro = snap.getBoolean("isPro") == true
        val roleStr = snap.getString("role")
        val fromFirestore = when (roleStr?.uppercase()) {
            UserRole.ADMIN.name -> UserRole.ADMIN
            else -> null
        }
        val email = snap.getString("email") ?: user.email
        val resolved = if (isAdmin) UserRole.ADMIN else fromFirestore ?: resolveRoleFromEmail(email)
        preferencesManager.setIsAdmin(resolved == UserRole.ADMIN)
        preferencesManager.setIsPro(isPro)
        return resolved
    }

    private suspend fun onSignedIn(user: FirebaseUser) {
        persistUserSession(
            uid = user.uid,
            email = user.email,
            displayName = user.displayName,
            photoUrl = user.photoUrl?.toString(),
        )
    }

    private suspend fun persistUserSession(
        uid: String,
        email: String?,
        displayName: String?,
        photoUrl: String?,
    ) {
        userSession.setUserId(uid)
        preferencesManager.setUserId(uid)
        if (!email.isNullOrBlank()) {
            preferencesManager.setAccountEmail(email.trim())
        }
        val resolvedDisplay = displayName?.takeIf { it.isNotBlank() }
            ?: email?.trim()?.substringBefore("@")?.takeIf { it.isNotBlank() }
        preferencesManager.setDisplayName(resolvedDisplay)

        val docRef = firestore.collection(USERS_COLLECTION).document(uid)
        val snapshot = docRef.get().await()
        val existingAdmin = snapshot.getBoolean("isAdmin") == true ||
            snapshot.getString("role")?.uppercase() == UserRole.ADMIN.name
        val existingPro = snapshot.getBoolean("isPro") == true
        val role = if (existingAdmin) UserRole.ADMIN else resolveRoleFromEmail(email)
        preferencesManager.setIsAdmin(role == UserRole.ADMIN)
        preferencesManager.setIsPro(existingPro)

        val payload = hashMapOf<String, Any?>(
            "email" to email,
            "displayName" to (displayName?.takeIf { it.isNotBlank() } ?: resolvedDisplay),
            "photoUrl" to photoUrl,
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        if (!snapshot.exists()) {
            payload["createdAt"] = FieldValue.serverTimestamp()
            payload["active"] = true
            payload["banned"] = false
            payload["role"] = UserRole.USER.name
            payload["isAdmin"] = false
            payload["isPro"] = false
        }
        runCatching {
            docRef.set(payload, SetOptions.merge()).await()
        }.onFailure {
            syncUserProfileViaBackend()
        }
    }

    private suspend fun syncUserProfileViaBackend() {
        runCatching {
            val response = accountApiService.ensureProfile()
            if (!response.isSuccessful) {
                error("ensure-profile failed: ${response.code()}")
            }
        }
    }

    private fun resolveRoleFromEmail(email: String?): UserRole {
        if (email?.lowercase()?.trim() == "admin@example.com") return UserRole.ADMIN
        return UserRole.USER
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

    companion object {
        const val USERS_COLLECTION = "users"
    }
}
