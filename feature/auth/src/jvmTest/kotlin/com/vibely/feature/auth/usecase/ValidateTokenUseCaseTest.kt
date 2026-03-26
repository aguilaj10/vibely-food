package com.vibely.feature.auth.usecase

import com.vibely.domain.staff.Role
import com.vibely.domain.tenant.StoreId
import com.vibely.domain.tenant.UserId
import com.vibely.feature.auth.domain.auth.FakeAuthMode
import com.vibely.feature.auth.domain.model.User
import io.kotest.matchers.result.shouldBeFailure
import io.kotest.matchers.result.shouldBeSuccess
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class ValidateTokenUseCaseTest {
    private val fake = FakeAuthMode()
    private val useCase = ValidateTokenUseCase(fake)

    private val validUser =
        User(
            id = UserId("user-123"),
            email = "staff@vibely.com",
            role = Role.CASHIER,
            storeId = StoreId("store-456"),
        )

    @Test
    fun `success — returns user from AuthMode`() =
        runTest {
            fake.validateTokenResult = Result.success(validUser)
            useCase("valid-access-token").shouldBeSuccess() shouldBe validUser
        }

    @Test
    fun `failure — propagates failure from AuthMode`() =
        runTest {
            val error = RuntimeException("token expired")
            fake.validateTokenResult = Result.failure(error)
            useCase("expired-token").shouldBeFailure() shouldBe error
        }
}
