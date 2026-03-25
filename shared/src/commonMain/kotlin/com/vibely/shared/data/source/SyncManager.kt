package com.vibely.shared.data.source

/**
 * Manages network availability detection and offline-sync queuing.
 *
 * Platform implementations are registered via dependency injection.
 * Android uses ConnectivityManager; JVM server implementations can assume always-online
 * or use a network probe.
 *
 * This is a plain interface — not expect/actual — because the contract is identical
 * across platforms; only the implementation differs.
 */
interface SyncManager {

    /**
     * Returns `true` if the device currently has network connectivity.
     */
    fun isOnline(): Boolean

    /**
     * Queues [entity] for synchronisation when connectivity is restored.
     *
     * This is a fire-and-forget operation: failures to queue must be logged but
     * must NOT propagate as exceptions to the caller.
     */
    suspend fun queueForSync(entity: Any)
}
