package com.leticia.moodiva.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.leticia.moodiva.domain.model.ConversationMode
import com.leticia.moodiva.domain.model.Mood
import com.leticia.moodiva.presentation.AppStartState
import com.leticia.moodiva.presentation.components.MoodivaLogo
import com.leticia.moodiva.presentation.screens.home.HomeScreen
import com.leticia.moodiva.presentation.screens.home.HomeTab
import com.leticia.moodiva.presentation.screens.onboarding.OnboardingMode
import com.leticia.moodiva.presentation.screens.about.AboutRoute as AboutScreenRoute
import com.leticia.moodiva.presentation.screens.auth.AuthRoute as AuthScreenRoute
import com.leticia.moodiva.presentation.screens.chat.ChatRoute as ChatScreenRoute
import com.leticia.moodiva.presentation.screens.onboarding.OnboardingRoute as OnboardingScreenRoute
import com.leticia.moodiva.presentation.screens.profile.ProfileRoute as ProfileScreenRoute
import com.leticia.moodiva.presentation.screens.recommendations.RecommendationsRoute as RecommendationsScreenRoute
import com.leticia.moodiva.presentation.screens.saved.SavedRoute as SavedScreenRoute

// As telas também se chamam XRoute (padrão da skill android-compose-screen), como as rotas
// deste pacote. Os aliases deixam claro, aqui, quais são os composables das telas.

/**
 * Grafo de navegação do app (planejamento, seção 9.1).
 *
 * O destino raiz acompanha o [startState]: sem sessão → login; sem perfil → onboarding;
 * pronto → área principal. Ao trocar de raiz (login, logout, fim do onboarding), a pilha
 * inteira é limpa para o botão Voltar não levar a uma tela de outro estado.
 */
@Composable
fun MoodivaNavHost(
    navController: NavHostController,
    startState: AppStartState,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(startState) {
        navController.navigateToRootFor(startState)
    }

    NavHost(navController = navController, startDestination = SplashRoute, modifier = modifier) {
        composable<SplashRoute> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { MoodivaLogo(large = true) }
        }
        composable<AuthRoute> {
            AuthScreenRoute()
        }
        composable<OnboardingRoute> { entry ->
            val mode = OnboardingMode.valueOf(entry.toRoute<OnboardingRoute>().mode)
            OnboardingScreenRoute(mode = mode, onClose = { navController.popBackStack() })
        }
        composable<HomeRoute> {
            HomeScreen(
                tabContent = { tab, selectTab ->
                    when (tab) {
                        HomeTab.CHAT -> ChatScreenRoute(
                            onShowRecommendations = { mood, mode ->
                                navController.navigate(RecommendationsRoute(mood.name, mode.name))
                            },
                        )
                        HomeTab.SAVED -> SavedScreenRoute(onGoToChat = { selectTab(HomeTab.CHAT) })
                        HomeTab.PROFILE -> ProfileScreenRoute(
                            onRetakeTest = { navController.navigate(OnboardingRoute(OnboardingMode.RETAKE_TEST.name)) },
                            onEditTaste = { navController.navigate(OnboardingRoute(OnboardingMode.EDIT_TASTE.name)) },
                            onOpenAbout = { navController.navigate(AboutRoute) },
                        )
                    }
                },
            )
        }
        composable<RecommendationsRoute> { entry ->
            val route = entry.toRoute<RecommendationsRoute>()
            RecommendationsScreenRoute(
                mood = Mood.valueOf(route.mood),
                mode = ConversationMode.valueOf(route.mode),
                onBack = { navController.popBackStack() },
            )
        }
        composable<AboutRoute> {
            AboutScreenRoute(onBack = { navController.popBackStack() })
        }
    }
}

/**
 * Vai para a raiz do estado atual, a menos que já esteja nela. Isso evita, por exemplo,
 * que girar a tela nas recomendações volte para a conversa.
 */
private fun NavHostController.navigateToRootFor(state: AppStartState) {
    val current = currentBackStackEntry?.destination
    val alreadyThere = when (state) {
        AppStartState.LOADING -> return
        AppStartState.SIGNED_OUT -> current?.hasRoute(AuthRoute::class) == true
        AppStartState.NEEDS_ONBOARDING -> current?.hasRoute(OnboardingRoute::class) == true
        AppStartState.READY -> current != null &&
            !current.hasRoute(SplashRoute::class) &&
            !current.hasRoute(AuthRoute::class) &&
            !(current.hasRoute(OnboardingRoute::class) && previousBackStackEntry == null)
    }
    if (alreadyThere) return

    val target: Any = when (state) {
        AppStartState.SIGNED_OUT -> AuthRoute
        AppStartState.NEEDS_ONBOARDING -> OnboardingRoute(OnboardingMode.FULL.name)
        else -> HomeRoute
    }
    navigate(target) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}
