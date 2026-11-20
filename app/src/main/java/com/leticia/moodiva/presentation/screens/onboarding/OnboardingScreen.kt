package com.leticia.moodiva.presentation.screens.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.leticia.moodiva.R
import com.leticia.moodiva.domain.model.Era
import com.leticia.moodiva.domain.model.Genre
import com.leticia.moodiva.domain.model.OriginPreference
import com.leticia.moodiva.domain.rule.TipiCalculator
import com.leticia.moodiva.presentation.components.MoodivaLogo
import com.leticia.moodiva.presentation.components.TraitBars
import com.leticia.moodiva.presentation.theme.Dimens
import com.leticia.moodiva.presentation.theme.MoodivaTheme
import com.leticia.moodiva.presentation.util.TIPI_ITEMS
import com.leticia.moodiva.presentation.util.TIPI_SCALE
import com.leticia.moodiva.presentation.util.hintRes
import com.leticia.moodiva.presentation.util.labelRes
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** Para que o onboarding foi aberto (planejamento, seção 9.1). */
enum class OnboardingMode {
    /** Primeira vez: teste + gostos + resultado. */
    FULL,

    /** Pelo perfil: só o teste. */
    RETAKE_TEST,

    /** Pelo perfil: só os gostos. */
    EDIT_TASTE,
}

@Composable
fun OnboardingRoute(
    mode: OnboardingMode,
    onClose: () -> Unit,
    viewModel: OnboardingViewModel = koinViewModel(key = mode.name) { parametersOf(mode) },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) { viewModel.closeEvents.collect { onClose() } }

    // Voltar anda pelas etapas; na primeira, fecha a edição (no modo FULL, deixa o sistema sair).
    BackHandler(enabled = uiState.mode != OnboardingMode.FULL || uiState.step != uiState.steps.first()) {
        if (!viewModel.onBack()) onClose()
    }

    OnboardingScreen(
        uiState = uiState,
        actions = OnboardingActions(
            onAnswer = viewModel::onAnswer,
            onBack = { if (!viewModel.onBack()) onClose() },
            onContinue = viewModel::onContinue,
            onToggleGenre = viewModel::onToggleGenre,
            onArtistInputChange = viewModel::onArtistInputChange,
            onAddArtist = viewModel::onAddArtist,
            onRemoveArtist = viewModel::onRemoveArtist,
            onToggleEra = viewModel::onToggleEra,
            onSelectOrigin = viewModel::onSelectOrigin,
        ),
    )
}

/** Ações da tela agrupadas, para a assinatura do composable não ficar enorme. */
data class OnboardingActions(
    val onAnswer: (Int) -> Unit = {},
    val onBack: () -> Unit = {},
    val onContinue: () -> Unit = {},
    val onToggleGenre: (Genre) -> Unit = {},
    val onArtistInputChange: (String) -> Unit = {},
    val onAddArtist: () -> Unit = {},
    val onRemoveArtist: (String) -> Unit = {},
    val onToggleEra: (Era) -> Unit = {},
    val onSelectOrigin: (OriginPreference) -> Unit = {},
)

/** Onboarding: teste TIPI e gostos musicais (RF04–RF12). */
@Composable
fun OnboardingScreen(
    uiState: OnboardingUiState,
    actions: OnboardingActions,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (uiState.step != OnboardingStep.INTRO && uiState.step != OnboardingStep.RESULT) {
            LinearProgressIndicator(
                progress = { uiState.progress },
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer,
                drawStopIndicator = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.ScreenHorizontalPadding, vertical = Dimens.MediumSpacing),
            )
        }
        AnimatedContent(
            targetState = uiState.step to uiState.tipiIndex,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "onboarding-step",
            modifier = Modifier
                .weight(1f)
                .widthIn(max = Dimens.ContentMaxWidth),
        ) { (step, _) ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Dimens.ScreenHorizontalPadding, vertical = Dimens.SectionSpacing),
                verticalArrangement = Arrangement.spacedBy(Dimens.ItemSpacing),
            ) {
                when (step) {
                    OnboardingStep.INTRO -> IntroStep()
                    OnboardingStep.TIPI -> TipiStep(uiState, actions.onAnswer)
                    OnboardingStep.GENRES -> GenresStep(uiState.genres, actions.onToggleGenre)
                    OnboardingStep.ARTISTS -> ArtistsStep(uiState, actions)
                    OnboardingStep.ERAS -> ErasStep(uiState.eras, actions.onToggleEra)
                    OnboardingStep.ORIGIN -> OriginStep(uiState.origin, actions.onSelectOrigin)
                    OnboardingStep.RESULT -> ResultStep(uiState)
                }
            }
        }
        BottomBar(uiState, actions)
    }
}

