package com.vibely.domain.staff

import com.vibely.domain.common.Duration
import com.vibely.domain.common.Timestamp
import kotlin.jvm.JvmInline

@JvmInline
value class ShiftId(
    val value: String,
)

data class Shift(
    val id: ShiftId,
    val employeeId: EmployeeId,
    val clockIn: Timestamp,
    val clockOut: Timestamp?,
    val breakDuration: Duration,
)
