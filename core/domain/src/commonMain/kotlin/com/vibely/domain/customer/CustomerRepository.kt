package com.vibely.domain.customer

/**
 * Defines the data access contract for [Customer] aggregate roots.
 *
 * All operations are suspending to allow non-blocking execution.
 */
interface CustomerRepository {
    /**
     * Retrieves a [Customer] by its unique identifier.
     *
     * @param id The [CustomerId] to look up.
     * @return [Result.success] containing the [Customer] if found,
     *         or [Result.failure] if no customer with the given [id] exists.
     */
    suspend fun findById(id: CustomerId): Result<Customer>

    /**
     * Retrieves a [Customer] by phone number.
     *
     * Used during order placement to look up returning customers.
     * The [phone] parameter should be a normalised phone string
     * (e.g., E.164 format: "+1234567890").
     *
     * @param phone The phone number string to look up.
     * @return [Result.success] containing the [Customer] if found,
     *         or [Result.failure] if no customer with the given phone exists.
     */
    suspend fun findByPhone(phone: String): Result<Customer>

    /**
     * Retrieves all customers.
     *
     * @return A list of all [Customer] records. Returns an empty list if none exist.
     */
    suspend fun findAll(): List<Customer>

    /**
     * Persists a [Customer], inserting it if it does not exist or updating it if it does.
     *
     * @param customer The [Customer] to save.
     * @return [Result.success] containing the saved [Customer],
     *         or [Result.failure] if the operation could not be completed.
     */
    suspend fun save(customer: Customer): Result<Customer>

    /**
     * Removes the [Customer] with the given identifier.
     *
     * @param id The [CustomerId] of the customer to delete.
     * @return [Result.success] with [Unit] if deleted successfully,
     *         or [Result.failure] if no customer with the given [id] exists.
     */
    suspend fun deleteById(id: CustomerId): Result<Unit>
}
