package com.vibely.core.network.di

import com.vibely.core.network.auth.AuthApiClient
import com.vibely.core.network.auth.KtorAuthApiClient
import com.vibely.core.network.buildHttpClient
import org.koin.dsl.module

/**
 * Koin module providing the shared [io.ktor.client.HttpClient] and [AuthApiClient].
 *
 * Register this in your application's Koin setup alongside other feature modules.
 */
fun networkModule() =
    module {
        single { buildHttpClient() }
        single<AuthApiClient> {
            KtorAuthApiClient(
                client = get(),
                baseUrl = "https://api.vibely.com", // TODO: inject from BuildConfig in future
            )
        }
    }
