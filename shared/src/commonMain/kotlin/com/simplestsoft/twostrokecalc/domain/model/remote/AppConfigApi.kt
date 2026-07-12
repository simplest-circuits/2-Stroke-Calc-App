package com.simplestsoft.twostrokecalc.domain.model.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

class AppConfigApi(
    private val client: HttpClient,
) {
    suspend fun getCalculatorAvailability(): CalculatorAvailabilityResponse =
        client.get("config/calculators").body()

    suspend fun getProModules(): ProModulesResponse =
        client.get("config/pro-modules").body()

    suspend fun getDemoVehiclesConfig(): DemoVehiclesConfigResponse =
        client.get("config/demo-vehicles").body()
}
