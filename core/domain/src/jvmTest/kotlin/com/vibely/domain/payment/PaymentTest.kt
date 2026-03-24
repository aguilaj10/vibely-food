package com.vibely.domain.payment

import com.vibely.domain.common.Money
import com.vibely.domain.common.Timestamp
import com.vibely.domain.customer.CustomerId
import com.vibely.domain.ordering.ModifierId
import com.vibely.domain.ordering.OrderId
import com.vibely.domain.ordering.SelectedModifier
import com.vibely.domain.ordering.TableId
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class PaymentTest {
    private val now = Timestamp(1_700_000_000_000L)

    @Test
    fun `Receipt sums payments equal to total`() {
        val payment1 =
            Payment(
                id = PaymentId("pay-1"),
                orderId = OrderId("order-1"),
                amount = Money(1500),
                method = PaymentMethod.CASH,
                status = PaymentStatus.COMPLETED,
                timestamp = now,
            )
        val payment2 =
            Payment(
                id = PaymentId("pay-2"),
                orderId = OrderId("order-1"),
                amount = Money(500),
                method = PaymentMethod.CARD,
                status = PaymentStatus.COMPLETED,
                timestamp = now,
            )
        val lineItem =
            ReceiptLineItem(
                name = "Classic Burger",
                quantity = 1,
                unitPrice = Money(1099),
                modifiers = listOf(SelectedModifier(ModifierId("mod-1"), "Extra cheese", Money(150))),
            )
        val receipt =
            Receipt(
                orderId = OrderId("order-1"),
                tableId = TableId("table-3"),
                customerId = CustomerId("cust-1"),
                lineItems = listOf(lineItem),
                subtotal = Money(1800),
                taxAmount = Money(200),
                total = Money(2000),
                payments = listOf(payment1, payment2),
                closedAt = now,
            )

        receipt.payments.sumOf { it.amount.cents } shouldBe receipt.total.cents
        receipt.lineItems.size shouldBe 1
    }

    @Test
    fun `Receipt allows null customerId for anonymous order`() {
        val receipt =
            Receipt(
                orderId = OrderId("order-anon"),
                tableId = TableId("table-1"),
                customerId = null,
                lineItems = emptyList(),
                subtotal = Money.ZERO,
                taxAmount = Money.ZERO,
                total = Money.ZERO,
                payments = emptyList(),
                closedAt = now,
            )
        receipt.customerId shouldBe null
    }

    @Test
    fun `PaymentMethod is exhaustively handleable without else`() {
        PaymentMethod.entries.forEach { method ->
            val label =
                when (method) {
                    PaymentMethod.CASH -> "cash"
                    PaymentMethod.CARD -> "card"
                    PaymentMethod.DIGITAL_WALLET -> "digital wallet"
                    PaymentMethod.BANK_TRANSFER -> "bank transfer"
                }
            label.isNotEmpty() shouldBe true
        }
    }

    @Test
    fun `PaymentStatus is exhaustively handleable without else`() {
        PaymentStatus.entries.forEach { status ->
            val label =
                when (status) {
                    PaymentStatus.PENDING -> "pending"
                    PaymentStatus.COMPLETED -> "completed"
                    PaymentStatus.FAILED -> "failed"
                    PaymentStatus.REFUNDED -> "refunded"
                }
            label.isNotEmpty() shouldBe true
        }
    }

    @Test
    fun `Payment amount can be zero for bank transfer order`() {
        val comped =
            Payment(
                id = PaymentId("pay-comp"),
                orderId = OrderId("order-comp"),
                amount = Money.ZERO,
                method = PaymentMethod.BANK_TRANSFER,
                status = PaymentStatus.COMPLETED,
                timestamp = now,
            )
        comped.amount shouldBe Money.ZERO
    }
}
