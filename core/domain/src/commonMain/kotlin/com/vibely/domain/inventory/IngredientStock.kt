package com.vibely.domain.inventory

import kotlin.jvm.JvmInline

@JvmInline
value class IngredientId(
    val value: String,
)

data class IngredientStock(
    val id: IngredientId,
    val name: String,
    val quantity: Double,
    val unit: UnitOfMeasure,
    val alertThreshold: Double,
)
