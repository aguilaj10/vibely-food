package com.vibely.domain.ordering

import com.vibely.domain.common.Timestamp
import com.vibely.domain.customer.CustomerId
import kotlin.jvm.JvmInline

@JvmInline
value class OrderId(
    val value: String,
)

data class Order(
    val id: OrderId,
    val tableId: TableId,
    val customerId: CustomerId?,
    val items: List<OrderItem>,
    val status: OrderStatus,
    val createdAt: Timestamp,
    val updatedAt: Timestamp,
    val voidReason: String?,
)
