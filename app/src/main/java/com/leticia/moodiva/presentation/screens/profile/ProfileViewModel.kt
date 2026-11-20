package com.leticia.moodiva.presentation.screens.profile

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leticia.moodiva.domain.model.UserProfile
import com.leticia.moodiva.domain.usecase.ObserveProfileUseCase
import com.leticia.moodiva.domain.usecase.ObserveSessionUseCase
import com.leticia.moodiva.domain.usecase.SignOutUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data class Success(val email: String, val profile: UserProfile) : ProfileUiState
}

/** Perfil da conta logada (RF11, RF12) e logout (RF03). Tudo local: funciona offline. */
class ProfileViewModel(
    observeSession: ObserveSessionUseCase,
    observeProfile: ObserveProfileUseCase,
    private val signOut: SignOutUseCase,
) : ViewModel() {

    val uiState: StateFlow<ProfileUiState> = combine(observeSession(), observeProfile()) { user, profile ->
        if (user == null || profile == null) ProfileUiState.Loading else ProfileUiState.Success(user.email, profile)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ProfileUiState.Loading)

    /** A raiz do app percebe a sessão vazia e volta para o login. */
    fun onSignOut() {
        viewModelScope.launch { signOut() }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
