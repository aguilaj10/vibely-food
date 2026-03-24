package com.vibely.domain.inventory

import com.vibely.domain.common.Timestamp
import kotlin.jvm.JvmInline

@JvmInline
value class StockAlertId(
    val value: String,
)

data class StockAlert(
    val id: StockAlertId,
    val ingredientId: IngredientId,
    val quantityAtAlert: Double,
    val unit: UnitOfMeasure,
    val timestamp: Timestamp,
)
