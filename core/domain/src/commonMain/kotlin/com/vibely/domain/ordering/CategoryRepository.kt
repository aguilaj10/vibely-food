package com.vibely.domain.ordering

/**
 * Defines the data access contract for [Category] aggregate roots.
 *
 * All operations are suspending to allow non-blocking execution.
 * Implementations must not perform any work on the calling coroutine's thread directly.
 */
interface CategoryRepository {
    /**
     * Retrieves a [Category] by its unique identifier.
     *
     * @param id The [CategoryId] to look up.
     * @return [Result.success] containing the [Category] if found,
     *         or [Result.failure] if no category with the given [id] exists.
     */
    suspend fun findById(id: CategoryId): Result<Category>

    /**
     * Retrieves all categories regardless of availability.
     *
     * @return A list of all [Category] records. Returns an empty list if none exist.
     */
    suspend fun findAll(): List<Category>

    /**
     * Retrieves all categories where [Category.available] is `true`.
     *
     * @return A list of available [Category] records. Returns an empty list if none are available.
     */
    suspend fun findAvailable(): List<Category>

    /**
     * Persists a [Category], inserting it if it does not exist or updating it if it does.
     *
     * @param category The [Category] to save.
     * @return [Result.success] containing the saved [Category],
     *         or [Result.failure] if the operation could not be completed.
     */
    suspend fun save(category: Category): Result<Category>

    /**
     * Removes the [Category] with the given identifier.
     *
     * @param id The [CategoryId] of the category to delete.
     * @return [Result.success] with [Unit] if deleted successfully,
     *         or [Result.failure] if no category with the given [id] exists.
     */
    suspend fun deleteById(id: CategoryId): Result<Unit>
}
