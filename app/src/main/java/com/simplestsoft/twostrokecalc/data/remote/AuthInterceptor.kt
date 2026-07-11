package com.simplestsoft.twostrokecalc.data.remote

import com.simplestsoft.twostrokecalc.data.session.UserSession
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val userSession: UserSession,
    private val firebaseAuth: FirebaseAuth,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val user = firebaseAuth.currentUser
        var request = authorizeRequest(chain.request(), user, forceRefresh = false)
        var response = chain.proceed(request)
        if (response.code == 401 && user != null) {
            response.close()
            request = authorizeRequest(chain.request(), user, forceRefresh = true)
            response = chain.proceed(request)
        }
        return response
    }

    private fun authorizeRequest(
        request: Request,
        user: FirebaseUser?,
        forceRefresh: Boolean,
    ): Request {
        val builder = request.newBuilder()
        if (user != null) {
            builder.header("X-User-Id", user.uid)
            fetchIdToken(user, forceRefresh)?.let { token ->
                builder.header("Authorization", "Bearer $token")
            }
        } else {
            userSession.getUserId()?.let { builder.header("X-User-Id", it) }
        }
        return builder.build()
    }

    private fun fetchIdToken(user: FirebaseUser, forceRefresh: Boolean): String? {
        var refresh = forceRefresh
        repeat(MAX_TOKEN_ATTEMPTS) {
            try {
                val token = Tasks.await(user.getIdToken(refresh)).token
                if (!token.isNullOrBlank()) return token
            } catch (_: Exception) {
                refresh = true
            }
        }
        return null
    }

    companion object {
        private const val MAX_TOKEN_ATTEMPTS = 3
    }
}
