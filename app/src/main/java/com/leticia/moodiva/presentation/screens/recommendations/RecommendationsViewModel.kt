package com.leticia.moodiva.presentation.screens.recommendations

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leticia.moodiva.R
import com.leticia.moodiva.core.result.AppError
import com.leticia.moodiva.core.result.AppResult
import com.leticia.moodiva.domain.model.ConversationMode
import com.leticia.moodiva.domain.model.Mood
import com.leticia.moodiva.domain.model.Recommendations
import com.leticia.moodiva.domain.model.RecommendedTrack
import com.leticia.moodiva.domain.model.Track
import com.leticia.moodiva.domain.rule.MoodTagMapper
import com.leticia.moodiva.domain.usecase.LoadCoverUseCase
import com.leticia.moodiva.domain.usecase.ObserveRecommendationsUseCase
import com.leticia.moodiva.domain.usecase.ObserveSavedTracksUseCase
import com.leticia.moodiva.domain.usecase.RefreshRecommendationsUseCase
import com.leticia.moodiva.domain.usecase.SaveTrackResult
import com.leticia.moodiva.domain.usecase.SaveTrackUseCase
import com.leticia.moodiva.presentation.util.ErrorMessageMapper
import com.leticia.moodiva.presentation.util.UpdatedAtFormatter
import com.leticia.moodiva.presentation.util.UpdatedAtText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock

@Immutable
data class RecommendationsUiState(
    val mood: Mood,
    val mode: ConversationMode,
    /** O modo só aparece no título quando o humor o pergunta (RN10). */
    val showMode: Boolean = MoodTagMapper.asksForMode(mood),
    val content: RecommendationsContent = RecommendationsContent.Loading,
    val isRefreshing: Boolean = false,
    /** Chaves das músicas já salvas, para o ícone de "salvo" do card. */
    val savedKeys: Set<String> = emptySet(),
)

@Immutable
sealed interface RecommendationsContent {
    data object Loading : RecommendationsContent
    data class Success(val tracks: List<RecommendedTrack>, val updatedAt: UpdatedAtText?) : RecommendationsContent

    /** Vazio com motivo: sem resultados, ou sem cache e sem internet (com "Tentar novamente"). */
    data class Empty(@StringRes val messageRes: Int, val canRetry: Boolean) : RecommendationsContent
    data class Error(@StringRes val messageRes: Int) : RecommendationsContent
}

/** Situação da última atualização, usada quando o cache está vazio. */
private sealed interface RefreshStatus {
    data object AwaitingData : RefreshStatus
    data object NoResults : RefreshStatus
    data class Failed(val error: AppError) : RefreshStatus
}

/**
 * Recomendações de um humor e modo (RF15–RF18; Offline First, planejamento seção 6).
 *
 * A lista vem sempre do banco. Ao abrir, pede uma atualização que só chama a API se o
 * cache estiver vazio ou vencido (24 h):
 *
 * | Cache     | Atualização     | Estado                                  |
 * |-----------|-----------------|-----------------------------------------|
 * | com dados | qualquer        | Success (+ Snackbar se falhar, CA18)    |
 * | vazio     | em andamento    | Loading                                 |
 * | vazio     | sem músicas     | Empty (sem resultados)                  |
 * | vazio     | sem internet    | Empty "precisa de internet" + tentar (CA19) |
 * | vazio     | outra falha     | Error + "Tentar novamente"              |
 */
