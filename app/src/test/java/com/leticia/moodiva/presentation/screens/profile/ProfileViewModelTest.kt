package com.leticia.moodiva.presentation.screens.profile

import app.cash.turbine.test
import com.leticia.moodiva.domain.fake.FakeAuthRepository
import com.leticia.moodiva.domain.fake.FakeProfileRepository
import com.leticia.moodiva.domain.fake.TestData
import com.leticia.moodiva.domain.usecase.ObserveProfileUseCase
import com.leticia.moodiva.domain.usecase.ObserveSessionUseCase
import com.leticia.moodiva.domain.usecase.SignOutUseCase
import com.leticia.moodiva.presentation.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class) // advanceUntilIdle
class ProfileViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val auth = FakeAuthRepository()
    private val profiles = FakeProfileRepository().apply {
        profiles.value = mapOf(TestData.USER.id to TestData.profile())
    }

    private fun createViewModel() = ProfileViewModel(
        ObserveSessionUseCase(auth),
        ObserveProfileUseCase(auth, profiles),
        SignOutUseCase(auth),
    )

    @Test
    fun `deve mostrar o email e o perfil da conta`() = runTest {
        createViewModel().uiState.test {
            assertEquals(ProfileUiState.Loading, awaitItem())
            val state = awaitItem() as ProfileUiState.Success
            assertEquals("ana@mail.com", state.email)
            assertEquals(TestData.profile(), state.profile)
        }
    }

    @Test
    fun `sair deve encerrar a sessao`() = runTest {
        createViewModel().onSignOut()
        advanceUntilIdle()
        assertNull(auth.session.value)
    }
}
