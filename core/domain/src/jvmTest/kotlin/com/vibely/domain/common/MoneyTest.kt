package com.vibely.domain.common

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class MoneyTest {
    @Test
    fun `Money_ZERO has cents == 0`() {
        Money.ZERO.cents shouldBe 0L
    }

    @Test
    fun `plus combines cents exactly`() {
        Money(1099) + Money(501) shouldBe Money(1600)
    }

    @Test
    fun `plus with zero is identity`() {
        Money(999) + Money.ZERO shouldBe Money(999)
    }

    @Test
    fun `minus subtracts cents exactly`() {
        Money(1600) - Money(501) shouldBe Money(1099)
    }

    @Test
    fun `minus to zero`() {
        Money(500) - Money(500) shouldBe Money.ZERO
    }

    @Test
    fun `times scales by factor`() {
        Money(333) * 3 shouldBe Money(999)
    }

    @Test
    fun `times by one is identity`() {
        Money(750) * 1 shouldBe Money(750)
    }

    @Test
    fun `times by zero yields ZERO`() {
        Money(750) * 0 shouldBe Money.ZERO
    }

    @Test
    fun `sum of three prices is exact`() {
        Money(100) + Money(200) + Money(300) shouldBe Money(600)
    }

    @Test
    fun `sum of ten cent amounts — classic floating-point trap`() {
        // 10 × 0.10 in floating point ≠ 1.00; in integer cents it must be exact
        (1..10).fold(Money.ZERO) { acc, _ -> acc + Money(10) } shouldBe Money(100)
    }

    @Test
    fun `large amounts fit in Long`() {
        Money(Long.MAX_VALUE / 2) + Money(1L) shouldBe Money(Long.MAX_VALUE / 2 + 1L)
    }
}
