package com.vibely.domain.ordering

import kotlin.jvm.JvmInline

@JvmInline
value class CategoryId(
    val value: String,
)

data class Category(
    val id: CategoryId,
    val name: String,
    val displayOrder: Int,
    val available: Boolean,
)
