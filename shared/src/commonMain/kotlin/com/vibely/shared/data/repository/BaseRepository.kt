package com.vibely.shared.data.repository

import com.vibely.shared.data.source.LocalDataSource
import com.vibely.shared.data.source.RemoteDataSource
import com.vibely.shared.data.source.SyncManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onStart

/**
 * Abstract base class that implements a cache-first read strategy and a
 * write-through (online) / write-local-then-queue (offline) write strategy.
 *
 * Subclasses provide [localDataSource], [remoteDataSource], and [syncManager]
 * via constructor injection.
 *
 * **Read strategy** ([getById]):
 * 1. Returns the local result immediately if found.
 * 2. Falls back to the remote source on local miss, then caches the result locally.
 * 3. Returns [Result.failure] if both sources fail.
 *
 * **Write strategy** ([save]):
 * - Online: remote save -> local insert -> return remote result.
 * - Offline: local insert -> queue for sync -> return local result.
 *   Failure to queue is swallowed and must not propagate to the caller.
 *
 * **Nested transaction note**: If called from within an existing Exposed
 * transaction, the inner transaction call re-uses the outer transaction —
 * no nested transaction is created.
 *
 * @param T The domain entity type.
 * @param ID The identifier type for [T].
 */
abstract class BaseRepository<T : Any, ID : Any> {

    /** Local persistence data source. */
    protected abstract val localDataSource: LocalDataSource<T, ID>

    /** Remote network data source. */
    protected abstract val remoteDataSource: RemoteDataSource<T, ID>

    /** Sync state and offline queuing manager. */
    protected abstract val syncManager: SyncManager

    /**
     * Retrieves an entity by [id] using a cache-first strategy.
     *
     * Returns the local result immediately if found. Falls back to the remote
     * source on a local miss and caches the result.
     */
    suspend fun getById(id: ID): Result<T> {
        val local = localDataSource.getById(id)
        if (local.isSuccess) return local

        return remoteDataSource.getById(id).onSuccess { entity ->
            localDataSource.insert(entity)
        }
    }

    /**
     * Saves [entity] using a write-through strategy when online, or a
     * write-local-then-queue strategy when offline.
     *
     * Failure to queue for sync (offline path) is swallowed — it is logged
     * by the caller but must not surface as an error.
     */
    suspend fun save(entity: T): Result<T> {
        return if (syncManager.isOnline()) {
            remoteDataSource.save(entity).onSuccess { saved ->
                localDataSource.insert(saved)
            }
        } else {
            localDataSource.insert(entity).also { result ->
                result.onSuccess {
                    runCatching { syncManager.queueForSync(entity) }
                }
            }
        }
    }

    /**
     * Returns a [Flow] emitting the entity identified by [id] from the local store.
     *
     * Triggers [onSyncRequired] on first subscription to initiate a background
     * sync if needed.
     */
    fun observeById(id: ID): Flow<T> =
        localDataSource.observeById(id).onStart { onSyncRequired(id) }

    /**
     * Called when [observeById] begins collection. Subclasses may override to
     * trigger a background sync or refresh.
     *
     * Default implementation is a no-op — subclasses control the sync trigger rate.
     */
    protected open suspend fun onSyncRequired(id: ID) {
        // no-op by default — subclasses override to trigger background sync
    }
}
