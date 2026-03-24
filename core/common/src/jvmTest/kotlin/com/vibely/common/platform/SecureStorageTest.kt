package com.vibely.common.platform

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class SecureStorageTest {
    private val storage = SecureStorage()

    @Test
    fun `save then get returns stored value`() {
        storage.save("key1", "value1")
        storage.get("key1") shouldBe "value1"
    }

    @Test
    fun `get returns null for missing key`() {
        storage.get("missing").shouldBeNull()
    }

    @Test
    fun `delete removes the entry`() {
        storage.save("key2", "value2")
        storage.delete("key2")
        storage.get("key2").shouldBeNull()
    }

    @Test
    fun `delete is no-op for absent key`() {
        storage.delete("nonexistent") // should not throw
    }

    @Test
    fun `clear removes all entries`() {
        storage.save("a", "1")
        storage.save("b", "2")
        storage.clear()
        storage.get("a").shouldBeNull()
        storage.get("b").shouldBeNull()
    }

    @Test
    fun `save overwrites existing entry`() {
        storage.save("k", "old")
        storage.save("k", "new")
        storage.get("k") shouldBe "new"
    }
}
