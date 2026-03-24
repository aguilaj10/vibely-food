package com.vibely.domain.ordering

/**
 * Defines the data access contract for [MenuItem] aggregate roots.
 *
 * All operations are suspending to allow non-blocking execution.
 */
interface MenuItemRepository {
    /**
     * Retrieves a [MenuItem] by its unique identifier.
     *
     * @param id The [MenuItemId] to look up.
     * @return [Result.success] containing the [MenuItem] if found,
     *         or [Result.failure] if no item with the given [id] exists.
     */
    suspend fun findById(id: MenuItemId): Result<MenuItem>

    /**
     * Retrieves all menu items regardless of availability.
     *
     * @return A list of all [MenuItem] records. Returns an empty list if none exist.
     */
    suspend fun findAll(): List<MenuItem>

    /**
     * Retrieves all menu items where [MenuItem.available] is `true`.
     *
     * @return A list of available [MenuItem] records. Returns an empty list if none are available.
     */
    suspend fun findAvailable(): List<MenuItem>

    /**
     * Retrieves all menu items belonging to the given category.
     *
     * @param categoryId The [CategoryId] to filter by.
     * @return A list of [MenuItem] records in the given category.
     *         Returns an empty list if the category has no items.
     */
    suspend fun findByCategoryId(categoryId: CategoryId): List<MenuItem>

    /**
     * Persists a [MenuItem], inserting it if it does not exist or updating it if it does.
     *
     * @param menuItem The [MenuItem] to save.
     * @return [Result.success] containing the saved [MenuItem],
     *         or [Result.failure] if the operation could not be completed.
     */
    suspend fun save(menuItem: MenuItem): Result<MenuItem>

    /**
     * Removes the [MenuItem] with the given identifier.
     *
     * @param id The [MenuItemId] of the item to delete.
     * @return [Result.success] with [Unit] if deleted successfully,
     *         or [Result.failure] if no item with the given [id] exists.
     */
    suspend fun deleteById(id: MenuItemId): Result<Unit>
}
