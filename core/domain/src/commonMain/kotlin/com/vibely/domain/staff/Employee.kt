package com.vibely.domain.staff

import kotlin.jvm.JvmInline

@JvmInline
value class EmployeeId(
    val value: String,
)

data class Employee(
    val id: EmployeeId,
    val name: String,
    val pinHash: String,
    val role: Role,
)
