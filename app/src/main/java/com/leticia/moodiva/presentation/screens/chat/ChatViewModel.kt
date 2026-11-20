package com.leticia.moodiva.presentation.screens.chat

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leticia.moodiva.R
import com.leticia.moodiva.core.result.AppError
import com.leticia.moodiva.core.result.AppResult
import com.leticia.moodiva.domain.model.ConversationAnswer
import com.leticia.moodiva.domain.model.ConversationMode
import com.leticia.moodiva.domain.model.ConversationStep
import com.leticia.moodiva.domain.model.Mood
import com.leticia.moodiva.domain.rule.ConversationMachine
import com.leticia.moodiva.domain.usecase.ObserveRecommendationsUseCase
import com.leticia.moodiva.domain.usecase.RefreshRecommendationsUseCase
import com.leticia.moodiva.presentation.util.ErrorMessageMapper
import com.leticia.moodiva.presentation.util.labelRes
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Quem falou. */
enum class ChatAuthor { APP, USER }

/** Um balão da conversa. O texto é sempre um recurso de string. */
@Immutable
sealed interface ChatMessage {
    val id: Int
    val author: ChatAuthor

    data class Text(override val id: Int, override val author: ChatAuthor, @StringRes val textRes: Int) : ChatMessage

    /** Balão do app com o indicador de carregamento (identidade visual, "Estados das telas"). */
    data class Loading(override val id: Int) : ChatMessage {
        override val author = ChatAuthor.APP
    }
}

/** Chips de resposta disponíveis no momento. */
@Immutable
sealed interface ChatOption {
    @get:StringRes
    val labelRes: Int

    data class Answer(val answer: ConversationAnswer, override val labelRes: Int) : ChatOption
    data object SeeSongs : ChatOption { override val labelRes = R.string.chat_see_songs }
    data object Retry : ChatOption { override val labelRes = R.string.action_retry }
    data object Restart : ChatOption { override val labelRes = R.string.chat_restart }
}

@Immutable
data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val options: List<ChatOption> = emptyList(),
)

/**
 * Conversa guiada, sem IA (RF13, RF14; RN10, RN11).
 *
 * A lógica de passos fica na `ConversationMachine` (domain). Este ViewModel transforma
 * cada passo em balões e chips. Ao concluir, pede as recomendações (respeitando o cache
 * de 24 h) e, se houver músicas, abre a lista. A conversa funciona offline até esse ponto.
 */
class ChatViewModel(
    private val refreshRecommendations: RefreshRecommendationsUseCase,
    private val observeRecommendations: ObserveRecommendationsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    /** Evento único: abrir as recomendações de um humor e modo. */
    private val _showRecommendations = Channel<Pair<Mood, ConversationMode>>(Channel.BUFFERED)
    val showRecommendations: Flow<Pair<Mood, ConversationMode>> = _showRecommendations.receiveAsFlow()

    private var step: ConversationStep = ConversationMachine.START
    private var nextId = 0

    init {
        restart()
    }

    fun onOption(option: ChatOption) {
        when (option) {
            is ChatOption.Answer -> answer(option)
            ChatOption.SeeSongs -> (step as? ConversationStep.Done)?.let { openRecommendations(it) }
            ChatOption.Retry -> (step as? ConversationStep.Done)?.let { fetch(it) }
            ChatOption.Restart -> restart()
        }
    }

    private fun answer(option: ChatOption.Answer) {
        val next = ConversationMachine.next(step, option.answer) ?: return
        step = next
        addMessage(ChatAuthor.USER, option.labelRes)
        when (next) {
            ConversationStep.Greeting -> Unit
            ConversationStep.AskMood -> ask(R.string.chat_ask_mood)
            is ConversationStep.AskMode -> {
                if (next.mood == Mood.ANXIOUS) addMessage(ChatAuthor.APP, R.string.chat_anxious_note)
                ask(R.string.chat_ask_mode)
            }
            is ConversationStep.Done -> fetch(next)
        }
    }

    private fun restart() {
        step = ConversationMachine.START
        _uiState.value = ChatUiState()
        ask(R.string.chat_greeting)
    }

    /** Balão do app com a pergunta do passo e os chips das respostas possíveis. */
    private fun ask(@StringRes question: Int) {
        addMessage(ChatAuthor.APP, question)
        _uiState.update { it.copy(options = ConversationMachine.answersFor(step).map { a -> ChatOption.Answer(a, labelFor(a)) }) }
    }

    private fun fetch(done: ConversationStep.Done) {
        val loading = ChatMessage.Loading(nextId++)
        _uiState.update { it.copy(messages = it.messages + loading, options = emptyList()) }
        viewModelScope.launch {
            val result = refreshRecommendations(done.mood, done.mode)
            val hasCache = observeRecommendations(done.mood, done.mode).first().tracks.isNotEmpty()
            _uiState.update { it.copy(messages = it.messages - loading) }
            // Mesmo se a busca falhar, o cache (ainda que antigo) é mostrado (RNF01).
            if (result is AppResult.Success || hasCache) {
                addMessage(ChatAuthor.APP, R.string.chat_ready)
                _uiState.update { it.copy(options = listOf(ChatOption.SeeSongs, ChatOption.Restart)) }
                openRecommendations(done)
            } else {
                val error = (result as AppResult.Failure).error
                addMessage(
                    ChatAuthor.APP,
                    if (error == AppError.Network) R.string.chat_error_first_search else ErrorMessageMapper.messageFor(error),
                )
                _uiState.update { it.copy(options = listOf(ChatOption.Retry, ChatOption.Restart)) }
            }
        }
    }

    private fun openRecommendations(done: ConversationStep.Done) {
        _showRecommendations.trySend(done.mood to done.mode)
    }

    private fun addMessage(author: ChatAuthor, @StringRes text: Int) =
        _uiState.update { it.copy(messages = it.messages + ChatMessage.Text(nextId++, author, text)) }

    private fun labelFor(answer: ConversationAnswer): Int = when (answer) {
        ConversationAnswer.Start -> R.string.chat_answer_start
        is ConversationAnswer.ChooseMood -> answer.mood.labelRes
        is ConversationAnswer.ChooseMode -> answer.mode.labelRes
    }
}
