package com.leticia.moodiva.presentation.screens.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.leticia.moodiva.R
import com.leticia.moodiva.domain.model.ConversationMode
import com.leticia.moodiva.domain.model.Mood
import com.leticia.moodiva.presentation.components.MoodivaLogo
import com.leticia.moodiva.presentation.theme.ChatBubbleShapes
import com.leticia.moodiva.presentation.theme.Dimens
import com.leticia.moodiva.presentation.theme.MoodivaTheme
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ChatRoute(
    onShowRecommendations: (Mood, ConversationMode) -> Unit,
    viewModel: ChatViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.showRecommendations.collect { (mood, mode) -> onShowRecommendations(mood, mode) }
    }
    ChatScreen(uiState = uiState, onOption = viewModel::onOption)
}

/** Conversa guiada com balões e chips (RF13, RF14). */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChatScreen(
    uiState: ChatUiState,
    onOption: (ChatOption) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    // Cada balão novo rola a conversa até o fim.
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) listState.animateScrollToItem(uiState.messages.lastIndex)
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { MoodivaLogo() },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .widthIn(max = Dimens.ContentMaxWidth),
            ) {
                val bubbleMaxWidth = maxWidth * Dimens.BUBBLE_MAX_WIDTH_FRACTION
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(horizontal = Dimens.ScreenHorizontalPadding, vertical = Dimens.ItemSpacing),
                    modifier = Modifier
                        .fillMaxSize()
                        .semantics { liveRegion = LiveRegionMode.Polite },
                ) {
                    itemsIndexed(uiState.messages, key = { _, message -> message.id }) { index, message ->
                        val authorChanged = index > 0 && uiState.messages[index - 1].author != message.author
                        val topSpacing = when {
                            index == 0 -> Dimens.SmallSpacing
                            authorChanged -> Dimens.BubbleAuthorChangeSpacing
                            else -> Dimens.BubbleSameAuthorSpacing
                        }
                        ChatBubble(message, bubbleMaxWidth, Modifier.padding(top = topSpacing))
                    }
                }
            }
            if (uiState.options.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Dimens.MediumSpacing, Alignment.End),
                    verticalArrangement = Arrangement.spacedBy(Dimens.MediumSpacing),
                    modifier = Modifier
                        .widthIn(max = Dimens.ContentMaxWidth)
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.ScreenHorizontalPadding, vertical = Dimens.ItemSpacing),
                ) {
                    uiState.options.forEach { option -> AnswerChip(option, onClick = { onOption(option) }) }
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage, maxWidth: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier) {
    val isUser = message.author == ChatAuthor.USER
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Surface(
            shape = if (isUser) ChatBubbleShapes.User else ChatBubbleShapes.App,
            color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer,
            contentColor = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.widthIn(max = maxWidth),
        ) {
            when (message) {
                is ChatMessage.Text -> Text(
                    text = stringResource(message.textRes),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = Dimens.ItemSpacing, vertical = Dimens.ContentPadding),
                )
                is ChatMessage.Loading -> {
                    val description = stringResource(R.string.chat_loading)
                    Row(
                        modifier = Modifier
                            .padding(horizontal = Dimens.ItemSpacing, vertical = Dimens.ContentPadding)
                            .semantics { contentDescription = description },
                        horizontalArrangement = Arrangement.spacedBy(Dimens.ContentPadding),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = Dimens.ProgressStrokeWidth,
                            modifier = Modifier.size(Dimens.IconSmall),
                        )
                        Text(description, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}

/** Chip de resposta: `surfaceContainerLowest` com borda `outlineVariant` (identidade visual). */
@Composable
private fun AnswerChip(option: ChatOption, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        contentColor = MaterialTheme.colorScheme.primary,
        border = BorderStroke(Dimens.BorderWidth, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Text(
            text = stringResource(option.labelRes),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = Dimens.ItemSpacing, vertical = Dimens.ContentPadding),
        )
    }
}

@PreviewLightDark
@Composable
private fun ChatScreenPreview() {
    MoodivaTheme {
        ChatScreen(
            uiState = ChatUiState(
                messages = listOf(
                    ChatMessage.Text(0, ChatAuthor.APP, R.string.chat_greeting),
                    ChatMessage.Text(1, ChatAuthor.USER, R.string.chat_answer_start),
                    ChatMessage.Text(2, ChatAuthor.APP, R.string.chat_ask_mood),
                    ChatMessage.Text(3, ChatAuthor.USER, R.string.mood_sad),
                    ChatMessage.Loading(4),
                ),
                options = listOf(ChatOption.Restart),
            ),
            onOption = {},
        )
    }
}
