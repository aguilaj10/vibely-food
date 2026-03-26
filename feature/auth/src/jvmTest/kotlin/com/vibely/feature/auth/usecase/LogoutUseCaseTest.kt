package com.vibely.feature.auth.usecase

import com.vibely.feature.auth.domain.auth.FakeAuthMode
import io.kotest.matchers.result.shouldBeFailure
import io.kotest.matchers.result.shouldBeSuccess
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class LogoutUseCaseTest {
    private val fake = FakeAuthMode()
    private val useCase = LogoutUseCase(fake)

    @Test
    fun `success — returns Unit from AuthMode`() =
        runTest {
            fake.logoutResult = Result.success(Unit)
            useCase("some-access-token").shouldBeSuccess() shouldBe Unit
        }

    @Test
    fun `failure — propagates failure from AuthMode`() =
        runTest {
            val error = RuntimeException("logout failed")
            fake.logoutResult = Result.failure(error)
            useCase("some-access-token").shouldBeFailure() shouldBe error
        }
}
