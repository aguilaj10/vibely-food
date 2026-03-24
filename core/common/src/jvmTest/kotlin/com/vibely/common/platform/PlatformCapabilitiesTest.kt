package com.vibely.common.platform

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class PlatformCapabilitiesTest {
    private val caps = PlatformCapabilities()

    @Test
    fun `JVM supportsLocalCache is false`() {
        caps.supportsLocalCache shouldBe false
    }

    @Test
    fun `JVM supportsBackgroundSync is true`() {
        caps.supportsBackgroundSync shouldBe true
    }

    @Test
    fun `JVM supportsNotifications is false`() {
        caps.supportsNotifications shouldBe false
    }
}
