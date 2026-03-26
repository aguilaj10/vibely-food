package com.vibely.feature.auth.domain.model

/**
 * User-supplied login credentials. Never persisted.
 */
data class Credentials(
    val email: String,
    val password: String,
)
