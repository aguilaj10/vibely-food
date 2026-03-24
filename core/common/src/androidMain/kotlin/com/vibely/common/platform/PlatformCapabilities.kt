package com.vibely.common.platform

/**
 * Android implementation of [PlatformCapabilities].
 * All capabilities are available on Android.
 */
actual class PlatformCapabilities actual constructor() {
    /** True — Android can persist data to local storage. */
    actual val supportsLocalCache: Boolean = true

    /** True — Android supports background work via WorkManager and services. */
    actual val supportsBackgroundSync: Boolean = true

    /** True — Android can display push and local notifications. */
    actual val supportsNotifications: Boolean = true
}
