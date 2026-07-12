package com.simplestsoft.twostrokecalc.di

import com.simplestsoft.twostrokecalc.data.auth.SharedAuthRepository
import com.simplestsoft.twostrokecalc.data.config.CalculatorAvailabilityRepository
import com.simplestsoft.twostrokecalc.data.config.DemoVehiclesConfigRepository
import com.simplestsoft.twostrokecalc.data.config.ProAccessRepository
import com.simplestsoft.twostrokecalc.data.preferences.AppPreferencesStore
import com.simplestsoft.twostrokecalc.data.session.UserSession
import com.simplestsoft.twostrokecalc.domain.model.remote.AccountApi
import com.simplestsoft.twostrokecalc.domain.model.remote.AdminApi
import com.simplestsoft.twostrokecalc.domain.model.remote.AppConfigApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import org.koin.core.context.GlobalContext

@Module
@InstallIn(SingletonComponent::class)
object SharedBridgeModule {

    @Provides
    @Singleton
    fun provideUserSession(): UserSession = GlobalContext.get().get()

    @Provides
    @Singleton
    fun provideAppPreferencesStore(): AppPreferencesStore = GlobalContext.get().get()

    @Provides
    @Singleton
    fun provideAppConfigApi(): AppConfigApi = GlobalContext.get().get()

    @Provides
    @Singleton
    fun provideAccountApi(): AccountApi = GlobalContext.get().get()

    @Provides
    @Singleton
    fun provideAdminApi(): AdminApi = GlobalContext.get().get()

    @Provides
    @Singleton
    fun provideSharedAuthRepository(): SharedAuthRepository = GlobalContext.get().get()

    @Provides
    @Singleton
    fun provideProAccessRepository(): ProAccessRepository = GlobalContext.get().get()

    @Provides
    @Singleton
    fun provideCalculatorAvailabilityRepository(): CalculatorAvailabilityRepository =
        GlobalContext.get().get()

    @Provides
    @Singleton
    fun provideDemoVehiclesConfigRepository(): DemoVehiclesConfigRepository =
        GlobalContext.get().get()
}
