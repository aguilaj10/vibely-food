package com.vibely.domain.payment

import com.vibely.domain.common.Money
import com.vibely.domain.ordering.SelectedModifier

data class ReceiptLineItem(
    val name: String,
    val quantity: Int,
    val unitPrice: Money,
    val modifiers: List<SelectedModifier>,
)
