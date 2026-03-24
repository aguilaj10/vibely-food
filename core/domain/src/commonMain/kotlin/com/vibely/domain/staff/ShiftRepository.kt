package com.vibely.domain.staff

/**
 * Defines the data access contract for [Shift] aggregate roots.
 *
 * All operations are suspending to allow non-blocking execution.
 */
interface ShiftRepository {
    /**
     * Retrieves a [Shift] by its unique identifier.
     *
     * @param id The [ShiftId] to look up.
     * @return [Result.success] containing the [Shift] if found,
     *         or [Result.failure] if no shift with the given [id] exists.
     */
    suspend fun findById(id: ShiftId): Result<Shift>

    /**
     * Retrieves all shifts for a given employee.
     *
     * @param employeeId The [EmployeeId] to filter by.
     * @return A list of [Shift] records for the given employee.
     *         Returns an empty list if the employee has no shifts.
     */
    suspend fun findByEmployeeId(employeeId: EmployeeId): List<Shift>

    /**
     * Retrieves all currently open shifts.
     *
     * An open shift is one where [Shift.clockOut] is `null`, indicating
     * the employee has clocked in but has not yet clocked out.
     *
     * @return A list of open [Shift] records. Returns an empty list if no shifts are open.
     */
    suspend fun findOpenShifts(): List<Shift>

    /**
     * Persists a [Shift], inserting it if it does not exist or updating it if it does.
     *
     * @param shift The [Shift] to save.
     * @return [Result.success] containing the saved [Shift],
     *         or [Result.failure] if the operation could not be completed.
     */
    suspend fun save(shift: Shift): Result<Shift>

    /**
     * Removes the [Shift] with the given identifier.
     *
     * @param id The [ShiftId] of the shift to delete.
     * @return [Result.success] with [Unit] if deleted successfully,
     *         or [Result.failure] if no shift with the given [id] exists.
     */
    suspend fun deleteById(id: ShiftId): Result<Unit>
}
