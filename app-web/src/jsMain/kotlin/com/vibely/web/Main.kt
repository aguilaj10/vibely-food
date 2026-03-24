package com.vibely.web

import com.vibely.shared.di.commonModule
import com.vibely.shared.di.platformModule
import org.koin.core.context.startKoin

/**
 * Web/JS application entry point.
 * Initialises Koin before the UI is rendered.
 */
fun main() {
    startKoin {
        modules(commonModule(), platformModule())
    }
    // Compose for Web / UI initialisation follows in Phase 1.3
}
