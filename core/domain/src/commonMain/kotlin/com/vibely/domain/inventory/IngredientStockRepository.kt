package com.vibely.domain.inventory

/**
 * Defines the data access contract for [IngredientStock] aggregate roots.
 *
 * All operations are suspending to allow non-blocking execution.
 */
interface IngredientStockRepository {
    /**
     * Retrieves an [IngredientStock] by its unique identifier.
     *
     * @param id The [IngredientId] to look up.
     * @return [Result.success] containing the [IngredientStock] if found,
     *         or [Result.failure] if no ingredient with the given [id] exists.
     */
    suspend fun findById(id: IngredientId): Result<IngredientStock>

    /**
     * Retrieves all ingredient stock records.
     *
     * @return A list of all [IngredientStock] records. Returns an empty list if none exist.
     */
    suspend fun findAll(): List<IngredientStock>

    /**
     * Retrieves all ingredient stock records where current quantity is below the alert threshold.
     *
     * The condition is: [IngredientStock.quantity] < [IngredientStock.alertThreshold].
     * Used by the kitchen manager's low-stock alert screen.
     *
     * @return A list of [IngredientStock] records that are below threshold.
     *         Returns an empty list if all stock levels are sufficient.
     */
    suspend fun findBelowThreshold(): List<IngredientStock>

    /**
     * Persists an [IngredientStock] record, inserting it if it does not exist or updating it if it does.
     *
     * @param stock The [IngredientStock] to save.
     * @return [Result.success] containing the saved [IngredientStock],
     *         or [Result.failure] if the operation could not be completed.
     */
    suspend fun save(stock: IngredientStock): Result<IngredientStock>

    /**
     * Removes the [IngredientStock] record with the given identifier.
     *
     * @param id The [IngredientId] of the record to delete.
     * @return [Result.success] with [Unit] if deleted successfully,
     *         or [Result.failure] if no ingredient with the given [id] exists.
     */
    suspend fun deleteById(id: IngredientId): Result<Unit>
}
