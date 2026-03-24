package com.vibely.common.platform

/**
 * Describes the runtime capabilities available on the current platform.
 * Shared code uses these flags to gate platform-specific behaviour.
 */
expect class PlatformCapabilities() {
    /** True if the platform can persist data to local storage. */
    val supportsLocalCache: Boolean

    /** True if the platform supports background work after the UI is hidden. */
    val supportsBackgroundSync: Boolean

    /** True if the platform can display push or local notifications. */
    val supportsNotifications: Boolean
}
