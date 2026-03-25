package com.vibely.domain.tenant

import kotlin.jvm.JvmInline

/**
 * Strongly-typed identifier for an authenticated User.
 *
 * Backed by a UUID string. Used in the audit log trigger via
 * `app.current_user_id`. UUID format is guaranteed by the authentication
 * layer and is not validated inside this class.
 */
@JvmInline
value class UserId(
    val value: String
)
