package com.vibely.core.network.auth.dto

import kotlinx.serialization.Serializable

/** Authenticated user payload returned by POST /auth/validate. */
@Serializable
data class ValidateResponse(
    val userId: String,
    val email: String,
    val role: String,
    val storeId: String,
)
