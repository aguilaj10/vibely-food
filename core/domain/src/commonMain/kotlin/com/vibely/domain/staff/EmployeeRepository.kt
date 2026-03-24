package com.vibely.domain.staff

/**
 * Defines the data access contract for [Employee] aggregate roots.
 *
 * All operations are suspending to allow non-blocking execution.
 */
interface EmployeeRepository {
    /**
     * Retrieves an [Employee] by its unique identifier.
     *
     * @param id The [EmployeeId] to look up.
     * @return [Result.success] containing the [Employee] if found,
     *         or [Result.failure] if no employee with the given [id] exists.
     */
    suspend fun findById(id: EmployeeId): Result<Employee>

    /**
     * Retrieves an [Employee] by their PIN hash.
     *
     * **The [pinHash] parameter must be a pre-hashed value — never a raw PIN.**
     * The caller is responsible for hashing the PIN before invoking this method.
     * Used during clock-in authentication to identify the employee without
     * storing or transmitting the raw PIN.
     *
     * @param pinHash The hashed PIN string to look up.
     * @return [Result.success] containing the matching [Employee] if found,
     *         or [Result.failure] if no employee with the given hash exists.
     */
    suspend fun findByPinHash(pinHash: String): Result<Employee>

    /**
     * Retrieves all employees.
     *
     * @return A list of all [Employee] records. Returns an empty list if none exist.
     */
    suspend fun findAll(): List<Employee>

    /**
     * Retrieves all employees with the given [Role].
     *
     * @param role The [Role] to filter by.
     * @return A list of [Employee] records with the given role.
     *         Returns an empty list if no employees have that role.
     */
    suspend fun findByRole(role: Role): List<Employee>

    /**
     * Persists an [Employee], inserting it if it does not exist or updating it if it does.
     *
     * @param employee The [Employee] to save.
     * @return [Result.success] containing the saved [Employee],
     *         or [Result.failure] if the operation could not be completed.
     */
    suspend fun save(employee: Employee): Result<Employee>

    /**
     * Removes the [Employee] with the given identifier.
     *
     * @param id The [EmployeeId] of the employee to delete.
     * @return [Result.success] with [Unit] if deleted successfully,
     *         or [Result.failure] if no employee with the given [id] exists.
     */
    suspend fun deleteById(id: EmployeeId): Result<Unit>
}
