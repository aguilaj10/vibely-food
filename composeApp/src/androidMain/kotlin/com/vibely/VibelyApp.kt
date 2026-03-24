package com.vibely

import android.app.Application
import com.vibely.common.platform.ApplicationContextHolder
import com.vibely.shared.di.commonModule
import com.vibely.shared.di.platformModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

/**
 * Application entry point.
 * Initialises the application context holder and Koin with [commonModule] and
 * [platformModule] before any Activity or Service is created.
 */
class VibelyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ApplicationContextHolder.init(this)
        startKoin {
            androidContext(this@VibelyApp)
            modules(commonModule(), platformModule())
        }
    }
}
