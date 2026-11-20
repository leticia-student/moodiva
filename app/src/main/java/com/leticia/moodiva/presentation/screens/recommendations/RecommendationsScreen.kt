package com.leticia.moodiva.presentation.screens.recommendations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.leticia.moodiva.R
import com.leticia.moodiva.domain.model.ConversationMode
import com.leticia.moodiva.domain.model.Mood
import com.leticia.moodiva.domain.model.RecommendedTrack
import com.leticia.moodiva.domain.model.Track
import com.leticia.moodiva.domain.model.TrackSource
import com.leticia.moodiva.presentation.components.EmptyState
import com.leticia.moodiva.presentation.components.ErrorState
import com.leticia.moodiva.presentation.components.LoadingState
import com.leticia.moodiva.presentation.components.SourceBadge
import com.leticia.moodiva.presentation.components.TrackCover
import com.leticia.moodiva.presentation.theme.Dimens
import com.leticia.moodiva.presentation.theme.MoodivaTheme
import com.leticia.moodiva.presentation.util.ExternalLinks
import com.leticia.moodiva.presentation.util.UpdatedAtText
import com.leticia.moodiva.presentation.util.formatDuration
import com.leticia.moodiva.presentation.util.plainLabelRes
import com.leticia.moodiva.presentation.util.shortLabelRes
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun RecommendationsRoute(
    mood: Mood,
    mode: ConversationMode,
    onBack: () -> Unit,
    viewModel: RecommendationsViewModel = koinViewModel(key = "${mood.name}-${mode.name}") { parametersOf(mood, mode) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(context.getString(it)) }
    }

    RecommendationsScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onRefresh = viewModel::onRefresh,
        onRetry = viewModel::onRetry,
        onTrackVisible = viewModel::onTrackVisible,
        onSave = viewModel::onSave,
        onOpenSpotify = { track ->
            if (!ExternalLinks.open(context, ExternalLinks.spotifySearch(track))) {
                scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.track_no_app)) }
            }
        },
        onOpenYoutube = { track ->
            if (!ExternalLinks.open(context, ExternalLinks.youtubeSearch(track))) {
                scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.track_no_app)) }
            }
        },
    )
}

