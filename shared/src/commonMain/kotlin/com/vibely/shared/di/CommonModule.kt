package com.vibely.shared.di

import com.vibely.common.platform.PlatformLogger
import com.vibely.common.platform.SecureStorage
import com.vibely.shared.repository.UserPreferencesRepository
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Koin module containing platform-agnostic infrastructure bindings.
 *
 * Repository and use-case bindings are added here incrementally as each
 * feature is implemented. Do NOT add use cases or repository implementations
 * in advance — only wire infrastructure that exists right now.
 */
fun commonModule(): Module =
    module {
        single { UserPreferencesRepository() }
        single { SecureStorage() }
        single { PlatformLogger() }
    }
