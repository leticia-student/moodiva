package com.leticia.moodiva.di

import com.leticia.moodiva.presentation.MoodivaAppViewModel
import com.leticia.moodiva.presentation.screens.auth.AuthViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** ViewModels: o Koin cria cada um ligado ao ciclo de vida da tela que o pediu. */
val presentationModule = module {
    viewModelOf(::MoodivaAppViewModel)
    viewModelOf(::AuthViewModel)
}
