package com.vibely.domain.payment

/**
 * Represents the payment instrument used to settle an order.
 *
 * Values match the database [payment_method_type] type exactly.
 * Split payments (orders settled with multiple instruments) are recorded
 * as separate [Payment] records, each with its own [PaymentMethod].
 */
enum class PaymentMethod {
    CASH,
    CARD,
    DIGITAL_WALLET,
    BANK_TRANSFER,
}
