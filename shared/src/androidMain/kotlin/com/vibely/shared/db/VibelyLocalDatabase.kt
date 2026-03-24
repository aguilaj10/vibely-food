package com.vibely.shared.db

import androidx.room.Database
import androidx.room.RoomDatabase

/** Room database for local Vibely storage. Full implementation in Phase 1.1. */
@Database(entities = [], version = 1)
abstract class VibelyLocalDatabase : RoomDatabase() {
    /** Returns the [PendingEventDao] for accessing the pending event queue. */
    abstract fun pendingEventDao(): PendingEventDao
}
