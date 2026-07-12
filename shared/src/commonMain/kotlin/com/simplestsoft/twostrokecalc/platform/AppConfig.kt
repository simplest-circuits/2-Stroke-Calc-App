package com.simplestsoft.twostrokecalc.platform

expect object AppConfig {
    val baseUrl: String
    val firebaseProjectId: String
    val firebaseApiKey: String
    val isDebug: Boolean
}
