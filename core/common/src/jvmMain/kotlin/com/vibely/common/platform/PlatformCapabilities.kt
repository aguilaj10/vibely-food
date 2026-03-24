package com.vibely.common.platform

/**
 * JVM server implementation of [PlatformCapabilities].
 * The JVM target does not persist data locally and cannot send notifications.
 */
actual class PlatformCapabilities actual constructor() {
    /** False — the JVM server target does not use local client-side storage. */
    actual val supportsLocalCache: Boolean = false

    /** True — the JVM target supports background processing and scheduled tasks. */
    actual val supportsBackgroundSync: Boolean = true

    /** False — the JVM server target does not send push or local notifications. */
    actual val supportsNotifications: Boolean = false
}
