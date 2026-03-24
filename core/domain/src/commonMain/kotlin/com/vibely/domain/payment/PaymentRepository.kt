package com.vibely.domain.payment

import com.vibely.domain.ordering.OrderId

/**
 * Defines the data access contract for [Payment] aggregate roots.
 *
 * Payments are append-only records. Once persisted, a payment is never deleted.
 * All operations are suspending to allow non-blocking execution.
 */
interface PaymentRepository {
    /**
     * Retrieves a [Payment] by its unique identifier.
     *
     * @param id The [PaymentId] to look up.
     * @return [Result.success] containing the [Payment] if found,
     *         or [Result.failure] if no payment with the given [id] exists.
     */
    suspend fun findById(id: PaymentId): Result<Payment>

    /**
     * Retrieves all payments associated with the given order.
     *
     * An order may have multiple payments (e.g., split payment between cash and card).
     * Used to reconstruct how an order was settled when generating a receipt.
     *
     * @param orderId The [OrderId] to filter by.
     * @return A list of [Payment] records for the given order.
     *         Returns an empty list if the order has no payments.
     */
    suspend fun findByOrderId(orderId: OrderId): List<Payment>

    /**
     * Persists a [Payment]. Payments are append-only — this method only inserts;
     * updating an existing payment is not supported.
     *
     * @param payment The [Payment] to save.
     * @return [Result.success] containing the saved [Payment],
     *         or [Result.failure] if the operation could not be completed.
     */
    suspend fun save(payment: Payment): Result<Payment>
}
