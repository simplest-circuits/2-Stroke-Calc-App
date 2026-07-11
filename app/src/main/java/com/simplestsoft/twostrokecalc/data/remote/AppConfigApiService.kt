package com.simplestsoft.twostrokecalc.data.remote

import com.simplestsoft.twostrokecalc.domain.model.remote.CalculatorAvailabilityResponse
import com.simplestsoft.twostrokecalc.domain.model.remote.DemoVehiclesConfigResponse
import com.simplestsoft.twostrokecalc.domain.model.remote.ProModulesResponse
import retrofit2.http.GET

interface AppConfigApiService {
    @GET("config/calculators")
    suspend fun getCalculatorAvailability(): CalculatorAvailabilityResponse

    @GET("config/pro-modules")
    suspend fun getProModules(): ProModulesResponse

    @GET("config/demo-vehicles")
    suspend fun getDemoVehiclesConfig(): DemoVehiclesConfigResponse
}