@Composable
private fun BottomBar(uiState: OnboardingUiState, actions: OnboardingActions) {
    Column(
        modifier = Modifier
            .widthIn(max = Dimens.ContentMaxWidth)
            .fillMaxWidth()
            .padding(horizontal = Dimens.ScreenHorizontalPadding, vertical = Dimens.ItemSpacing),
        verticalArrangement = Arrangement.spacedBy(Dimens.MediumSpacing),
    ) {
        uiState.errorRes?.let {
            Text(stringResource(it), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        }
        if (uiState.step == OnboardingStep.TIPI) {
            // Cada resposta já avança sozinha; aqui só dá para voltar.
            TextButton(onClick = actions.onBack) { Text(stringResource(R.string.onboarding_previous)) }
            return
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.MediumSpacing), verticalAlignment = Alignment.CenterVertically) {
            val isFirst = uiState.steps.first() == uiState.step
            if (!isFirst || uiState.mode != OnboardingMode.FULL) {
                TextButton(onClick = actions.onBack) {
                    Text(stringResource(if (isFirst) R.string.action_cancel else R.string.onboarding_previous))
                }
            }
            Button(
                onClick = actions.onContinue,
                enabled = uiState.canContinue && !uiState.isSaving,
                modifier = Modifier.weight(1f),
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(strokeWidth = Dimens.ProgressStrokeWidth, modifier = Modifier.size(Dimens.IconSmall))
                } else {
                    Text(stringResource(continueLabel(uiState)))
                }
            }
        }
    }
}

private fun continueLabel(uiState: OnboardingUiState): Int = when {
    uiState.step == OnboardingStep.INTRO -> R.string.onboarding_start_test
    uiState.step == OnboardingStep.RESULT && uiState.mode == OnboardingMode.FULL -> R.string.onboarding_go_to_chat
    uiState.steps.last() == uiState.step -> R.string.action_save
    uiState.step == OnboardingStep.ORIGIN -> R.string.onboarding_finish
    else -> R.string.action_continue
}

