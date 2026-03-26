package com.vibely.feature.auth.di

import com.vibely.feature.auth.BuildKonfig
import com.vibely.feature.auth.domain.auth.AuthMode
import com.vibely.feature.auth.domain.auth.DebugAuthMode
import com.vibely.feature.auth.domain.auth.FakeAuthMode
import com.vibely.feature.auth.domain.auth.ProductionAuthMode
import com.vibely.feature.auth.usecase.LoginUseCase
import com.vibely.feature.auth.usecase.LogoutUseCase
import com.vibely.feature.auth.usecase.RefreshTokenUseCase
import com.vibely.feature.auth.usecase.ValidateTokenUseCase
import org.koin.dsl.module

/**
 * Core auth domain Koin module.
 * Reads [BuildKonfig.AUTH_MODE] to select the [AuthMode] implementation.
 * Register alongside [authPlatformModule] and `networkModule`.
 *
 * This is the **only** place in the codebase that reads [BuildKonfig.AUTH_MODE].
 */
fun authModule() =
    module {
        single<AuthMode> {
            when (BuildKonfig.AUTH_MODE) {
                "debug" -> DebugAuthMode()
                "fake" -> FakeAuthMode()
                else -> ProductionAuthMode(apiClient = get(), storage = get())
            }
        }
        factory { LoginUseCase(get()) }
        factory { LogoutUseCase(get()) }
        factory { RefreshTokenUseCase(get()) }
        factory { ValidateTokenUseCase(get()) }
    }
