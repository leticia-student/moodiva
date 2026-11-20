package com.leticia.moodiva.presentation.screens.saved

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leticia.moodiva.R
import com.leticia.moodiva.core.result.AppResult
import com.leticia.moodiva.domain.model.ListenStatus
import com.leticia.moodiva.domain.model.SavedTrack
import com.leticia.moodiva.domain.usecase.MarkAsListenedUseCase
import com.leticia.moodiva.domain.usecase.ObserveSavedTracksUseCase
import com.leticia.moodiva.domain.usecase.RateTrackResult
import com.leticia.moodiva.domain.usecase.RateTrackUseCase
import com.leticia.moodiva.domain.usecase.RemoveSavedTrackUseCase
import com.leticia.moodiva.presentation.util.ErrorMessageMapper
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Filtro por status (RF19). */
enum class SavedFilter(@StringRes val labelRes: Int, @StringRes val emptyRes: Int) {
    ALL(R.string.saved_filter_all, R.string.saved_empty),
    WANT_TO_LISTEN(R.string.saved_filter_want, R.string.saved_empty_filter_want),
    LISTENED(R.string.saved_filter_listened, R.string.saved_empty_filter_listened),
}

@Immutable
sealed interface SavedContent {
    data object Loading : SavedContent
    data class Success(val tracks: List<SavedTrack>) : SavedContent

    /** [showChatAction]: a lista toda está vazia, então oferece "Conversar com a Moodiva". */
    data class Empty(@StringRes val messageRes: Int, val showChatAction: Boolean) : SavedContent
    data class Error(@StringRes val messageRes: Int) : SavedContent
}

@Immutable
data class SavedUiState(
    val filter: SavedFilter = SavedFilter.ALL,
    val content: SavedContent = SavedContent.Loading,
    /** Música aguardando confirmação de remoção (RF26), ou `null`. */
    val pendingRemoval: SavedTrack? = null,
)

/**
 * Músicas salvas (RF19–RF21, RF26). Tudo local: funciona offline.
 */
class SavedViewModel(
    observeSavedTracks: ObserveSavedTracksUseCase,
    private val markAsListened: MarkAsListenedUseCase,
    private val rateTrack: RateTrackUseCase,
    private val removeSavedTrack: RemoveSavedTrackUseCase,
) : ViewModel() {

    private val filter = MutableStateFlow(SavedFilter.ALL)
    private val pendingRemoval = MutableStateFlow<SavedTrack?>(null)

    private val _messages = Channel<Int>(Channel.BUFFERED)
    val messages: Flow<Int> = _messages.receiveAsFlow()

    val uiState: StateFlow<SavedUiState> = combine(observeSavedTracks(), filter, pendingRemoval) { all, filter, removal ->
        val visible = when (filter) {
            SavedFilter.ALL -> all
            SavedFilter.WANT_TO_LISTEN -> all.filter { it.status == ListenStatus.WANT_TO_LISTEN }
            SavedFilter.LISTENED -> all.filter { it.status == ListenStatus.LISTENED }
        }
        SavedUiState(
            filter = filter,
            content = if (visible.isEmpty()) {
                SavedContent.Empty(filter.emptyRes, showChatAction = all.isEmpty())
            } else {
                SavedContent.Success(visible)
            },
            pendingRemoval = removal,
        )
    }
        .catch { emit(SavedUiState(content = SavedContent.Error(R.string.error_generic))) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), SavedUiState())

    fun onFilter(value: SavedFilter) {
        filter.value = value
    }

    fun onMarkListened(track: SavedTrack) {
        viewModelScope.launch {
            val result = markAsListened(track.track.key)
            if (result is AppResult.Failure) _messages.send(ErrorMessageMapper.messageFor(result.error))
        }
    }

    fun onRate(track: SavedTrack, rating: Int) {
        viewModelScope.launch {
            when (val result = rateTrack(track.track.key, rating)) {
                RateTrackResult.Rated, RateTrackResult.InvalidRating, RateTrackResult.NotListened -> Unit
                is RateTrackResult.Failure -> _messages.send(ErrorMessageMapper.messageFor(result.error))
            }
        }
    }

    fun onRequestRemove(track: SavedTrack) {
        pendingRemoval.value = track
    }

    fun onDismissRemove() {
        pendingRemoval.value = null
    }

    fun onConfirmRemove() {
        val track = pendingRemoval.value ?: return
        pendingRemoval.value = null
        viewModelScope.launch {
            val result = removeSavedTrack(track.track.key)
            if (result is AppResult.Failure) _messages.send(ErrorMessageMapper.messageFor(result.error))
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
