package com.vibely.feature.auth.usecase

import com.vibely.feature.auth.domain.auth.FakeAuthMode
import com.vibely.feature.auth.domain.model.AuthToken
import io.kotest.matchers.result.shouldBeFailure
import io.kotest.matchers.result.shouldBeSuccess
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.time.Clock

class RefreshTokenUseCaseTest {
    private val fake = FakeAuthMode()
    private val useCase = RefreshTokenUseCase(fake)

    @Test
    fun `success — returns refreshed token from AuthMode`() =
        runTest {
            val token = AuthToken("new-access", "new-refresh", Clock.System.now())
            fake.refreshTokenResult = Result.success(token)
            useCase("old-refresh-token").shouldBeSuccess() shouldBe token
        }

    @Test
    fun `failure — propagates failure from AuthMode`() =
        runTest {
            val error = RuntimeException("refresh failed")
            fake.refreshTokenResult = Result.failure(error)
            useCase("old-refresh-token").shouldBeFailure() shouldBe error
        }
}
