package com.leticia.moodiva.presentation.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.leticia.moodiva.R
import com.leticia.moodiva.domain.model.Era
import com.leticia.moodiva.domain.model.Genre
import com.leticia.moodiva.domain.model.OriginPreference
import com.leticia.moodiva.domain.model.TasteProfile
import com.leticia.moodiva.domain.model.UserProfile
import com.leticia.moodiva.domain.rule.TipiCalculator
import com.leticia.moodiva.presentation.components.SecondaryButton
import com.leticia.moodiva.presentation.components.LoadingState
import com.leticia.moodiva.presentation.components.TraitBars
import com.leticia.moodiva.presentation.theme.Dimens
import com.leticia.moodiva.presentation.theme.MoodivaTheme
import com.leticia.moodiva.presentation.util.labelRes
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ProfileRoute(
    onRetakeTest: () -> Unit,
    onEditTaste: () -> Unit,
    onOpenAbout: () -> Unit,
    viewModel: ProfileViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ProfileScreen(
        uiState = uiState,
        onRetakeTest = onRetakeTest,
        onEditTaste = onEditTaste,
        onOpenAbout = onOpenAbout,
        onSignOut = viewModel::onSignOut,
    )
}

/** Perfil: traços do TIPI, gostos e ações (RF11, RF12, RF03). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    onRetakeTest: () -> Unit,
    onEditTaste: () -> Unit,
    onOpenAbout: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profile_title), style = MaterialTheme.typography.headlineSmall) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { innerPadding ->
        when (uiState) {
            ProfileUiState.Loading -> LoadingState(Modifier.padding(innerPadding))
            is ProfileUiState.Success -> Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentAlignment = Alignment.TopCenter,
            ) {
                ProfileContent(uiState, onRetakeTest, onEditTaste, onOpenAbout, onSignOut)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class) // FlowRow
@Composable
private fun ProfileContent(
    state: ProfileUiState.Success,
    onRetakeTest: () -> Unit,
    onEditTaste: () -> Unit,
    onOpenAbout: () -> Unit,
    onSignOut: () -> Unit,
) {
    val taste = state.profile.taste
    Column(
        modifier = Modifier
            .widthIn(max = Dimens.ContentMaxWidth)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Dimens.ScreenHorizontalPadding, vertical = Dimens.MediumSpacing),
        verticalArrangement = Arrangement.spacedBy(Dimens.SectionSpacing),
    ) {
        Text(
            stringResource(R.string.profile_account, state.email),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Section(R.string.profile_personality) {
            Text(
                stringResource(R.string.profile_personality_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TraitBars(state.profile.personality)
            SecondaryButton(onClick = onRetakeTest, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.profile_retake))
            }
        }

        Section(R.string.profile_genres) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.MediumSpacing), verticalArrangement = Arrangement.spacedBy(Dimens.MediumSpacing)) {
                taste.genres.sortedBy { it.ordinal }.forEach { GenreTag(stringResource(it.labelRes)) }
            }
        }
        Section(R.string.profile_artists) {
            Text(taste.favoriteArtists.joinToString(" · "), style = MaterialTheme.typography.bodyLarge)
        }
        Section(R.string.profile_eras) {
            Text(
                text = if (taste.eras.isEmpty()) {
                    stringResource(R.string.profile_eras_none)
                } else {
                    taste.eras.sortedBy { it.ordinal }.map { stringResource(it.labelRes) }.joinToString(" · ")
                },
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        Section(R.string.profile_origin) {
            Text(stringResource(taste.origin.labelRes), style = MaterialTheme.typography.bodyLarge)
            SecondaryButton(onClick = onEditTaste, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.profile_edit_taste))
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onOpenAbout) { Text(stringResource(R.string.profile_about)) }
            TextButton(onClick = onSignOut) { Text(stringResource(R.string.profile_sign_out)) }
        }
    }
}

@Composable
private fun Section(title: Int, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.ContentPadding)) {
        Text(stringResource(title), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
        content()
    }
}

/** Gênero exibido no perfil: `secondaryContainer` (identidade visual, chip de gênero). */
@Composable
private fun GenreTag(label: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = Dimens.ContentPadding, vertical = Dimens.MediumSpacing),
        )
    }
}

@PreviewLightDark
@Composable
private fun ProfilePreview() {
    MoodivaTheme {
        ProfileScreen(
            uiState = ProfileUiState.Success(
                email = "ana@mail.com",
                profile = UserProfile(
                    personality = TipiCalculator.calculate(listOf(5, 3, 6, 2, 7, 2, 6, 1, 5, 3))!!,
                    taste = TasteProfile(
                        genres = setOf(Genre.MPB, Genre.POP),
                        favoriteArtists = listOf("Caetano Veloso", "Gal Costa"),
                        eras = setOf(Era.SEVENTIES),
                        origin = OriginPreference.BOTH,
                    ),
                ),
            ),
            onRetakeTest = {}, onEditTaste = {}, onOpenAbout = {}, onSignOut = {},
        )
    }
}
