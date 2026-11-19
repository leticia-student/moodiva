package com.leticia.moodiva.presentation.navigation

import kotlinx.serialization.Serializable

// Rotas type-safe do Navigation Compose (planejamento, seção 9.1).
// Cada rota é uma classe serializável: o compilador confere os argumentos.
// Enums viajam como o nome (String), um tipo simples.

/** Abertura enquanto a sessão salva é lida. */
@Serializable
data object SplashRoute

/** Login e cadastro. */
@Serializable
data object AuthRoute

/** Onboarding. [mode] é o nome de `OnboardingMode`. */
@Serializable
data class OnboardingRoute(val mode: String)

/** Área principal com a barra inferior: Conversa, Salvas e Perfil. */
@Serializable
data object HomeRoute

/** Recomendações de um humor e modo. */
@Serializable
data class RecommendationsRoute(val mood: String, val mode: String)

/** Tela "Sobre". */
@Serializable
data object AboutRoute
