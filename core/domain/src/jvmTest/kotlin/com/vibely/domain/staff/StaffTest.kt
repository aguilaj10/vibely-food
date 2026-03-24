package com.vibely.domain.staff

import com.vibely.domain.common.Duration
import com.vibely.domain.common.Timestamp
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class StaffTest {
    private val clockInTime = Timestamp(1_700_000_000_000L)

    @Test
    fun `Employee can be constructed with role MANAGER`() {
        val employee =
            Employee(
                id = EmployeeId("emp-1"),
                name = "Ana García",
                pinHash = "\$2b\$12\$hashedValueHere",
                role = Role.MANAGER,
            )
        employee.role shouldBe Role.MANAGER
        employee.role.name shouldBe "MANAGER"
        employee.pinHash.isNotEmpty() shouldBe true
    }

    @Test
    fun `Active shift has null clockOut`() {
        val shift =
            Shift(
                id = ShiftId("shift-1"),
                employeeId = EmployeeId("emp-1"),
                clockIn = clockInTime,
                clockOut = null,
                breakDuration = Duration(0L),
            )
        shift.clockOut.shouldBeNull()
        shift.breakDuration.millis shouldBe 0L
    }

    @Test
    fun `Completed shift has non-null clockOut`() {
        val clockOutTime = Timestamp(1_700_003_600_000L)
        val shift =
            Shift(
                id = ShiftId("shift-2"),
                employeeId = EmployeeId("emp-2"),
                clockIn = clockInTime,
                clockOut = clockOutTime,
                breakDuration = Duration(900_000L),
            )
        shift.clockOut shouldBe clockOutTime
        shift.breakDuration.millis shouldBe 900_000L
    }

    @Test
    fun `Role is exhaustively handleable without else`() {
        Role.values().forEach { role ->
            val label =
                when (role) {
                    Role.OWNER -> "owner"
                    Role.MANAGER -> "manager"
                    Role.CASHIER -> "cashier"
                    Role.WAITER -> "waiter"
                    Role.KITCHEN -> "kitchen"
                    Role.VIEWER -> "viewer"
                }
            label.isNotEmpty() shouldBe true
        }
    }

    @Test
    fun `EmployeeId wraps a string`() {
        EmployeeId("emp-abc").value shouldBe "emp-abc"
    }
}
