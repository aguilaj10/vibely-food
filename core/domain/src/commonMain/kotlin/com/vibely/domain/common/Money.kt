package com.vibely.domain.common

import kotlin.jvm.JvmInline

@JvmInline
value class Money(
    val cents: Long
) {
    operator fun plus(other: Money): Money = Money(cents + other.cents)

    operator fun minus(other: Money): Money = Money(cents - other.cents)

    operator fun times(factor: Int): Money = Money(cents * factor)

    companion object {
        val ZERO: Money = Money(0)
    }
}
