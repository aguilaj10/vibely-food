package com.vibely.domain.ordering

import com.vibely.domain.common.Money
import kotlin.jvm.JvmInline

@JvmInline
value class MenuItemId(
    val value: String,
)

data class MenuItem(
    val id: MenuItemId,
    val name: String,
    val description: String,
    val basePrice: Money,
    val available: Boolean,
    val categoryId: CategoryId,
    val modifierGroups: List<ModifierGroup>,
)
