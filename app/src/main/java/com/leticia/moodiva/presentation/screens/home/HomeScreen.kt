package com.leticia.moodiva.presentation.screens.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.leticia.moodiva.R
import com.leticia.moodiva.presentation.theme.MoodivaTheme

/** Abas da área principal. */
enum class HomeTab(val labelRes: Int) {
    CHAT(R.string.tab_chat),
    SAVED(R.string.tab_saved),
    PROFILE(R.string.tab_profile),
}

/**
 * Área principal com a barra inferior (planejamento, seção 9.1).
 *
 * A aba escolhida sobrevive à rotação (`rememberSaveable`). Cada aba é desenhada pelo
 * conteúdo recebido em [tabContent], que já traz seu próprio ViewModel.
 */
@Composable
fun HomeScreen(
    tabContent: @Composable (tab: HomeTab, selectTab: (HomeTab) -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selected by rememberSaveable { mutableStateOf(HomeTab.CHAT) }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                HomeTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = tab == selected,
                        onClick = { selected = tab },
                        icon = { TabIcon(tab) },
                        label = { Text(stringResource(tab.labelRes)) },
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .fillMaxSize(),
        ) {
            tabContent(selected) { selected = it }
        }
    }
}

@Composable
private fun TabIcon(tab: HomeTab) {
    // Decorativos: o rótulo da aba já está escrito embaixo.
    when (tab) {
        HomeTab.CHAT -> Icon(painterResource(R.drawable.ic_notification), contentDescription = null)
        HomeTab.SAVED -> Icon(Icons.Rounded.Favorite, contentDescription = null)
        HomeTab.PROFILE -> Icon(Icons.Rounded.Person, contentDescription = null)
    }
}

@PreviewLightDark
@Composable
private fun HomeScreenPreview() {
    MoodivaTheme {
        HomeScreen(tabContent = { tab, _ -> Text(stringResource(tab.labelRes)) })
    }
}
