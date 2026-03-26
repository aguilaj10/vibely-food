package com.vibely.feature.auth.di

import com.vibely.feature.auth.storage.Pkcs12KeystoreTokenStorage
import com.vibely.feature.auth.storage.TokenStorage
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun authPlatformModule(): Module =
    module {
        single<TokenStorage> { Pkcs12KeystoreTokenStorage() }
    }
