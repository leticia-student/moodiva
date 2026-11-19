package com.leticia.moodiva.di

import com.leticia.moodiva.domain.usecase.CalculateNotificationDelayUseCase
import com.leticia.moodiva.domain.usecase.CalculatePersonalityUseCase
import com.leticia.moodiva.domain.usecase.GenerateRecommendationsUseCase
import com.leticia.moodiva.domain.usecase.LoadCoverUseCase
import com.leticia.moodiva.domain.usecase.MarkAsListenedUseCase
import com.leticia.moodiva.domain.usecase.ObserveConnectivityUseCase
import com.leticia.moodiva.domain.usecase.ObserveProfileUseCase
import com.leticia.moodiva.domain.usecase.ObserveRecommendationsUseCase
import com.leticia.moodiva.domain.usecase.ObserveSavedTracksUseCase
import com.leticia.moodiva.domain.usecase.ObserveSessionUseCase
import com.leticia.moodiva.domain.usecase.RateTrackUseCase
import com.leticia.moodiva.domain.usecase.RefreshRecommendationsUseCase
import com.leticia.moodiva.domain.usecase.RemoveSavedTrackUseCase
import com.leticia.moodiva.domain.usecase.SaveProfileUseCase
import com.leticia.moodiva.domain.usecase.SaveTrackUseCase
import com.leticia.moodiva.domain.usecase.SignInUseCase
import com.leticia.moodiva.domain.usecase.SignOutUseCase
import com.leticia.moodiva.domain.usecase.SignUpUseCase
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

/** Casos de uso: sem estado, então uma instância nova a cada pedido (`factory`). */
val domainModule = module {
    // Conta e infraestrutura
    factoryOf(::ObserveSessionUseCase)
    factoryOf(::SignUpUseCase)
    factoryOf(::SignInUseCase)
    factoryOf(::SignOutUseCase)
    factoryOf(::ObserveConnectivityUseCase)
    factoryOf(::CalculateNotificationDelayUseCase)

    // Perfil
    factoryOf(::ObserveProfileUseCase)
    factoryOf(::CalculatePersonalityUseCase)
    factoryOf(::SaveProfileUseCase)

    // Recomendações
    factoryOf(::GenerateRecommendationsUseCase)
    factoryOf(::RefreshRecommendationsUseCase)
    factoryOf(::ObserveRecommendationsUseCase)
    factoryOf(::LoadCoverUseCase)

    // Músicas salvas
    factoryOf(::ObserveSavedTracksUseCase)
    factoryOf(::SaveTrackUseCase)
    factoryOf(::MarkAsListenedUseCase)
    factoryOf(::RateTrackUseCase)
    factoryOf(::RemoveSavedTrackUseCase)
}
