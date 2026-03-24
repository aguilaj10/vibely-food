package com.vibely.domain.ordering

import com.vibely.domain.customer.CustomerId

/**
 * Defines the data access contract for [Order] aggregate roots.
 *
 * Orders are the central transaction records linking tables, customers,
 * items, and status. All operations are suspending to allow non-blocking execution.
 */
interface OrderRepository {
    /**
     * Retrieves an [Order] by its unique identifier.
     *
     * @param id The [OrderId] to look up.
     * @return [Result.success] containing the [Order] if found,
     *         or [Result.failure] if no order with the given [id] exists.
     */
    suspend fun findById(id: OrderId): Result<Order>

    /**
     * Retrieves all orders associated with a given table.
     *
     * A table may have multiple orders over time (e.g., re-opened after a void).
     *
     * @param tableId The [TableId] to filter by.
     * @return A list of [Order] records for the given table.
     *         Returns an empty list if the table has no orders.
     */
    suspend fun findByTableId(tableId: TableId): List<Order>

    /**
     * Retrieves all orders placed by a given customer.
     *
     * @param customerId The [CustomerId] to filter by.
     * @return A list of [Order] records for the given customer.
     *         Returns an empty list if the customer has no orders.
     */
    suspend fun findByCustomerId(customerId: CustomerId): List<Order>

    /**
     * Retrieves all orders with the given [OrderStatus].
     *
     * Used by the kitchen display to show OPEN and IN_PROGRESS orders,
     * and by the cashier to show orders ready for payment (DELIVERED).
     *
     * @param status The [OrderStatus] to filter by.
     * @return A list of [Order] records with the given status.
     *         Returns an empty list if no orders have that status.
     */
    suspend fun findByStatus(status: OrderStatus): List<Order>

    /**
     * Persists an [Order], inserting it if it does not exist or updating it if it does.
     *
     * @param order The [Order] to save.
     * @return [Result.success] containing the saved [Order],
     *         or [Result.failure] if the operation could not be completed.
     */
    suspend fun save(order: Order): Result<Order>

    /**
     * Removes the [Order] with the given identifier.
     *
     * @param id The [OrderId] of the order to delete.
     * @return [Result.success] with [Unit] if deleted successfully,
     *         or [Result.failure] if no order with the given [id] exists.
     */
    suspend fun deleteById(id: OrderId): Result<Unit>
}
