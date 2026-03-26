package com.vibely.core.network.auth.dto

import kotlinx.serialization.Serializable

/** Login request payload for POST /auth/login. */
@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
)
