package com.leticia.moodiva.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leticia.moodiva.domain.usecase.ObserveConnectivityUseCase
import com.leticia.moodiva.domain.usecase.ObserveProfileUseCase
import com.leticia.moodiva.domain.usecase.ObserveSessionUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Para onde o app deve ir, conforme a sessão e o perfil (RF04, RN33). */
enum class AppStartState {
    /** Lendo a sessão salva: mostra a abertura com o nome Moodiva. */
    LOADING,

    /** Ninguém logado: login e cadastro. */
    SIGNED_OUT,

    /** Logado, mas sem perfil neste aparelho: onboarding. */
    NEEDS_ONBOARDING,

    /** Pronto: conversa, salvas e perfil. */
    READY,
}

/**
 * ViewModel da raiz do app: decide o destino inicial e informa a conexão (RF22).
 *
 * A conexão começa como online (`true`) para o banner não "piscar" na abertura.
 */
class MoodivaAppViewModel(
    observeSession: ObserveSessionUseCase,
    observeProfile: ObserveProfileUseCase,
    observeConnectivity: ObserveConnectivityUseCase,
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class) // flatMapLatest
    val startState: StateFlow<AppStartState> = observeSession()
        .flatMapLatest { user ->
            if (user == null) {
                flowOf(AppStartState.SIGNED_OUT)
            } else {
                observeProfile().map { profile ->
                    if (profile == null) AppStartState.NEEDS_ONBOARDING else AppStartState.READY
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), AppStartState.LOADING)

    val isOnline: StateFlow<Boolean> = observeConnectivity()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), true)

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
