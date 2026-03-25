package com.vibely.shared.data.source

import kotlinx.coroutines.flow.Flow

/**
 * Contract for a local persistence data source that stores entities of type [T]
 * identified by [ID].
 *
 * Implementations are platform-specific (e.g., Room on Android, Exposed on JVM).
 */
interface LocalDataSource<T : Any, ID : Any> {
    /**
     * Retrieves an entity by its [id].
     *
     * @return [Result.success] with the entity if found, [Result.failure] if not found
     *         or if a storage error occurs.
     */
    suspend fun getById(id: ID): Result<T>

    /**
     * Inserts or replaces an entity.
     *
     * @return [Result.success] with the persisted entity, [Result.failure] on storage error.
     */
    suspend fun insert(entity: T): Result<T>

    /**
     * Deletes the entity identified by [id].
     *
     * @return [Result.success] on deletion, [Result.failure] if the entity was not found
     *         or if a storage error occurs.
     */
    suspend fun delete(id: ID): Result<Unit>

    /**
     * Returns a [Flow] that emits the entity whenever it changes in the local store.
     *
     * The flow does not complete — it emits until cancelled.
     */
    fun observeById(id: ID): Flow<T>
}
