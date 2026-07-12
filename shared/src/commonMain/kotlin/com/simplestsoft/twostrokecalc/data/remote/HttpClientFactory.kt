package com.simplestsoft.twostrokecalc.data.remote

import com.simplestsoft.twostrokecalc.data.session.UserSession
import com.simplestsoft.twostrokecalc.platform.AppConfig
import com.simplestsoft.twostrokecalc.platform.currentAuthUserId
import com.simplestsoft.twostrokecalc.platform.fetchAuthIdToken
import io.ktor.client.HttpClient
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

fun createApiHttpClient(userSession: UserSession): HttpClient {
    val authPlugin = createClientPlugin("AuthHeaderPlugin") {
        onRequest { request, _ ->
            val userId = currentAuthUserId() ?: userSession.getUserId()
            if (userId != null) {
                request.headers.append("X-User-Id", userId)
                fetchAuthIdToken(forceRefresh = false)?.let { token ->
                    request.headers.append(HttpHeaders.Authorization, "Bearer $token")
                }
            }
        }
    }

    return HttpClient {
        expectSuccess = false
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    encodeDefaults = true
                },
            )
        }
        if (AppConfig.isDebug) {
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        println("Ktor: $message")
                    }
                }
                level = LogLevel.INFO
                sanitizeHeader { header -> header == HttpHeaders.Authorization }
            }
        }
        defaultRequest {
            url(AppConfig.baseUrl)
            header(HttpHeaders.ContentType, "application/json")
        }
        install(authPlugin)
    }
}

fun createAppConfigApi(httpClient: HttpClient): com.simplestsoft.twostrokecalc.domain.model.remote.AppConfigApi =
    com.simplestsoft.twostrokecalc.domain.model.remote.AppConfigApi(httpClient)

fun createAccountApi(httpClient: HttpClient): com.simplestsoft.twostrokecalc.domain.model.remote.AccountApi =
    com.simplestsoft.twostrokecalc.domain.model.remote.AccountApi(httpClient)

fun createAdminApi(httpClient: HttpClient): com.simplestsoft.twostrokecalc.domain.model.remote.AdminApi =
    com.simplestsoft.twostrokecalc.domain.model.remote.AdminApi(httpClient)
