package com.vibely.shared.di

import androidx.room.Room
import com.vibely.shared.db.VibelyLocalDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/** Android [platformModule]: wires Room database, DataStore, and DAOs into Koin. */
actual fun platformModule() =
    module {
        single { androidContext().vibelyDataStore }
        single {
            Room
                .databaseBuilder(
                    androidContext(),
                    VibelyLocalDatabase::class.java,
                    "vibely_local.db",
                ).build()
        }
        single { get<VibelyLocalDatabase>().pendingEventDao() }
    }
