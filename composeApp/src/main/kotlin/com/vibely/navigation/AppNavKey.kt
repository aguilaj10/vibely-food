package com.vibely.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Type-safe navigation destination identities for Vibely POS.
 * Each object represents one screen in the app's back stack.
 */
@Serializable
sealed interface AppNavKey : NavKey {

    /** The login form — shown when the user has no valid session. */
    @Serializable
    data object Login : AppNavKey

    /**
     * The table floor plan — the primary post-login destination.
     * The actual content is implemented in Phase 2.
     */
    @Serializable
    data object FloorPlan : AppNavKey
}
