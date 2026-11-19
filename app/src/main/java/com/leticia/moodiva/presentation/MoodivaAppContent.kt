package com.leticia.moodiva.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.leticia.moodiva.presentation.components.OfflineBanner
import com.leticia.moodiva.presentation.navigation.MoodivaNavHost
import com.leticia.moodiva.presentation.theme.MoodivaTheme
import org.koin.compose.viewmodel.koinViewModel

/** Raiz do app: liga o [MoodivaAppViewModel] à navegação. */
@Composable
fun MoodivaRoot(viewModel: MoodivaAppViewModel = koinViewModel()) {
    val startState by viewModel.startState.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val navController = rememberNavController()

    MoodivaAppContent(isOnline = isOnline) {
        MoodivaNavHost(navController = navController, startState = startState)
    }
}

/**
 * Estrutura sem estado da raiz: banner "sem internet" acima de todas as telas (RF22)
 * e a navegação ocupando o resto. Quando o banner aparece, ele ocupa a área da barra
 * de status; essa área é "consumida" para as telas abaixo não somarem o espaço de novo.
 */
@Composable
fun MoodivaAppContent(
    isOnline: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        AnimatedVisibility(visible = !isOnline) {
            OfflineBanner(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars))
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .then(if (isOnline) Modifier else Modifier.consumeWindowInsets(WindowInsets.statusBars)),
        ) {
            content()
        }
    }
}

@PreviewLightDark
@Composable
private fun MoodivaAppContentOfflinePreview() {
    MoodivaTheme {
        MoodivaAppContent(isOnline = false) { Text("Conteúdo") }
    }
}
