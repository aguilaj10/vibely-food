package com.vibely.domain.payment

import com.vibely.domain.common.Money
import com.vibely.domain.common.Timestamp
import com.vibely.domain.customer.CustomerId
import com.vibely.domain.ordering.OrderId
import com.vibely.domain.ordering.TableId

data class Receipt(
    val orderId: OrderId,
    val tableId: TableId,
    val customerId: CustomerId?,
    val lineItems: List<ReceiptLineItem>,
    val subtotal: Money,
    val taxAmount: Money,
    val total: Money,
    val payments: List<Payment>,
    val closedAt: Timestamp,
)
