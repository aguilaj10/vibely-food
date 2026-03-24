package com.vibely.domain.common

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class DurationTest {
    @Test
    fun `Duration stores millis`() {
        Duration(3_600_000L).millis shouldBe 3_600_000L
    }

    @Test
    fun `Duration zero`() {
        Duration(0L).millis shouldBe 0L
    }
}
