package com.vibely.domain.ordering

/**
 * Defines the data access contract for [Table] aggregate roots.
 *
 * All operations are suspending to allow non-blocking execution.
 */
interface TableRepository {
    /**
     * Retrieves a [Table] by its unique identifier.
     *
     * @param id The [TableId] to look up.
     * @return [Result.success] containing the [Table] if found,
     *         or [Result.failure] if no table with the given [id] exists.
     */
    suspend fun findById(id: TableId): Result<Table>

    /**
     * Retrieves all tables regardless of status.
     *
     * @return A list of all [Table] records. Returns an empty list if none exist.
     */
    suspend fun findAll(): List<Table>

    /**
     * Retrieves all tables with the given [TableStatus].
     *
     * Useful for showing free tables during order placement or reserved tables
     * in the floor plan view.
     *
     * @param status The [TableStatus] to filter by (FREE, OCCUPIED, or RESERVED).
     * @return A list of [Table] records with the given status.
     *         Returns an empty list if no tables have that status.
     */
    suspend fun findByStatus(status: TableStatus): List<Table>

    /**
     * Retrieves all tables belonging to the given section.
     *
     * @param sectionId The [SectionId] to filter by.
     * @return A list of [Table] records in the given section.
     *         Returns an empty list if the section has no tables.
     */
    suspend fun findBySectionId(sectionId: SectionId): List<Table>

    /**
     * Persists a [Table], inserting it if it does not exist or updating it if it does.
     *
     * @param table The [Table] to save.
     * @return [Result.success] containing the saved [Table],
     *         or [Result.failure] if the operation could not be completed.
     */
    suspend fun save(table: Table): Result<Table>

    /**
     * Removes the [Table] with the given identifier.
     *
     * @param id The [TableId] of the table to delete.
     * @return [Result.success] with [Unit] if deleted successfully,
     *         or [Result.failure] if no table with the given [id] exists.
     */
    suspend fun deleteById(id: TableId): Result<Unit>
}
