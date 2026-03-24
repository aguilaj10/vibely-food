package com.vibely.common.platform

/**
 * Web/JS implementation of [PlatformCapabilities].
 * The browser can persist data via storage APIs but cannot run background tasks.
 */
actual class PlatformCapabilities actual constructor() {
    /** True — the browser can persist data via localStorage and other storage APIs. */
    actual val supportsLocalCache: Boolean = true

    /** False — browsers do not support true background processing after the tab is hidden. */
    actual val supportsBackgroundSync: Boolean = false

    /** False — the web client does not request notification permissions at this stage. */
    actual val supportsNotifications: Boolean = false
}