@Composable
private fun StepTitle(title: Int, hint: Int? = null) {
    Text(stringResource(title), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
    hint?.let {
        Text(stringResource(it), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun IntroStep() {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { MoodivaLogo(large = true) }
    Text(
        stringResource(R.string.onboarding_intro_title),
        style = MaterialTheme.typography.displaySmall,
        color = MaterialTheme.colorScheme.onBackground,
    )
    Text(stringResource(R.string.onboarding_intro_text), style = MaterialTheme.typography.bodyLarge)
    // RF06: aviso de que o teste não é uma avaliação psicológica.
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer, shape = MaterialTheme.shapes.medium) {
        Text(
            stringResource(R.string.onboarding_disclaimer),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(Dimens.ItemSpacing),
        )
    }
}

@Composable
private fun TipiStep(uiState: OnboardingUiState, onAnswer: (Int) -> Unit) {
    val index = uiState.tipiIndex
    val selected = uiState.answers[index]
    Text(
        stringResource(R.string.onboarding_tipi_progress, index + 1, TipiCalculator.ITEM_COUNT),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
    )
    Text(stringResource(R.string.onboarding_tipi_prompt), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
        Text(
            stringResource(TIPI_ITEMS[index]),
            // Identidade visual: perguntas do teste em bodyLarge.
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(Dimens.SectionSpacing),
        )
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectableGroup(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        (TipiCalculator.MIN_ANSWER..TipiCalculator.MAX_ANSWER).forEach { value ->
            ScaleOption(value = value, selected = selected == value, onClick = { onAnswer(value) })
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(stringResource(R.string.onboarding_tipi_scale_low), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(stringResource(R.string.onboarding_tipi_scale_high), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    selected?.let {
        Text(
            stringResource(TIPI_SCALE[it - 1]),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Um ponto da escala 1–7: selecionado em `primary`, senão borda `outlineVariant`. */
@Composable
private fun ScaleOption(value: Int, selected: Boolean, onClick: () -> Unit) {
    val description = stringResource(R.string.onboarding_answer_cd, value, stringResource(TIPI_SCALE[value - 1]))
    Surface(
        shape = CircleShape,
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLowest,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        border = if (selected) null else BorderStroke(Dimens.BorderWidth, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .size(Dimens.ScaleOptionSize)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .semantics { contentDescription = description },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(value.toString(), style = MaterialTheme.typography.labelLarge)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class) // FlowRow
@Composable
private fun GenresStep(selected: Set<Genre>, onToggle: (Genre) -> Unit) {
    StepTitle(R.string.onboarding_genres_title, R.string.onboarding_genres_hint)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.MediumSpacing)) {
        Genre.entries.forEach { genre ->
            SecondaryChip(label = stringResource(genre.labelRes), selected = genre in selected, onClick = { onToggle(genre) })
        }
    }
}

/** Chip de gênero/época: selecionado em `secondaryContainer` (identidade visual). */
@Composable
private fun SecondaryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
        shape = MaterialTheme.shapes.small,
    )
}

@OptIn(ExperimentalLayoutApi::class) // FlowRow
@Composable
private fun ArtistsStep(uiState: OnboardingUiState, actions: OnboardingActions) {
    StepTitle(R.string.onboarding_artists_title, R.string.onboarding_artists_hint)
    val full = uiState.artists.size >= OnboardingUiState.MAX_ARTISTS
    OutlinedTextField(
        value = uiState.artistInput,
        onValueChange = actions.onArtistInputChange,
        label = { Text(stringResource(R.string.onboarding_artist_field)) },
        enabled = !full,
        supportingText = if (full) { { Text(stringResource(R.string.onboarding_artists_limit)) } } else null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { actions.onAddArtist() }),
        trailingIcon = {
            IconButton(onClick = actions.onAddArtist, enabled = uiState.canAddArtist) {
                Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.onboarding_artist_add))
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.MediumSpacing)) {
        uiState.artists.forEach { artist ->
            InputChip(
                selected = true,
                onClick = { actions.onRemoveArtist(artist) },
                label = { Text(artist, style = MaterialTheme.typography.labelMedium) },
                trailingIcon = {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.onboarding_artist_remove, artist),
                        modifier = Modifier.size(Dimens.IconSmall),
                    )
                },
                shape = MaterialTheme.shapes.small,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class) // FlowRow
@Composable
private fun ErasStep(selected: Set<Era>, onToggle: (Era) -> Unit) {
    StepTitle(R.string.onboarding_eras_title, R.string.onboarding_eras_hint)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Dimens.MediumSpacing)) {
        Era.entries.forEach { era ->
            SecondaryChip(label = stringResource(era.labelRes), selected = era in selected, onClick = { onToggle(era) })
        }
    }
}

@Composable
private fun OriginStep(selected: OriginPreference?, onSelect: (OriginPreference) -> Unit) {
    StepTitle(R.string.onboarding_origin_title, R.string.onboarding_origin_hint)
    Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(Dimens.MediumSpacing)) {
        OriginPreference.entries.forEach { origin ->
            val isSelected = origin == selected
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                border = BorderStroke(
                    if (isSelected) Dimens.SelectedBorderWidth else Dimens.BorderWidth,
                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(selected = isSelected, onClick = { onSelect(origin) }, role = Role.RadioButton),
            ) {
                Row(Modifier.padding(Dimens.ContentPadding), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = isSelected, onClick = null)
                    Column(Modifier.padding(start = Dimens.MediumSpacing)) {
                        Text(stringResource(origin.labelRes), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(origin.hintRes), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultStep(uiState: OnboardingUiState) {
    Text(
        stringResource(R.string.onboarding_result_title),
        style = MaterialTheme.typography.displaySmall,
        color = MaterialTheme.colorScheme.onBackground,
    )
    if (uiState.mode == OnboardingMode.FULL) {
        Text(stringResource(R.string.onboarding_result_text), style = MaterialTheme.typography.bodyLarge)
    }
    uiState.result?.let { TraitBars(it, Modifier.padding(top = Dimens.MediumSpacing)) }
    Text(
        stringResource(R.string.onboarding_disclaimer),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@PreviewLightDark
@Composable
private fun OnboardingTipiPreview() {
    MoodivaTheme {
        Surface {
            OnboardingScreen(
                uiState = OnboardingUiState(
                    mode = OnboardingMode.FULL,
                    step = OnboardingStep.TIPI,
                    tipiIndex = 2,
                    answers = listOf(5, 3, 6, null, null, null, null, null, null, null),
                ),
                actions = OnboardingActions(),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun OnboardingGenresPreview() {
    MoodivaTheme {
        Surface {
            OnboardingScreen(
                uiState = OnboardingUiState(mode = OnboardingMode.FULL, step = OnboardingStep.GENRES, genres = setOf(Genre.MPB, Genre.POP)),
                actions = OnboardingActions(),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun OnboardingResultPreview() {
    MoodivaTheme {
        Surface {
            OnboardingScreen(
                uiState = OnboardingUiState(
                    mode = OnboardingMode.FULL,
                    step = OnboardingStep.RESULT,
                    result = TipiCalculator.calculate(listOf(5, 3, 6, 2, 7, 2, 6, 1, 5, 3)),
                ),
                actions = OnboardingActions(),
            )
        }
    }
}
