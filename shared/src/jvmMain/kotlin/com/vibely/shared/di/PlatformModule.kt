package com.vibely.shared.di

import org.koin.dsl.module

/** JVM [platformModule]: wires database configuration and factory into Koin. */
actual fun platformModule() =
    module {
        single {
            DatabaseConfig(
                url = System.getenv("DATABASE_URL") ?: "jdbc:postgresql://localhost:5432/vibely",
                username = System.getenv("DB_USER") ?: "postgres",
                password = System.getenv("DB_PASSWORD") ?: "postgres",
            )
        }
        single { DatabaseFactory(get()) }
    }
