package com.vibely.domain.customer

import kotlin.jvm.JvmInline

@JvmInline
value class CustomerId(
    val value: String,
)

data class Customer(
    val id: CustomerId,
    val name: String,
    val phone: String?,
    val email: String?,
    val loyaltyPoints: Int,
)
