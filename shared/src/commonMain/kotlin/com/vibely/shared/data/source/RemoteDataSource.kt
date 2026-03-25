package com.vibely.shared.data.source

/**
 * Contract for a remote network data source that operates on entities of type [T]
 * identified by [ID].
 *
 * Implementations are platform-specific (e.g., Ktor HTTP client).
 */
interface RemoteDataSource<T : Any, ID : Any> {
    /**
     * Fetches an entity by its [id] from the remote source.
     *
     * @return [Result.success] with the entity, [Result.failure] on network error or
     *         if the entity does not exist remotely.
     */
    suspend fun getById(id: ID): Result<T>

    /**
     * Saves (creates or updates) an entity on the remote source.
     *
     * @return [Result.success] with the saved entity as returned by the server,
     *         [Result.failure] on network or validation error.
     */
    suspend fun save(entity: T): Result<T>
}