class RecommendationsViewModel(
    private val mood: Mood,
    private val mode: ConversationMode,
    observeRecommendations: ObserveRecommendationsUseCase,
    observeSavedTracks: ObserveSavedTracksUseCase,
    private val refreshRecommendations: RefreshRecommendationsUseCase,
    private val loadCover: LoadCoverUseCase,
    private val saveTrack: SaveTrackUseCase,
    private val clock: Clock,
) : ViewModel() {

    private val refreshStatus = MutableStateFlow<RefreshStatus>(RefreshStatus.AwaitingData)
    private val isRefreshing = MutableStateFlow(false)
    private val coversInFlight = mutableSetOf<String>()

    private val _messages = Channel<Int>(Channel.BUFFERED)
    val messages: Flow<Int> = _messages.receiveAsFlow()

    private val recommendations: Flow<Recommendations> = observeRecommendations(mood, mode)

    val uiState: StateFlow<RecommendationsUiState> = combine(
        recommendations,
        refreshStatus,
        isRefreshing,
        observeSavedTracks().map { list -> list.map { it.track.key }.toSet() },
    ) { recs, status, refreshing, savedKeys ->
        RecommendationsUiState(
            mood = mood,
            mode = mode,
            content = contentFor(recs, status),
            isRefreshing = refreshing,
            savedKeys = savedKeys,
        )
    }
        .catch { emit(RecommendationsUiState(mood, mode, content = RecommendationsContent.Error(R.string.error_generic))) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), RecommendationsUiState(mood, mode))

    init {
        refresh(force = false)
    }

    /** Pull-to-refresh: busca mesmo com o cache válido (RF16). */
    fun onRefresh() {
        isRefreshing.value = true
        refresh(force = true, onFinished = { isRefreshing.value = false })
    }

    fun onRetry() = refresh(force = false)

    /** O card apareceu: busca a capa se ainda não foi consultada (RN28). */
    fun onTrackVisible(track: Track) {
        if (!coversInFlight.add(track.key)) return
        viewModelScope.launch {
            // Sem internet ou com falha, a capa fica pendente e é tentada de novo depois.
            if (loadCover(track) is AppResult.Failure) coversInFlight.remove(track.key)
        }
    }

    fun onSave(track: RecommendedTrack) {
        viewModelScope.launch {
            val message = when (val result = saveTrack(track)) {
                SaveTrackResult.Saved -> R.string.track_saved_message
                SaveTrackResult.AlreadySaved -> R.string.track_already_saved
                is SaveTrackResult.Failure -> ErrorMessageMapper.messageFor(result.error)
            }
            _messages.send(message)
        }
    }

    private fun refresh(force: Boolean, onFinished: () -> Unit = {}) {
        refreshStatus.value = RefreshStatus.AwaitingData
        viewModelScope.launch {
            when (val result = refreshRecommendations(mood, mode, force)) {
                is AppResult.Success -> {
                    // O banco avisa a tela dos dados novos de forma assíncrona: só marcamos
                    // "sem resultados" se ele estiver de fato vazio.
                    if (isCacheEmpty()) refreshStatus.value = RefreshStatus.NoResults
                }
                is AppResult.Failure -> {
                    refreshStatus.value = RefreshStatus.Failed(result.error)
                    // Com músicas na tela, o erro não bloqueia: aparece num Snackbar (CA18).
                    if (!isCacheEmpty()) _messages.send(ErrorMessageMapper.messageFor(result.error))
                }
            }
            onFinished()
        }
    }

    private suspend fun isCacheEmpty(): Boolean {
        val current = try {
            recommendations.first()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
        return current == null || (current.tracks.isEmpty() && current.updatedAt == null)
    }

    private fun contentFor(recs: Recommendations, status: RefreshStatus): RecommendationsContent = when {
        recs.tracks.isNotEmpty() -> RecommendationsContent.Success(
            tracks = recs.tracks,
            updatedAt = recs.updatedAt?.let { UpdatedAtFormatter.format(it, clock.instant()) },
        )
        // Há cache, mas todas as músicas saíram (ex.: todas já ouvidas) ou a busca não achou nada.
        recs.updatedAt != null -> RecommendationsContent.Empty(R.string.recommendations_empty, canRetry = false)
        else -> when (status) {
            RefreshStatus.AwaitingData -> RecommendationsContent.Loading
            RefreshStatus.NoResults -> RecommendationsContent.Empty(R.string.recommendations_empty, canRetry = false)
            is RefreshStatus.Failed ->
                if (status.error == AppError.Network) {
                    RecommendationsContent.Empty(R.string.recommendations_empty_offline, canRetry = true)
                } else {
                    RecommendationsContent.Error(ErrorMessageMapper.messageFor(status.error))
                }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
