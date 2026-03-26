package com.vibely.core.network.auth.dto

import kotlinx.serialization.Serializable

/** Token pair returned by login and refresh endpoints. */
@Serializable
data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: String, // ISO-8601 UTC timestamp — parsed to Instant by callers
)
