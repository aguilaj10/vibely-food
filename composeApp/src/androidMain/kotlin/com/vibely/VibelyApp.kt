package com.vibely

import android.app.Application
import com.vibely.common.platform.ApplicationContextHolder
import com.vibely.core.network.di.networkModule
import com.vibely.feature.auth.LoginViewModel
import com.vibely.feature.auth.di.authModule
import com.vibely.feature.auth.di.authPlatformModule
import com.vibely.shared.di.commonModule
import com.vibely.shared.di.platformModule
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.dsl.module

/**
 * Application entry point.
 * Initialises the application context holder and Koin with all required modules
 * before any Activity or Service is created.
 */
class VibelyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ApplicationContextHolder.init(this)
        startKoin {
            androidContext(this@VibelyApp)
            modules(
                commonModule(),
                platformModule(),
                networkModule(),
                authModule(),
                authPlatformModule(),
                module { viewModel { LoginViewModel(get()) } },
            )
        }
    }
}
