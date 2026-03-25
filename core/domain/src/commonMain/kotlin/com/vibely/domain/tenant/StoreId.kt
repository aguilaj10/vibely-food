package com.vibely.domain.tenant

import kotlin.jvm.JvmInline

/**
 * Strongly-typed identifier for a Store (store-level isolation boundary).
 *
 * Backed by a UUID string. UUID format is guaranteed by the authentication
 * layer and is not validated inside this class.
 */
@JvmInline
value class StoreId(val value: String)
