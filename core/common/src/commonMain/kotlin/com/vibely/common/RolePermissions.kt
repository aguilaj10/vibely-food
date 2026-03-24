package com.vibely.common

import com.vibely.domain.staff.Role

/**
 * Maps each [Role] to the set of [Permission] values it holds.
 *
 * This mapping is authoritative and non-negotiable. Permission checks must use
 * [permissionsFor] or the [Role.hasPermission] extension rather than inspecting
 * this object directly.
 */
object RolePermissions {
    private val ALL = Permission.entries.toSet()

    private val mapping: Map<Role, Set<Permission>> =
        mapOf(
            Role.OWNER to ALL,
            Role.MANAGER to (ALL - Permission.MANAGE_USERS),
            Role.CASHIER to
                setOf(
                    Permission.CREATE_ORDER,
                    Permission.UPDATE_ORDER_STATUS,
                    Permission.PROCESS_PAYMENT,
                    Permission.VIEW_ORDERS,
                    Permission.VIEW_MENU,
                    Permission.VIEW_TABLES,
                ),
            Role.WAITER to
                setOf(
                    Permission.CREATE_ORDER,
                    Permission.VIEW_ORDERS,
                    Permission.VIEW_MENU,
                    Permission.VIEW_TABLES,
                ),
            Role.KITCHEN to
                setOf(
                    Permission.VIEW_ORDERS,
                    Permission.UPDATE_ORDER_STATUS,
                ),
            Role.VIEWER to
                setOf(
                    Permission.VIEW_ORDERS,
                    Permission.VIEW_MENU,
                    Permission.VIEW_TABLES,
                    Permission.VIEW_REPORTS,
                ),
        )

    /**
     * Returns the set of [Permission] values held by the given [role].
     *
     * @param role The [Role] to query.
     * @return The non-null, non-empty set of permissions for [role].
     */
    fun permissionsFor(role: Role): Set<Permission> = mapping.getValue(role)
}

/**
 * Returns `true` if this role holds the given [permission].
 *
 * @param permission The [Permission] to check.
 */
fun Role.hasPermission(permission: Permission): Boolean = RolePermissions.permissionsFor(this).contains(permission)
