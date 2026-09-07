package com.simplestsoft.twostrokecalc.di

import com.simplestsoft.twostrokecalc.data.vehicles.SharedVehicleCatalogRepository
import com.simplestsoft.twostrokecalc.data.vehicles.SharedVehicleRepository
import com.simplestsoft.twostrokecalc.data.vehicles.SharedVehicleCatalogFirestoreSync
import com.simplestsoft.twostrokecalc.data.auth.SharedAuthRepository
import com.simplestsoft.twostrokecalc.data.community.CommunitySetupRepository
import com.simplestsoft.twostrokecalc.data.community.EngineCatalogFirestoreSync
import com.simplestsoft.twostrokecalc.data.community.SharedEngineCatalogRepository
import com.simplestsoft.twostrokecalc.data.config.CalculatorAvailabilityRepository
import com.simplestsoft.twostrokecalc.data.config.DemoVehiclesConfigRepository
import com.simplestsoft.twostrokecalc.data.config.ProAccessRepository
import com.simplestsoft.twostrokecalc.data.config.ToolAvailabilityRepository
import com.simplestsoft.twostrokecalc.data.preferences.AppPreferencesStore
import com.simplestsoft.twostrokecalc.data.settings.SharedUserSettingsFirestoreSync
import com.simplestsoft.twostrokecalc.data.tools.ToolSessionFirestoreSync
import com.simplestsoft.twostrokecalc.data.tools.ToolSessionRepository
import com.simplestsoft.twostrokecalc.data.remote.createAccountApi
import com.simplestsoft.twostrokecalc.data.remote.createAdminApi
import com.simplestsoft.twostrokecalc.data.remote.createApiHttpClient
import com.simplestsoft.twostrokecalc.data.remote.createAppConfigApi
import com.simplestsoft.twostrokecalc.data.session.UserSession
import com.simplestsoft.twostrokecalc.domain.model.remote.AccountApi
import com.simplestsoft.twostrokecalc.domain.model.remote.AdminApi
import com.simplestsoft.twostrokecalc.domain.model.remote.AppConfigApi
import org.koin.dsl.module

val sharedModule = module {
    single { UserSession() }
    single { AppPreferencesStore() }
    single { createApiHttpClient(get()) }
    single<AppConfigApi> { createAppConfigApi(get()) }
    single<AccountApi> { createAccountApi(get()) }
    single<AdminApi> { createAdminApi(get()) }
    single { SharedAuthRepository(get(), get(), get()) }
    single { CalculatorAvailabilityRepository(get(), get()) }
    single { ToolAvailabilityRepository(get(), get()) }
    single { ToolSessionRepository(get()) }
    single { ToolSessionFirestoreSync(get()) }
    single { DemoVehiclesConfigRepository(get(), get()) }
    single { ProAccessRepository(get(), get()) }
    single { SharedVehicleRepository(get(), get()) }
    single { SharedVehicleCatalogFirestoreSync(get()) }
    single { SharedVehicleCatalogRepository(get()) }
    single { EngineCatalogFirestoreSync(get()) }
    single { SharedEngineCatalogRepository(get()) }
    single { CommunitySetupRepository(get()) }
    single { SharedUserSettingsFirestoreSync(get()) }
}

fun sharedKoinModules() = listOf(sharedModule)
