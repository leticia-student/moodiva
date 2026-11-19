package com.leticia.moodiva.presentation.screens.onboarding

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

/** Para que o onboarding foi aberto (planejamento, seção 9.1). */
enum class OnboardingMode {
    /** Primeira vez: teste + gostos + resultado. */
    FULL,

    /** Pelo perfil: só o teste. */
    RETAKE_TEST,

    /** Pelo perfil: só os gostos. */
    EDIT_TASTE,
}

// Esqueleto temporário: substituído pela tela real na T11.
@Composable
fun OnboardingRoute(mode: OnboardingMode, onClose: () -> Unit) {
    Text("onboarding $mode")
}
