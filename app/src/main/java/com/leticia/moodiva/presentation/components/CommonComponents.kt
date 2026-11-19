package com.leticia.moodiva.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.leticia.moodiva.R
import com.leticia.moodiva.presentation.theme.Dimens
import com.leticia.moodiva.presentation.theme.MoodivaTheme

// Estados padrão das telas (identidade visual, "Estados das telas").

/** Carregando: indicador circular centralizado, em `primary`. */
@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

/** Vazio: frase curta explicando o motivo e, opcionalmente, uma ação. */
@Composable
fun EmptyState(
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Dimens.ScreenHorizontalPadding, vertical = Dimens.SectionSpacing),
        verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_music_note),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            Button(onClick = onAction) { Text(actionLabel) }
        }
    }
}

/** Erro: o que aconteceu e como resolver, em `errorContainer`, com "Tentar novamente". */
@Composable
fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Dimens.ScreenHorizontalPadding),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(Dimens.SectionSpacing),
                verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(text = message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                Button(onClick = onRetry) { Text(stringResource(R.string.action_retry)) }
            }
        }
    }
}

/** Banner "sem internet" (RF22), em `tertiaryContainer`. */
@Composable
fun OfflineBanner(modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Dimens.ScreenHorizontalPadding, vertical = Dimens.MediumSpacing),
            horizontalArrangement = Arrangement.spacedBy(Dimens.MediumSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Decorativo: o texto ao lado já informa a situação.
            Icon(
                painter = painterResource(R.drawable.ic_cloud_off),
                contentDescription = null,
                modifier = Modifier.size(Dimens.IconSmall),
            )
            Text(text = stringResource(R.string.offline_banner), style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** Nome "Moodiva" na fonte manuscrita, só em `displayLarge`/`displayMedium` (identidade visual, regra 4). */
@Composable
fun MoodivaLogo(modifier: Modifier = Modifier, large: Boolean = false) {
    Text(
        text = stringResource(R.string.app_name),
        style = if (large) MaterialTheme.typography.displayLarge else MaterialTheme.typography.displayMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier,
    )
}

@PreviewLightDark
@Composable
private fun StatesPreview() {
    MoodivaTheme {
        Surface {
            Column {
                OfflineBanner()
                Box(Modifier.size(Dimens.CoverDetails)) {
                    EmptyState(message = "Você ainda não salvou nenhuma música.", actionLabel = "Conversar", onAction = {})
                }
                Box(Modifier.size(Dimens.CoverDetails)) {
                    ErrorState(message = "Não foi possível buscar músicas. Verifique sua conexão.", onRetry = {})
                }
            }
        }
    }
}
