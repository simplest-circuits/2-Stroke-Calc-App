package com.simplestsoft.twostrokecalc.ui.auth

import android.content.Context
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.simplestsoft.twostrokecalc.R
import com.google.firebase.auth.FirebaseAuthException
import com.simplestsoft.twostrokecalc.ui.util.NetworkExceptionMessage

/**
 * Maps [FirebaseAuthException] codes to localized strings for login, register and password reset.
 */
object AuthExceptionMessage {

    fun getMessage(context: Context, throwable: Throwable?): String {
        if (throwable is GetCredentialCancellationException) {
            return context.getString(R.string.auth_error_google_cancelled)
        }
        if (throwable is GetCredentialException) {
            return context.getString(R.string.auth_error_google_config)
        }
        val e = throwable as? FirebaseAuthException
            ?: return NetworkExceptionMessage.resolve(
                context,
                throwable,
                throwable?.message?.takeIf { it.isNotBlank() }
                    ?: context.getString(R.string.auth_error_generic),
            )
        return when (e.errorCode) {
            "ERROR_INVALID_EMAIL" -> context.getString(R.string.auth_error_invalid_email)
            "ERROR_WRONG_PASSWORD",
            "ERROR_INVALID_CREDENTIAL",
            -> context.getString(R.string.auth_error_wrong_password)
            "ERROR_USER_NOT_FOUND" -> context.getString(R.string.auth_error_user_not_found)
            "ERROR_USER_DISABLED" -> context.getString(R.string.auth_error_user_disabled)
            "ERROR_EMAIL_ALREADY_IN_USE" -> context.getString(R.string.auth_error_email_already_in_use)
            "ERROR_WEAK_PASSWORD" -> context.getString(R.string.auth_error_weak_password)
            "ERROR_NETWORK_REQUEST_FAILED" -> context.getString(R.string.auth_error_network)
            "ERROR_TOO_MANY_REQUESTS" -> context.getString(R.string.auth_error_too_many_requests)
            else -> e.message?.takeIf { it.isNotBlank() }
                ?: context.getString(R.string.auth_error_generic)
        }
    }
}
