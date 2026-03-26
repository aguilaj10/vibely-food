package com.vibely.feature.auth.di

import com.vibely.feature.auth.storage.EncryptedSharedPreferencesTokenStorage
import com.vibely.feature.auth.storage.TokenStorage
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun authPlatformModule(): Module =
    module {
        single<TokenStorage> { EncryptedSharedPreferencesTokenStorage(androidContext()) }
    }
