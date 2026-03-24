package com.vibely.common

/**
 * Placeholder constants for offline sync configuration.
 *
 * All values are `TODO()` placeholders. Actual values will be determined
 * when the sync layer is designed and load-tested.
 */
object SyncConstants {
    /** Interval (ms) between sync polling attempts when the device is online. Source: sync layer design. */
    val SYNC_INTERVAL_MS: Long
        get() = TODO("Determine from battery/data usage tradeoffs — typically 30_000L–60_000L")

    /** Maximum number of events that may queue locally before sync is forced. Source: sync layer design. */
    val MAX_PENDING_EVENTS: Int
        get() = TODO("Determine from offline usage patterns — typically 1_000")

    /** Number of events sent to the server in a single sync batch. Source: sync layer design. */
    val EVENT_BATCH_SIZE: Int
        get() = TODO("Determine from server throughput limits — typically 50–100")
}
