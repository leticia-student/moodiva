package com.leticia.moodiva.presentation

import app.cash.turbine.test
import com.leticia.moodiva.R
import com.leticia.moodiva.core.result.AppError
import com.leticia.moodiva.domain.fake.FakeAuthRepository
import com.leticia.moodiva.domain.fake.FakeNetworkMonitor
import com.leticia.moodiva.domain.fake.FakeProfileRepository
import com.leticia.moodiva.domain.fake.TestData
import com.leticia.moodiva.domain.usecase.ObserveConnectivityUseCase
import com.leticia.moodiva.domain.usecase.ObserveProfileUseCase
import com.leticia.moodiva.domain.usecase.ObserveSessionUseCase
import com.leticia.moodiva.presentation.util.ErrorMessageMapper
import com.leticia.moodiva.presentation.util.UpdatedAtFormatter
import com.leticia.moodiva.presentation.util.UpdatedAtText
import com.leticia.moodiva.presentation.util.formatDuration
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.Duration

class MoodivaAppViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val auth = FakeAuthRepository(initialUser = null)
    private val profiles = FakeProfileRepository()
    private val network = FakeNetworkMonitor(online = true)

    private fun createViewModel() = MoodivaAppViewModel(
        ObserveSessionUseCase(auth),
        ObserveProfileUseCase(auth, profiles),
        ObserveConnectivityUseCase(network),
    )

    @Test
    fun `sem sessao deve ir para o login`() = runTest {
        createViewModel().startState.test {
            assertEquals(AppStartState.LOADING, awaitItem())
            assertEquals(AppStartState.SIGNED_OUT, awaitItem())
        }
    }

    @Test
    fun `conta sem perfil deve ir para o onboarding e depois para a home`() = runTest {
        auth.session.value = TestData.USER
        createViewModel().startState.test {
            skipItems(1) // LOADING
            assertEquals(AppStartState.NEEDS_ONBOARDING, awaitItem())

            profiles.profiles.value = mapOf(TestData.USER.id to TestData.profile())
            assertEquals(AppStartState.READY, awaitItem())

            auth.session.value = null // logout
            assertEquals(AppStartState.SIGNED_OUT, awaitItem())
        }
    }

    @Test
    fun `conexao deve comecar online e refletir o monitor`() = runTest {
        createViewModel().isOnline.test {
            assertEquals(true, awaitItem())
            network.isOnline.value = false
            assertEquals(false, awaitItem())
        }
    }
}

class PresentationUtilsTest {

    private val now = TestData.NOW

    @Test
    fun `atualizado ha deve usar minutos e horas`() {
        assertEquals(UpdatedAtText.JustNow, UpdatedAtFormatter.format(now.minusSeconds(30), now))
        assertEquals(UpdatedAtText.MinutesAgo(5), UpdatedAtFormatter.format(now.minus(Duration.ofMinutes(5)), now))
        assertEquals(UpdatedAtText.HoursAgo(3), UpdatedAtFormatter.format(now.minus(Duration.ofHours(3)), now))
        assertEquals(UpdatedAtText.JustNow, UpdatedAtFormatter.format(now.plusSeconds(60), now))
    }

    @Test
    fun `erros devem virar mensagens amigaveis`() {
        assertEquals(R.string.error_network, ErrorMessageMapper.messageFor(AppError.Network))
        assertEquals(R.string.error_invalid_api_key, ErrorMessageMapper.messageFor(AppError.InvalidApiKey))
        assertEquals(R.string.error_rate_limited, ErrorMessageMapper.messageFor(AppError.RateLimited))
        assertEquals(R.string.error_server, ErrorMessageMapper.messageFor(AppError.Http(503)))
        assertEquals(R.string.error_generic, ErrorMessageMapper.messageFor(AppError.Http(404)))
    }

    @Test
    fun `duracao deve ser formatada em minutos e segundos`() {
        assertEquals("4:21", formatDuration(261))
        assertEquals("0:05", formatDuration(5))
    }
}
