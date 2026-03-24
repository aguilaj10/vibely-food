package com.vibely.domain.ordering

import kotlin.jvm.JvmInline

@JvmInline
value class SectionId(
    val value: String,
)

data class Section(
    val id: SectionId,
    val name: String,
    val tables: List<TableId>,
)
