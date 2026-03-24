package com.vibely.server

import com.vibely.shared.di.commonModule
import com.vibely.shared.di.platformModule
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.koin.core.context.startKoin

/**
 * JVM server entry point.
 * Koin is started before the Ktor engine so all injection is available
 * to route handlers and repositories.
 */
fun main() {
    startKoin {
        modules(commonModule(), platformModule())
    }
    embeddedServer(Netty, port = 8080) {
        // Ktor configuration follows in Phase 1.2
    }.start(wait = true)
}
