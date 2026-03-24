package com.vibely.domain.customer

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class CustomerTest {
    @Test
    fun `Customer can be constructed with all fields`() {
        val customer =
            Customer(
                id = CustomerId("cust-001"),
                name = "Ana García",
                phone = "+34600000001",
                email = "ana@example.com",
                loyaltyPoints = 150,
            )
        customer.id.value shouldBe "cust-001"
        customer.name shouldBe "Ana García"
        customer.phone shouldBe "+34600000001"
        customer.email shouldBe "ana@example.com"
        customer.loyaltyPoints shouldBe 150
    }

    @Test
    fun `Customer phone and email can be null for anonymous walk-in`() {
        val walkIn =
            Customer(
                id = CustomerId("cust-anon"),
                name = "Walk-in",
                phone = null,
                email = null,
                loyaltyPoints = 0,
            )
        walkIn.phone.shouldBeNull()
        walkIn.email.shouldBeNull()
        walkIn.loyaltyPoints shouldBe 0
    }

    @Test
    fun `Customer equality is value-based`() {
        val c1 = Customer(CustomerId("x"), "Name", null, null, 0)
        val c2 = Customer(CustomerId("x"), "Name", null, null, 0)
        c1 shouldBe c2
    }

    @Test
    fun `CustomerId wraps a string`() {
        CustomerId("abc-123").value shouldBe "abc-123"
    }
}
