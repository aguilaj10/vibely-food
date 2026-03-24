package com.vibely.domain.ordering

import com.vibely.domain.common.Money
import kotlin.jvm.JvmInline

@JvmInline
value class ModifierId(
    val value: String,
)

data class Modifier(
    val id: ModifierId,
    val name: String,
    val priceAdjustment: Money,
    val available: Boolean,
)
