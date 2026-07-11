package com.simplestsoft.twostrokecalc.di

import com.simplestsoft.twostrokecalc.logging.DefaultErrorLogger
import com.simplestsoft.twostrokecalc.logging.ErrorLogger
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LoggingModule {
    @Binds
    @Singleton
    abstract fun bindErrorLogger(impl: DefaultErrorLogger): ErrorLogger
}
