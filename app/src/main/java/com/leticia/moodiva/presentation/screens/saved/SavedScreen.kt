package com.leticia.moodiva.presentation.screens.saved

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.leticia.moodiva.R
import com.leticia.moodiva.domain.model.ListenStatus
import com.leticia.moodiva.domain.model.SavedTrack
import com.leticia.moodiva.domain.model.Track
import com.leticia.moodiva.domain.model.TrackSource
import com.leticia.moodiva.presentation.components.EmptyState
import com.leticia.moodiva.presentation.components.ErrorState
import com.leticia.moodiva.presentation.components.LoadingState
import com.leticia.moodiva.presentation.components.RatingBar
import com.leticia.moodiva.presentation.components.TrackCover
import com.leticia.moodiva.presentation.theme.Dimens
import com.leticia.moodiva.presentation.theme.MoodivaTheme
import com.leticia.moodiva.presentation.util.ExternalLinks
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import java.time.Instant

@Composable
fun SavedRoute(
    onGoToChat: () -> Unit,
    viewModel: SavedViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(context.getString(it)) }
    }

    fun open(uri: android.net.Uri) {
        if (!ExternalLinks.open(context, uri)) {
            scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.track_no_app)) }
        }
    }

    SavedScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        actions = SavedActions(
            onFilter = viewModel::onFilter,
            onMarkListened = viewModel::onMarkListened,
            onRate = viewModel::onRate,
            onRequestRemove = viewModel::onRequestRemove,
            onConfirmRemove = viewModel::onConfirmRemove,
            onDismissRemove = viewModel::onDismissRemove,
            onOpenSpotify = { open(ExternalLinks.spotifySearch(it)) },
            onOpenYoutube = { open(ExternalLinks.youtubeSearch(it)) },
            onGoToChat = onGoToChat,
        ),
    )
}

data class SavedActions(
    val onFilter: (SavedFilter) -> Unit = {},
    val onMarkListened: (SavedTrack) -> Unit = {},
    val onRate: (SavedTrack, Int) -> Unit = { _, _ -> },
    val onRequestRemove: (SavedTrack) -> Unit = {},
    val onConfirmRemove: () -> Unit = {},
    val onDismissRemove: () -> Unit = {},
    val onOpenSpotify: (Track) -> Unit = {},
    val onOpenYoutube: (Track) -> Unit = {},
    val onGoToChat: () -> Unit = {},
)

/** Lista de músicas salvas com filtro, "Já ouvi", estrelas e remoção (RF19–RF21, RF26). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedScreen(
    uiState: SavedUiState,
    snackbarHostState: SnackbarHostState,
    actions: SavedActions,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.saved_title), style = MaterialTheme.typography.headlineSmall) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier
                    .widthIn(max = Dimens.ContentMaxWidth)
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.ScreenHorizontalPadding),
                horizontalArrangement = Arrangement.spacedBy(Dimens.MediumSpacing),
            ) {
                SavedFilter.entries.forEach { filter -> FilterOption(filter, filter == uiState.filter) { actions.onFilter(filter) } }
            }
            when (val content = uiState.content) {
                SavedContent.Loading -> LoadingState()
                is SavedContent.Error -> ErrorState(stringResource(content.messageRes), onRetry = { actions.onFilter(uiState.filter) })
                is SavedContent.Empty -> EmptyState(
                    message = stringResource(content.messageRes),
                    actionLabel = if (content.showChatAction) stringResource(R.string.saved_go_chat) else null,
                    onAction = if (content.showChatAction) actions.onGoToChat else null,
                )
                is SavedContent.Success -> LazyColumn(
                    modifier = Modifier
                        .widthIn(max = Dimens.ContentMaxWidth)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = Dimens.ScreenHorizontalPadding, vertical = Dimens.ItemSpacing),
                    verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
                ) {
                    items(content.tracks, key = { it.track.key }) { saved -> SavedItem(saved, actions) }
                }
            }
        }
    }

    uiState.pendingRemoval?.let { track ->
        AlertDialog(
            onDismissRequest = actions.onDismissRemove,
            title = { Text(stringResource(R.string.saved_remove_title)) },
            text = { Text(stringResource(R.string.saved_remove_text, track.track.name)) },
            confirmButton = { TextButton(onClick = actions.onConfirmRemove) { Text(stringResource(R.string.saved_remove_confirm)) } },
            dismissButton = { TextButton(onClick = actions.onDismissRemove) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

/** Filtro: selecionado como o chip de resposta selecionado (`primaryContainer` + borda `primary`). */
@Composable
private fun FilterOption(filter: SavedFilter, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(stringResource(filter.labelRes), style = MaterialTheme.typography.labelMedium) },
        shape = MaterialTheme.shapes.small,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = MaterialTheme.colorScheme.outlineVariant,
            selectedBorderColor = MaterialTheme.colorScheme.primary,
            borderWidth = Dimens.BorderWidth,
            selectedBorderWidth = Dimens.SelectedBorderWidth,
        ),
    )
}

/** Item da lista (`surfaceContainerLow`, identidade visual). */
@Composable
private fun SavedItem(saved: SavedTrack, actions: SavedActions) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(Dimens.ContentPadding), verticalArrangement = Arrangement.spacedBy(Dimens.MediumSpacing)) {
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.ContentPadding), verticalAlignment = Alignment.CenterVertically) {
                TrackCover(saved.coverUrl)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Dimens.SmallSpacing)) {
                    Text(saved.track.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        saved.track.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        stringResource(if (saved.status == ListenStatus.LISTENED) R.string.saved_status_listened else R.string.saved_status_want),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { actions.onRequestRemove(saved) }) {
                    Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.saved_remove), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (saved.status == ListenStatus.WANT_TO_LISTEN) {
                OutlinedButton(
                    onClick = { actions.onMarkListened(saved) },
                    border = BorderStroke(Dimens.BorderWidth, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(R.string.saved_mark_listened)) }
            } else {
                RatingBar(rating = saved.rating, onRate = { actions.onRate(saved, it) })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.MediumSpacing)) {
                TextButton(onClick = { actions.onOpenSpotify(saved.track) }) { Text(stringResource(R.string.track_open_spotify)) }
                TextButton(onClick = { actions.onOpenYoutube(saved.track) }) { Text(stringResource(R.string.track_open_youtube)) }
            }
        }
    }
}

private val previewSaved = listOf(
    SavedTrack(Track("Palco", "Gilberto Gil"), null, TrackSource.TASTE, ListenStatus.WANT_TO_LISTEN, null, Instant.EPOCH),
    SavedTrack(Track("Tik Tok", "Ke\$ha"), null, TrackSource.MOOD, ListenStatus.LISTENED, 4, Instant.EPOCH),
)

@PreviewLightDark
@Composable
private fun SavedSuccessPreview() {
    MoodivaTheme {
        SavedScreen(SavedUiState(content = SavedContent.Success(previewSaved)), SnackbarHostState(), SavedActions())
    }
}

@PreviewLightDark
@Composable
private fun SavedEmptyPreview() {
    MoodivaTheme {
        SavedScreen(
            SavedUiState(content = SavedContent.Empty(R.string.saved_empty, showChatAction = true)),
            SnackbarHostState(),
            SavedActions(),
        )
    }
}
