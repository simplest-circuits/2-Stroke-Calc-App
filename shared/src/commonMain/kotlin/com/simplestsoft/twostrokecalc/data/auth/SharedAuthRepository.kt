package com.simplestsoft.twostrokecalc.data.auth

import com.simplestsoft.twostrokecalc.data.preferences.AppPreferencesStore
import com.simplestsoft.twostrokecalc.data.session.UserSession
import com.simplestsoft.twostrokecalc.domain.model.UserRole
import com.simplestsoft.twostrokecalc.domain.model.remote.AccountApi
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.FieldValue
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class AuthUserInfo(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: String?,
)

class SharedAuthRepository(
    private val userSession: UserSession,
    private val preferencesStore: AppPreferencesStore,
    private val accountApi: AccountApi,
) {
    val authState: Flow<AuthUserInfo?> =
        Firebase.auth.authStateChanged.map { user -> user?.toAuthUserInfo() }

    suspend fun signInWithEmail(email: String, password: String): Result<Unit> =
        runCatching {
            Firebase.auth.signInWithEmailAndPassword(email.trim(), password)
            val user = Firebase.auth.currentUser ?: error("No user after sign-in")
            onSignedIn(user)
        }

    suspend fun register(email: String, password: String, displayName: String?): Result<Unit> =
        runCatching {
            Firebase.auth.createUserWithEmailAndPassword(email.trim(), password)
            val user = Firebase.auth.currentUser ?: error("No user after registration")
            if (!displayName.isNullOrBlank()) {
                user.updateProfile(displayName = displayName.trim())
            }
            onSignedIn(user)
        }

    suspend fun sendPasswordReset(email: String): Result<Unit> =
        runCatching {
            Firebase.auth.sendPasswordResetEmail(email.trim())
        }

    suspend fun signInWithGoogleIdToken(idToken: String): Result<Unit> =
        runCatching {
            Firebase.auth.signInWithCredential(
                dev.gitlive.firebase.auth.GoogleAuthProvider.credential(idToken, null),
            )
            val user = Firebase.auth.currentUser ?: error("No user after Google sign-in")
            onSignedIn(user)
        }

    suspend fun signOut() {
        Firebase.auth.signOut()
        userSession.setUserId(null)
        preferencesStore.clearUserScoped()
    }

    suspend fun updatePassword(currentPassword: String, newPassword: String): Result<Unit> =
        runCatching {
            val user = Firebase.auth.currentUser ?: error("Not signed in")
            val email = user.email ?: error("No email")
            user.reauthenticate(
                dev.gitlive.firebase.auth.EmailAuthProvider.credential(email, currentPassword),
            )
            user.updatePassword(newPassword)
        }

    suspend fun ensureFirestoreUserProfile() {
        val user = Firebase.auth.currentUser ?: return
        persistUserSession(
            uid = user.uid,
            email = user.email,
            displayName = user.displayName,
            photoUrl = user.photoURL,
        )
        val snapshot = Firebase.firestore.collection(USERS_COLLECTION).document(user.uid).get()
        if (!snapshot.exists) {
            syncUserProfileViaBackend()
        }
    }

    suspend fun loadUserRoleFromFirestore(): UserRole {
        val user = Firebase.auth.currentUser ?: return UserRole.USER
        val snap = Firebase.firestore.collection(USERS_COLLECTION).document(user.uid).get()
        val isAdmin = snap.get<Boolean>("isAdmin") == true
        val isPro = snap.get<Boolean>("isPro") == true
        val roleStr = snap.get<String>("role")
        val fromFirestore = when (roleStr?.uppercase()) {
            UserRole.ADMIN.name -> UserRole.ADMIN
            else -> null
        }
        val email = snap.get<String>("email") ?: user.email
        val resolved = if (isAdmin) UserRole.ADMIN else fromFirestore ?: resolveRoleFromEmail(email)
        preferencesStore.setIsAdmin(resolved == UserRole.ADMIN)
        preferencesStore.setIsPro(isPro)
        return resolved
    }

    fun currentUserId(): String? = Firebase.auth.currentUser?.uid

    private suspend fun onSignedIn(user: FirebaseUser) {
        persistUserSession(
            uid = user.uid,
            email = user.email,
            displayName = user.displayName,
            photoUrl = user.photoURL,
        )
    }

    private suspend fun persistUserSession(
        uid: String,
        email: String?,
        displayName: String?,
        photoUrl: String?,
    ) {
        userSession.setUserId(uid)
        preferencesStore.setUserId(uid)
        if (!email.isNullOrBlank()) {
            preferencesStore.setAccountEmail(email.trim())
        }
        val resolvedDisplay = displayName?.takeIf { it.isNotBlank() }
            ?: email?.trim()?.substringBefore("@")?.takeIf { it.isNotBlank() }
        preferencesStore.setDisplayName(resolvedDisplay)

        val docRef = Firebase.firestore.collection(USERS_COLLECTION).document(uid)
        val snapshot = docRef.get()
        val existingAdmin = snapshot.get<Boolean>("isAdmin") == true ||
            snapshot.get<String>("role")?.uppercase() == UserRole.ADMIN.name
        val existingPro = snapshot.get<Boolean>("isPro") == true
        val role = if (existingAdmin) UserRole.ADMIN else resolveRoleFromEmail(email)
        preferencesStore.setIsAdmin(role == UserRole.ADMIN)
        preferencesStore.setIsPro(existingPro)

        val payload = mutableMapOf<String, Any?>(
            "email" to email,
            "displayName" to (displayName?.takeIf { it.isNotBlank() } ?: resolvedDisplay),
            "photoUrl" to photoUrl,
            "updatedAt" to FieldValue.serverTimestamp,
        )
        if (!snapshot.exists) {
            payload["createdAt"] = FieldValue.serverTimestamp
            payload["active"] = true
            payload["banned"] = false
            payload["role"] = UserRole.USER.name
            payload["isAdmin"] = false
            payload["isPro"] = false
        }
        runCatching {
            docRef.set(payload, merge = true)
        }.onFailure {
            syncUserProfileViaBackend()
        }
    }

    private suspend fun syncUserProfileViaBackend() {
        runCatching {
            val success = accountApi.ensureProfile()
            if (!success) {
                error("ensure-profile failed")
            }
        }
    }

    private fun resolveRoleFromEmail(email: String?): UserRole {
        if (email?.lowercase()?.trim() == "admin@example.com") return UserRole.ADMIN
        return UserRole.USER
    }

    private fun FirebaseUser.toAuthUserInfo() = AuthUserInfo(
        uid = uid,
        email = email,
        displayName = displayName,
        photoUrl = photoURL,
    )

    companion object {
        const val USERS_COLLECTION = "users"
    }
}
