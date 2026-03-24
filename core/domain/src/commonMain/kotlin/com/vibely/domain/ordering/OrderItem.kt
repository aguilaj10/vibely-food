package com.vibely.domain.ordering

import com.vibely.domain.common.Money

data class OrderItem(
    val menuItemId: MenuItemId,
    val menuItemName: String,
    val quantity: Int,
    val unitPrice: Money,
    val selectedModifiers: List<SelectedModifier>,
    val note: String?,
)
