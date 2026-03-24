package com.vibely.domain.ordering

import kotlin.jvm.JvmInline

@JvmInline
value class TableId(
    val value: String,
)

enum class TableStatus {
    FREE,
    OCCUPIED,
    RESERVED,
}

data class Table(
    val id: TableId,
    val sectionId: SectionId,
    val capacity: Int,
    val status: TableStatus,
)