/** Lista de recomendações com pull-to-refresh (RF15–RF18). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecommendationsScreen(
    uiState: RecommendationsUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onTrackVisible: (Track) -> Unit,
    onSave: (RecommendedTrack) -> Unit,
    onOpenSpotify: (Track) -> Unit,
    onOpenYoutube: (Track) -> Unit,
    modifier: Modifier = Modifier,
) {
    val moodLabel = stringResource(uiState.mood.plainLabelRes)
    val title = if (uiState.showMode) {
        stringResource(R.string.recommendations_title, moodLabel, stringResource(uiState.mode.shortLabelRes))
    } else {
        moodLabel
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(title, style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) {
            when (val content = uiState.content) {
                RecommendationsContent.Loading -> LoadingState()
                is RecommendationsContent.Error -> ErrorState(stringResource(content.messageRes), onRetry)
                // Rolável para o gesto de puxar funcionar também no estado vazio.
                is RecommendationsContent.Empty -> Box(Modifier.verticalScroll(rememberScrollState())) {
                    EmptyState(
                        message = stringResource(content.messageRes),
                        actionLabel = if (content.canRetry) stringResource(R.string.action_retry) else null,
                        onAction = if (content.canRetry) onRetry else null,
                    )
                }
                is RecommendationsContent.Success -> LazyColumn(
                    modifier = Modifier
                        .widthIn(max = Dimens.ContentMaxWidth)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = Dimens.ScreenHorizontalPadding, vertical = Dimens.MediumSpacing),
                    verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
                ) {
                    content.updatedAt?.let { updatedAt ->
                        item(key = "updated_at") {
                            Text(
                                text = updatedAt.asString(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    items(content.tracks, key = { it.track.key }) { recommended ->
                        LaunchedEffect(recommended.track.key, recommended.coverChecked) {
                            if (!recommended.coverChecked) onTrackVisible(recommended.track)
                        }
                        TrackCard(
                            recommended = recommended,
                            isSaved = recommended.track.key in uiState.savedKeys,
                            onSave = { onSave(recommended) },
                            onOpenSpotify = { onOpenSpotify(recommended.track) },
                            onOpenYoutube = { onOpenYoutube(recommended.track) },
                        )
                    }
                }
            }
        }
    }
}

/** Card de música: `surfaceContainerLow`, capa 56dp, selo de origem e ações (identidade visual). */
@Composable
private fun TrackCard(
    recommended: RecommendedTrack,
    isSaved: Boolean,
    onSave: () -> Unit,
    onOpenSpotify: () -> Unit,
    onOpenYoutube: () -> Unit,
) {
    val track = recommended.track
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(Dimens.ContentPadding), verticalArrangement = Arrangement.spacedBy(Dimens.ContentPadding)) {
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.ContentPadding), verticalAlignment = Alignment.CenterVertically) {
                TrackCover(recommended.coverUrl)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Dimens.SmallSpacing)) {
                    Text(
                        track.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        listOfNotNull(track.artist, track.durationSeconds?.let(::formatDuration)).joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    SourceBadge(recommended.source)
                }
                IconButton(onClick = onSave) {
                    Icon(
                        imageVector = if (isSaved) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = stringResource(if (isSaved) R.string.track_saved else R.string.track_save),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.MediumSpacing)) {
                val spotifyDescription = stringResource(R.string.track_open_spotify_cd, track.name)
                val youtubeDescription = stringResource(R.string.track_open_youtube_cd, track.name)
                Button(
                    onClick = onOpenSpotify,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { contentDescription = spotifyDescription },
                ) { Text(stringResource(R.string.track_open_spotify)) }
                OutlinedButton(
                    onClick = onOpenYoutube,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { contentDescription = youtubeDescription },
                ) { Text(stringResource(R.string.track_open_youtube)) }
            }
        }
    }
}

@Composable
private fun UpdatedAtText.asString(): String = when (this) {
    UpdatedAtText.JustNow -> stringResource(R.string.recommendations_updated_now)
    is UpdatedAtText.MinutesAgo -> pluralStringResource(R.plurals.recommendations_updated_minutes, minutes, minutes)
    is UpdatedAtText.HoursAgo -> pluralStringResource(R.plurals.recommendations_updated_hours, hours, hours)
}

private val previewTracks = listOf(
    RecommendedTrack(Track("Send Me On My Way", "Rusted Root", 261), TrackSource.MOOD),
    RecommendedTrack(Track("Palco", "Gilberto Gil"), TrackSource.TASTE, coverChecked = true),
    RecommendedTrack(Track("Uma música com um nome bem comprido para testar a quebra de linha", "Gal Costa", 199), TrackSource.TASTE),
)

@PreviewLightDark
@Composable
private fun RecommendationsSuccessPreview() {
    MoodivaTheme {
        RecommendationsScreen(
            uiState = RecommendationsUiState(
                mood = Mood.SAD,
                mode = ConversationMode.SHIFT,
                content = RecommendationsContent.Success(previewTracks, UpdatedAtText.HoursAgo(3)),
                savedKeys = setOf(previewTracks[1].track.key),
            ),
            snackbarHostState = SnackbarHostState(),
            onBack = {}, onRefresh = {}, onRetry = {}, onTrackVisible = {}, onSave = {}, onOpenSpotify = {}, onOpenYoutube = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun RecommendationsOfflineEmptyPreview() {
    MoodivaTheme {
        RecommendationsScreen(
            uiState = RecommendationsUiState(
                mood = Mood.HAPPY,
                mode = ConversationMode.MATCH,
                content = RecommendationsContent.Empty(R.string.recommendations_empty_offline, canRetry = true),
            ),
            snackbarHostState = SnackbarHostState(),
            onBack = {}, onRefresh = {}, onRetry = {}, onTrackVisible = {}, onSave = {}, onOpenSpotify = {}, onOpenYoutube = {},
        )
    }
}
