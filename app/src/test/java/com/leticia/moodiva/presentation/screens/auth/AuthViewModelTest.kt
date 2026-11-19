package com.leticia.moodiva.presentation.screens.auth

import com.leticia.moodiva.R
import com.leticia.moodiva.domain.fake.FakeAuthRepository
import com.leticia.moodiva.domain.usecase.SignInUseCase
import com.leticia.moodiva.domain.usecase.SignUpUseCase
import com.leticia.moodiva.presentation.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class) // advanceUntilIdle
class AuthViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val auth = FakeAuthRepository(initialUser = null)
    private val viewModel = AuthViewModel(SignInUseCase(auth), SignUpUseCase(auth))

    private fun fillSignUp(email: String, password: String, confirmation: String) {
        viewModel.onToggleMode()
        viewModel.onEmailChange(email)
        viewModel.onPasswordChange(password)
        viewModel.onConfirmationChange(confirmation)
    }

    @Test
    fun `cadastro valido deve abrir a sessao`() = runTest {
        fillSignUp("ana@mail.com", "123456", "123456")
        viewModel.onSubmit()
        assertTrue(viewModel.uiState.value.isLoading)

        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("ana@mail.com", auth.session.value?.email)
    }

    @Test
    fun `campos invalidos devem mostrar erro em cada campo`() = runTest {
        fillSignUp("ana", "123", "12")
        viewModel.onSubmit()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(R.string.auth_error_email_invalid, state.emailError)
        assertEquals(R.string.auth_error_password_short, state.passwordError)
        assertEquals(R.string.auth_error_confirmation, state.confirmationError)
        assertNull(auth.session.value)
    }

    @Test
    fun `digitar deve limpar o erro do campo`() = runTest {
        viewModel.onSubmit()
        advanceUntilIdle()
        assertEquals(R.string.auth_error_email_blank, viewModel.uiState.value.emailError)

        viewModel.onEmailChange("a")
        assertNull(viewModel.uiState.value.emailError)
    }

    @Test
    fun `login com senha errada deve mostrar erro geral`() = runTest {
        auth.signUp("ana@mail.com", "123456")
        auth.signOut()

        viewModel.onEmailChange("ana@mail.com")
        viewModel.onPasswordChange("000000")
        viewModel.onSubmit()
        advanceUntilIdle()

        assertEquals(R.string.auth_error_invalid_credentials, viewModel.uiState.value.formError)
        assertNull(auth.session.value)
    }

    @Test
    fun `email ja usado deve ser avisado`() = runTest {
        auth.signUp("ana@mail.com", "123456")
        fillSignUp("ana@mail.com", "123456", "123456")
        viewModel.onSubmit()
        advanceUntilIdle()

        assertEquals(R.string.auth_error_email_used, viewModel.uiState.value.formError)
    }

    @Test
    fun `trocar de modo deve manter o email e limpar o resto`() {
        viewModel.onEmailChange("ana@mail.com")
        viewModel.onPasswordChange("123")
        viewModel.onToggleMode()

        val state = viewModel.uiState.value
        assertTrue(state.isSignUp)
        assertEquals("ana@mail.com", state.email)
        assertEquals("", state.password)
    }
}
