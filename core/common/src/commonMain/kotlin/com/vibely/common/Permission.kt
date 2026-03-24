package com.vibely.common

/**
 * Represents a discrete action a user may perform within the system.
 *
 * Permissions are assigned per [com.vibely.domain.staff.Role] via [RolePermissions].
 */
enum class Permission {
    /** Configure store name, currency, timezone, and operational settings. */
    MANAGE_STORE,

    /** Create, update, and deactivate user accounts. */
    MANAGE_USERS,

    /** Create, update, and remove menu items and categories. */
    MANAGE_MENU,

    /** Adjust stock levels and low-stock alert thresholds. */
    MANAGE_INVENTORY,

    /** Open new orders and add line items. */
    CREATE_ORDER,

    /** Advance an order through its lifecycle or cancel it. */
    UPDATE_ORDER_STATUS,

    /** Record and finalise payments for an order. */
    PROCESS_PAYMENT,

    /** Read order records. */
    VIEW_ORDERS,

    /** Read menu and category records. */
    VIEW_MENU,

    /** Read restaurant table and section layout. */
    VIEW_TABLES,

    /** Access sales and shift reports. */
    VIEW_REPORTS,
}
