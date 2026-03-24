package com.vibely.domain.staff

/**
 * Represents the role assigned to a user within a store.
 *
 * Values match the database [user_role] type exactly.
 */
enum class Role {
    OWNER,
    MANAGER,
    CASHIER,
    WAITER,
    KITCHEN,
    VIEWER,
}
