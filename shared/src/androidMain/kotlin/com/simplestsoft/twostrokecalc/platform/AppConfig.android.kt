package com.simplestsoft.twostrokecalc.platform

import java.util.Properties

actual object AppConfig {
    private val properties: Properties by lazy {
        Properties().apply {
            val file = sequenceOf(
                "firebase.properties",
                "../firebase.properties",
            ).mapNotNull { path ->
                java.io.File(path).takeIf { it.exists() }
            }.firstOrNull()
            if (file != null) {
                file.inputStream().use { load(it) }
            }
        }
    }

    actual val baseUrl: String = run {
        val raw = properties.getProperty(
            "base_url",
            "https://europe-west3-strokecalc-app.cloudfunctions.net/api/",
        )
        if (raw.endsWith("/")) raw else "$raw/"
    }

    actual val firebaseProjectId: String =
        properties.getProperty("firebase.projectId", "strokecalc-app")

    actual val firebaseApiKey: String =
        properties.getProperty("firebase.apiKey", "")

    actual val isDebug: Boolean = true
}
