package com.vibely.shared.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "vibely_prefs")

/** Returns the application [DataStore] instance backed by `vibely_prefs`. */
val Context.vibelyDataStore: DataStore<Preferences>
    get() = dataStore
