package com.leticia.moodiva.presentation.screens.about

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.ViewModel
import com.leticia.moodiva.R
import com.leticia.moodiva.presentation.components.SecondaryButton
import com.leticia.moodiva.presentation.components.MoodivaLogo
import com.leticia.moodiva.presentation.theme.Dimens
import com.leticia.moodiva.presentation.theme.MoodivaTheme
import com.leticia.moodiva.presentation.util.ExternalLinks
import org.koin.compose.viewmodel.koinViewModel

/** Só guarda a versão do app, lida do BuildConfig no `di` (o ViewModel não o conhece). */
class AboutViewModel(val appVersion: String) : ViewModel()

@Composable
fun AboutRoute(onBack: () -> Unit, viewModel: AboutViewModel = koinViewModel()) {
    val context = LocalContext.current
    AboutScreen(
        appVersion = viewModel.appVersion,
        onBack = onBack,
        onOpenLastFm = { ExternalLinks.open(context, Uri.parse(ExternalLinks.LASTFM_URL)) },
    )
}

/** Tela "Sobre" com as atribuições exigidas (RF25, RN37). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    appVersion: String,
    onBack: () -> Unit,
    onOpenLastFm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about_title), style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { innerPadding ->
        Box(Modifier.padding(innerPadding).fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    .widthIn(max = Dimens.ContentMaxWidth)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Dimens.ScreenHorizontalPadding, vertical = Dimens.MediumSpacing),
                verticalArrangement = Arrangement.spacedBy(Dimens.SectionSpacing),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    MoodivaLogo(large = true)
                    Text(
                        stringResource(R.string.about_version, appVersion),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(stringResource(R.string.about_description), style = MaterialTheme.typography.bodyLarge)

                Credit(R.string.about_lastfm_title, R.string.about_lastfm_text) {
                    // RN37: "powered by AudioScrobbler" com link para o Last.fm.
                    SecondaryButton(onClick = onOpenLastFm) { Text(stringResource(R.string.about_lastfm_button)) }
                }
                Credit(R.string.about_tipi_title, R.string.about_tipi_text)
                Credit(R.string.about_fonts_title, R.string.about_fonts_text)
                Credit(R.string.about_icons_title, R.string.about_icons_text)
            }
        }
    }
}

@Composable
private fun Credit(title: Int, text: Int, extra: @Composable () -> Unit = {}) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(Dimens.ItemSpacing), verticalArrangement = Arrangement.spacedBy(Dimens.MediumSpacing)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(text), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            extra()
        }
    }
}

@PreviewLightDark
@Composable
private fun AboutPreview() {
    MoodivaTheme {
        AboutScreen(appVersion = "1.0", onBack = {}, onOpenLastFm = {})
    }
}
