package com.vibely.domain.ordering

import kotlin.jvm.JvmInline

@JvmInline
value class TableId(
    val value: String,
)

/**
 * Represents the occupancy state of a restaurant table.
 *
 * Values match the database [table_status] type exactly.
 */
enum class TableStatus {
    AVAILABLE,
    OCCUPIED,
    RESERVED,
    CLEANING,
}

data class Table(
    val id: TableId,
    val sectionId: SectionId,
    val capacity: Int,
    val status: TableStatus,
)
