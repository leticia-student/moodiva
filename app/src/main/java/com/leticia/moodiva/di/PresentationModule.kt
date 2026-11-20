package com.leticia.moodiva.di

import com.leticia.moodiva.presentation.MoodivaAppViewModel
import com.leticia.moodiva.presentation.screens.auth.AuthViewModel
import com.leticia.moodiva.presentation.screens.chat.ChatViewModel
import com.leticia.moodiva.presentation.screens.onboarding.OnboardingViewModel
import com.leticia.moodiva.presentation.screens.recommendations.RecommendationsViewModel
import com.leticia.moodiva.presentation.screens.saved.SavedViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** ViewModels: o Koin cria cada um ligado ao ciclo de vida da tela que o pediu. */
val presentationModule = module {
    viewModelOf(::MoodivaAppViewModel)
    viewModelOf(::AuthViewModel)
    viewModelOf(::ChatViewModel)
    viewModelOf(::SavedViewModel)

    // O modo vem da rota de navegação, passado pela tela com parametersOf(mode).
    viewModel { params -> OnboardingViewModel(mode = params.get(), get(), get(), get()) }

    // Humor e modo vêm da rota de navegação, passados pela tela com parametersOf(mood, mode).
    viewModel { params ->
        RecommendationsViewModel(
            mood = params.get(),
            mode = params.get(),
            observeRecommendations = get(),
            observeSavedTracks = get(),
            refreshRecommendations = get(),
            loadCover = get(),
            saveTrack = get(),
            clock = get(),
        )
    }
}
