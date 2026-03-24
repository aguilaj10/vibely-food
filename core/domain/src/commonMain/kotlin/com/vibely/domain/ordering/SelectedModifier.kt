package com.vibely.domain.ordering

import com.vibely.domain.common.Money

data class SelectedModifier(
    val modifierId: ModifierId,
    val name: String,
    val priceAdjustment: Money,
)
