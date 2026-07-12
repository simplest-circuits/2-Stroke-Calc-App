package com.simplestsoft.twostrokecalc.logging

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

interface ErrorLogger {
    fun log(
        tag: String,
        message: String,
        throwable: Throwable? = null,
        details: Map<String, String>? = null,
    )
}

@Singleton
class DefaultErrorLogger @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val metadataCollector: ErrorLogMetadataCollector,
) : ErrorLogger {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun log(
        tag: String,
        message: String,
        throwable: Throwable?,
        details: Map<String, String>?,
    ) {
        if (throwable != null) {
            Log.e(tag, message, throwable)
        } else {
            Log.w(tag, message)
        }
        if (auth.currentUser?.uid == null) return
        scope.launch {
            runCatching {
                val payload = linkedMapOf<String, Any>(
                    "tag" to tag,
                    "message" to message,
                    "ts" to FieldValue.serverTimestamp(),
                )
                throwable?.let { error ->
                    payload["exceptionType"] = error.javaClass.name
                    error.message?.trim()?.takeIf { it.isNotEmpty() }?.let { payload["exceptionMessage"] = it }
                    payload["throwable"] = error.stackTraceToString()
                }
                metadataCollector.collect().forEach { (key, value) ->
                    if (value.isNotBlank()) payload[key] = value
                }
                details?.forEach { (key, value) ->
                    if (value.isNotBlank()) payload[key] = value
                }
                firestore.collection("error_logs").add(payload)
            }
        }
    }
}
