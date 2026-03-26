package com.vibely.feature.auth.usecase

import com.vibely.feature.auth.domain.auth.FakeAuthMode
import com.vibely.feature.auth.domain.model.AuthToken
import com.vibely.feature.auth.domain.model.Credentials
import io.kotest.matchers.result.shouldBeFailure
import io.kotest.matchers.result.shouldBeSuccess
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.time.Clock

class LoginUseCaseTest {
    private val fake = FakeAuthMode()
    private val useCase = LoginUseCase(fake)
    private val credentials = Credentials("staff@vibely.com", "secret")

    @Test
    fun `success — returns token from AuthMode`() =
        runTest {
            val token = AuthToken("access", "refresh", Clock.System.now())
            fake.authenticateResult = Result.success(token)
            useCase(credentials).shouldBeSuccess() shouldBe token
        }

    @Test
    fun `failure — propagates failure from AuthMode`() =
        runTest {
            val error = RuntimeException("invalid credentials")
            fake.authenticateResult = Result.failure(error)
            useCase(credentials).shouldBeFailure() shouldBe error
        }

    @Test
    fun `delegates — calls AuthMode exactly once`() =
        runTest {
            fake.authenticateResult = Result.success(AuthToken("a", "r", Clock.System.now()))
            useCase(credentials)
            fake.authenticateCallCount shouldBe 1
        }
}
