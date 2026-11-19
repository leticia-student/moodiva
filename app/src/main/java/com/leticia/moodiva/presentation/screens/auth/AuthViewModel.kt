package com.leticia.moodiva.presentation.screens.auth

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leticia.moodiva.R
import com.leticia.moodiva.domain.model.CredentialProblem
import com.leticia.moodiva.domain.model.SignInResult
import com.leticia.moodiva.domain.model.SignUpResult
import com.leticia.moodiva.domain.usecase.SignInUseCase
import com.leticia.moodiva.domain.usecase.SignUpUseCase
import com.leticia.moodiva.presentation.util.ErrorMessageMapper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Estado do formulário de login/cadastro. Erros por campo, como `@StringRes`. */
@Immutable
data class AuthUiState(
    val isSignUp: Boolean = false,
    val email: String = "",
    val password: String = "",
    val confirmation: String = "",
    val isPasswordVisible: Boolean = false,
    @StringRes val emailError: Int? = null,
    @StringRes val passwordError: Int? = null,
    @StringRes val confirmationError: Int? = null,
    @StringRes val formError: Int? = null,
    val isLoading: Boolean = false,
)

/**
 * Login e cadastro locais (RF01–RF02).
 *
 * Em caso de sucesso, não navega: a sessão muda e a raiz do app (MoodivaAppViewModel)
 * leva o usuário ao onboarding ou à conversa.
 */
class AuthViewModel(
    private val signIn: SignInUseCase,
    private val signUp: SignUpUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    // Ao digitar, o erro do campo some: o usuário vê que está corrigindo.
    fun onEmailChange(value: String) = _uiState.update { it.copy(email = value, emailError = null, formError = null) }

    fun onPasswordChange(value: String) =
        _uiState.update { it.copy(password = value, passwordError = null, formError = null) }

    fun onConfirmationChange(value: String) =
        _uiState.update { it.copy(confirmation = value, confirmationError = null, formError = null) }

    fun onTogglePasswordVisibility() = _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }

    /** Alterna entre "Entrar" e "Criar conta", mantendo o e-mail digitado. */
    fun onToggleMode() = _uiState.update {
        AuthUiState(isSignUp = !it.isSignUp, email = it.email)
    }

    fun onSubmit() {
        val state = _uiState.value
        if (state.isLoading) return
        _uiState.update { it.copy(isLoading = true, formError = null) }
        viewModelScope.launch {
            if (state.isSignUp) handle(signUp(state.email, state.password, state.confirmation))
            else handle(signIn(state.email, state.password))
        }
    }

    private fun handle(result: SignUpResult) = when (result) {
        is SignUpResult.Success -> _uiState.update { it.copy(isLoading = false) }
        SignUpResult.EmailAlreadyUsed -> showFormError(R.string.auth_error_email_used)
        is SignUpResult.Invalid -> showProblems(result.problems)
        is SignUpResult.Failure -> showFormError(ErrorMessageMapper.messageFor(result.error))
    }

    private fun handle(result: SignInResult) = when (result) {
        is SignInResult.Success -> _uiState.update { it.copy(isLoading = false) }
        SignInResult.InvalidCredentials -> showFormError(R.string.auth_error_invalid_credentials)
        is SignInResult.Invalid -> showProblems(result.problems)
        is SignInResult.Failure -> showFormError(ErrorMessageMapper.messageFor(result.error))
    }

    private fun showFormError(@StringRes message: Int) =
        _uiState.update { it.copy(isLoading = false, formError = message) }

    private fun showProblems(problems: Set<CredentialProblem>) = _uiState.update {
        it.copy(
            isLoading = false,
            emailError = when {
                CredentialProblem.EMAIL_BLANK in problems -> R.string.auth_error_email_blank
                CredentialProblem.EMAIL_INVALID in problems -> R.string.auth_error_email_invalid
                else -> null
            },
            passwordError = when {
                CredentialProblem.PASSWORD_BLANK in problems -> R.string.auth_error_password_blank
                CredentialProblem.PASSWORD_TOO_SHORT in problems -> R.string.auth_error_password_short
                else -> null
            },
            confirmationError = if (CredentialProblem.CONFIRMATION_MISMATCH in problems) {
                R.string.auth_error_confirmation
            } else {
                null
            },
        )
    }
}
