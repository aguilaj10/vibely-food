package com.vibely.core.network.auth.dto

import kotlinx.serialization.Serializable

/** Token refresh request payload for POST /auth/refresh. */
@Serializable
data class RefreshRequest(
    val refreshToken: String,
)
