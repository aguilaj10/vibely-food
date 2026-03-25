package com.vibely.shared.di

import com.vibely.database.DatabaseConfig
import com.vibely.database.DatabaseFactory
import org.koin.dsl.module

/** JVM [platformModule]: wires database configuration and factory into Koin. */
actual fun platformModule() =
    module {
        single<DatabaseConfig> { DatabaseConfig.fromEnvironment() }
        single<DatabaseFactory> { DatabaseFactory(config = get()) }
    }
