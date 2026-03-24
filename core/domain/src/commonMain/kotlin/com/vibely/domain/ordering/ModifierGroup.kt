package com.vibely.domain.ordering

import kotlin.jvm.JvmInline

@JvmInline
value class ModifierGroupId(
    val value: String,
)

data class ModifierGroup(
    val id: ModifierGroupId,
    val name: String,
    val required: Boolean,
    val minSelections: Int,
    val maxSelections: Int,
    val options: List<Modifier>,
)
