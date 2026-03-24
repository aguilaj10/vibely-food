package com.vibely.domain.ordering

import com.vibely.domain.common.Money
import com.vibely.domain.common.Timestamp
import com.vibely.domain.customer.CustomerId
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class OrderTest {
    private val now = Timestamp(1_700_000_000_000L)

    @Test
    fun `Order can be constructed with all required fields`() {
        val modifier =
            SelectedModifier(
                modifierId = ModifierId("mod-1"),
                name = "Extra cheese",
                priceAdjustment = Money(150),
            )
        val item1 =
            OrderItem(
                menuItemId = MenuItemId("item-1"),
                menuItemName = "Classic Burger",
                quantity = 1,
                unitPrice = Money(1099),
                selectedModifiers = listOf(modifier),
                note = "No onions",
            )
        val item2 =
            OrderItem(
                menuItemId = MenuItemId("item-2"),
                menuItemName = "Fries",
                quantity = 2,
                unitPrice = Money(399),
                selectedModifiers = emptyList(),
                note = null,
            )
        val order =
            Order(
                id = OrderId("order-001"),
                tableId = TableId("table-5"),
                customerId = CustomerId("cust-001"),
                items = listOf(item1, item2),
                status = OrderStatus.DRAFT,
                createdAt = now,
                updatedAt = now,
                voidReason = null,
            )

        order.id.value shouldBe "order-001"
        order.items.size shouldBe 2
        order.status shouldBe OrderStatus.DRAFT
        order.voidReason.shouldBeNull()
        order.items[0].selectedModifiers[0].priceAdjustment shouldBe Money(150)
    }

    @Test
    fun `Order allows null customerId for anonymous order`() {
        val order =
            Order(
                id = OrderId("order-anon"),
                tableId = TableId("table-1"),
                customerId = null,
                items = emptyList(),
                status = OrderStatus.DRAFT,
                createdAt = now,
                updatedAt = now,
                voidReason = null,
            )
        order.customerId.shouldBeNull()
        order.items shouldBe emptyList()
    }

    @Test
    fun `Order with CANCELLED status can carry voidReason`() {
        val order =
            Order(
                id = OrderId("order-void"),
                tableId = TableId("table-2"),
                customerId = null,
                items = emptyList(),
                status = OrderStatus.CANCELLED,
                createdAt = now,
                updatedAt = now,
                voidReason = "Customer left",
            )
        order.status shouldBe OrderStatus.CANCELLED
        order.voidReason shouldBe "Customer left"
    }

    @Test
    fun `OrderStatus is exhaustively handleable without else`() {
        OrderStatus.entries.forEach { status ->
            val label =
                when (status) {
                    OrderStatus.DRAFT -> "draft"
                    OrderStatus.PENDING -> "pending"
                    OrderStatus.PREPARING -> "preparing"
                    OrderStatus.READY -> "ready"
                    OrderStatus.COMPLETED -> "completed"
                    OrderStatus.CANCELLED -> "cancelled"
                }
            label.isNotEmpty() shouldBe true
        }
    }

    @Test
    fun `SelectedModifier captures snapshot values`() {
        val snap =
            SelectedModifier(
                modifierId = ModifierId("mod-x"),
                name = "Jalapeños",
                priceAdjustment = Money(50),
            )
        snap.name shouldBe "Jalapeños"
        snap.priceAdjustment.cents shouldBe 50L
    }
}
