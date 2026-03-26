package com.vibely.feature.auth.domain.model

import com.vibely.domain.staff.Role
import com.vibely.domain.tenant.StoreId
import com.vibely.domain.tenant.UserId

/**
 * Authenticated staff member identity returned by [ValidateTokenUseCase].
 */
data class User(
    val id: UserId,
    val email: String,
    val role: Role,
    val storeId: StoreId,
)
