package com.vibely.domain.common

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class TimestampTest {
    @Test
    fun `Timestamp stores epochMillis`() {
        Timestamp(1_700_000_000_000L).epochMillis shouldBe 1_700_000_000_000L
    }

    @Test
    fun `two Timestamps with same millis are equal`() {
        Timestamp(42L) shouldBe Timestamp(42L)
    }
}
