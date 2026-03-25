package com.vibely.domain.tenant

import kotlin.jvm.JvmInline

/**
 * Strongly-typed identifier for an Organization (top-level tenant).
 *
 * Backed by a UUID string. UUID format is guaranteed by the authentication
 * layer and is not validated inside this class.
 */
@JvmInline
value class OrganizationId(
    val value: String
)
