package com.vibely.domain.ordering

/**
 * Represents the lifecycle state of an order.
 *
 * Values match the database [order_status] type exactly.
 */
enum class OrderStatus {
    DRAFT,
    PENDING,
    PREPARING,
    READY,
    COMPLETED,
    CANCELLED,
}
