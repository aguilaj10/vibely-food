package com.vibely.domain.payment

import com.vibely.domain.common.Money
import com.vibely.domain.common.Timestamp
import com.vibely.domain.ordering.OrderId
import kotlin.jvm.JvmInline

@JvmInline
value class PaymentId(
    val value: String,
)

data class Payment(
    val id: PaymentId,
    val orderId: OrderId,
    val amount: Money,
    val method: PaymentMethod,
    val status: PaymentStatus,
    val timestamp: Timestamp,
)
