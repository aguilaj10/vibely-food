package com.vibely.feature.auth.domain.model

import kotlin.time.Instant

/**
 * A valid authentication session with both access and refresh tokens.
 */
data class AuthToken(
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: Instant,
)
