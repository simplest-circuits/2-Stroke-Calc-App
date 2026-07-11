package com.simplestsoft.twostrokecalc.ui.util

import android.content.Context
import com.simplestsoft.twostrokecalc.R
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException
import javax.net.ssl.SSLHandshakeException

/**
 * Maps network-related throwables and raw messages to localized, user-friendly strings.
 */
object NetworkExceptionMessage {

    fun resolve(context: Context, throwable: Throwable?, fallback: String): String {
        messageForThrowable(context, throwable)?.let { return it }
        val raw = throwable?.message?.trim().orEmpty()
        if (raw.isNotBlank() && looksLikeNetworkFailure(raw)) {
            return messageForRawNetworkText(context, raw)
        }
        return fallback
    }

    fun messageForThrowable(context: Context, throwable: Throwable?): String? {
        var current: Throwable? = throwable
        val seen = HashSet<Throwable>()
        while (current != null && current !in seen) {
            seen.add(current)
            when (current) {
                is UnknownHostException, is ConnectException ->
                    return context.getString(R.string.error_no_internet_connection)
                is SocketTimeoutException ->
                    return context.getString(R.string.error_network_timeout)
                is SSLHandshakeException, is SSLException ->
                    return context.getString(R.string.error_network_ssl)
            }
            current.message?.trim()?.takeIf { it.isNotBlank() }?.let { message ->
                if (looksLikeNetworkFailure(message)) {
                    return messageForRawNetworkText(context, message)
                }
            }
            current = current.cause
        }
        if (throwable is IOException) {
            val message = throwable.message?.trim().orEmpty()
            if (message.isBlank() || looksLikeNetworkFailure(message)) {
                return context.getString(R.string.error_no_internet_connection)
            }
        }
        return null
    }

    fun looksLikeNetworkFailure(message: String): Boolean {
        val lower = message.lowercase()
        return lower.contains("unable to resolve host") ||
            lower.contains("no address associated with hostname") ||
            lower.contains("failed to connect") ||
            lower.contains("network is unreachable") ||
            lower.contains("connection refused") ||
            lower.contains("connection reset") ||
            lower.contains("connection timed out") ||
            lower.contains("software caused connection abort") ||
            lower.contains("enetunreach") ||
            lower.contains("econnrefused") ||
            lower.contains("etimedout") ||
            (lower.contains("timeout") && (
                lower.contains("connect") ||
                    lower.contains("read") ||
                    lower.contains("socket")
                ))
    }

    private fun messageForRawNetworkText(context: Context, message: String): String {
        if (message.contains("timeout", ignoreCase = true)) {
            return context.getString(R.string.error_network_timeout)
        }
        return context.getString(R.string.error_no_internet_connection)
    }
}
