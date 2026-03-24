package com.vibely.domain.ordering

/**
 * Defines the data access contract for [Section] aggregate roots.
 *
 * Sections represent physical areas of the restaurant that group tables.
 * All operations are suspending to allow non-blocking execution.
 */
interface SectionRepository {
    /**
     * Retrieves a [Section] by its unique identifier.
     *
     * @param id The [SectionId] to look up.
     * @return [Result.success] containing the [Section] if found,
     *         or [Result.failure] if no section with the given [id] exists.
     */
    suspend fun findById(id: SectionId): Result<Section>

    /**
     * Retrieves all sections.
     *
     * @return A list of all [Section] records. Returns an empty list if none exist.
     */
    suspend fun findAll(): List<Section>

    /**
     * Persists a [Section], inserting it if it does not exist or updating it if it does.
     *
     * @param section The [Section] to save.
     * @return [Result.success] containing the saved [Section],
     *         or [Result.failure] if the operation could not be completed.
     */
    suspend fun save(section: Section): Result<Section>

    /**
     * Removes the [Section] with the given identifier.
     *
     * @param id The [SectionId] of the section to delete.
     * @return [Result.success] with [Unit] if deleted successfully,
     *         or [Result.failure] if no section with the given [id] exists.
     */
    suspend fun deleteById(id: SectionId): Result<Unit>
}
