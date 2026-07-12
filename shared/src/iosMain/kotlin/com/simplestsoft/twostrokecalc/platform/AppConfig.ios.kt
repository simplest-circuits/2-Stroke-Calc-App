package com.simplestsoft.twostrokecalc.platform

actual object AppConfig {
    actual val baseUrl: String =
        "https://europe-west3-strokecalc-app.cloudfunctions.net/api/"

    actual val firebaseProjectId: String = "strokecalc-app"

    actual val firebaseApiKey: String = ""

    actual val isDebug: Boolean = false
}
