package com.leticia.moodiva.presentation.screens.onboarding

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leticia.moodiva.core.result.AppResult
import com.leticia.moodiva.domain.model.Era
import com.leticia.moodiva.domain.model.Genre
import com.leticia.moodiva.domain.model.OriginPreference
import com.leticia.moodiva.domain.model.PersonalityProfile
import com.leticia.moodiva.domain.model.TasteProfile
import com.leticia.moodiva.domain.model.UserProfile
import com.leticia.moodiva.domain.rule.TextNormalizer
import com.leticia.moodiva.domain.rule.TipiCalculator
import com.leticia.moodiva.domain.usecase.CalculatePersonalityUseCase
import com.leticia.moodiva.domain.usecase.ObserveProfileUseCase
import com.leticia.moodiva.domain.usecase.SaveProfileUseCase
import com.leticia.moodiva.presentation.util.ErrorMessageMapper
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Etapas do onboarding, na ordem do modo `FULL`. */
enum class OnboardingStep { INTRO, TIPI, GENRES, ARTISTS, ERAS, ORIGIN, RESULT }

@Immutable
data class OnboardingUiState(
    val mode: OnboardingMode,
    val step: OnboardingStep,
    val tipiIndex: Int = 0,
    val answers: List<Int?> = List(TipiCalculator.ITEM_COUNT) { null },
    val genres: Set<Genre> = emptySet(),
    val artists: List<String> = emptyList(),
    val artistInput: String = "",
    val eras: Set<Era> = emptySet(),
    val origin: OriginPreference? = null,
    val result: PersonalityProfile? = null,
    val isSaving: Boolean = false,
    @StringRes val errorRes: Int? = null,
) {
    val steps: List<OnboardingStep> get() = stepsFor(mode)

    /** Progresso de 0 a 1: cada item do TIPI conta como uma etapa. */
    val progress: Float
        get() {
            val units = steps.sumOf { if (it == OnboardingStep.TIPI) TipiCalculator.ITEM_COUNT else 1 }
            val done = steps.takeWhile { it != step }
                .sumOf { if (it == OnboardingStep.TIPI) TipiCalculator.ITEM_COUNT else 1 } +
                if (step == OnboardingStep.TIPI) tipiIndex else 0
            return done.toFloat() / units
        }

    val canAddArtist: Boolean
        get() = artistInput.isNotBlank() && artists.size < MAX_ARTISTS

    /** RF07–RF10: pode avançar da etapa atual? */
    val canContinue: Boolean
        get() = when (step) {
            OnboardingStep.GENRES -> genres.isNotEmpty()
            OnboardingStep.ARTISTS -> artists.size in 1..MAX_ARTISTS
            OnboardingStep.ORIGIN -> origin != null
            OnboardingStep.TIPI -> answers[tipiIndex] != null
            else -> true
        }

    companion object {
        const val MAX_ARTISTS = 5

        fun stepsFor(mode: OnboardingMode): List<OnboardingStep> = when (mode) {
            OnboardingMode.FULL -> OnboardingStep.entries
            OnboardingMode.RETAKE_TEST -> listOf(OnboardingStep.INTRO, OnboardingStep.TIPI, OnboardingStep.RESULT)
            OnboardingMode.EDIT_TASTE -> listOf(
                OnboardingStep.GENRES, OnboardingStep.ARTISTS, OnboardingStep.ERAS, OnboardingStep.ORIGIN,
            )
        }
    }
}

/**
 * Onboarding (RF04–RF12): teste TIPI e gostos musicais.
 *
 * - `FULL`: na primeira vez. O perfil só é salvo no fim; a raiz do app então leva à conversa.
 * - `RETAKE_TEST` e `EDIT_TASTE`: abertos pelo perfil, partem do perfil atual e, ao salvar,
 *   emitem [closeEvents] para a tela voltar. Salvar descarta o cache de recomendações (RN26).
 */
class OnboardingViewModel(
    private val mode: OnboardingMode,
    private val observeProfile: ObserveProfileUseCase,
    private val calculatePersonality: CalculatePersonalityUseCase,
    private val saveProfile: SaveProfileUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState(mode = mode, step = OnboardingUiState.stepsFor(mode).first()))
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _closeEvents = Channel<Unit>(Channel.BUFFERED)
    val closeEvents: Flow<Unit> = _closeEvents.receiveAsFlow()

    /** Perfil atual (modos de edição), usado como ponto de partida e para completar o que não muda. */
    private var currentProfile: UserProfile? = null

    init {
        if (mode != OnboardingMode.FULL) {
            viewModelScope.launch {
                currentProfile = observeProfile().first()
                currentProfile?.taste?.let { taste ->
                    _uiState.update {
                        it.copy(genres = taste.genres, artists = taste.favoriteArtists, eras = taste.eras, origin = taste.origin)
                    }
                }
            }
        }
    }

    // ---------- TIPI ----------

    /** Responde o item atual e avança sozinho para o próximo (RF05). */
    fun onAnswer(value: Int) {
        if (!TipiCalculator.isValidAnswer(value)) return
        val state = _uiState.value
        val answers = state.answers.toMutableList().also { it[state.tipiIndex] = value }
        if (state.tipiIndex < TipiCalculator.ITEM_COUNT - 1) {
            _uiState.update { it.copy(answers = answers, tipiIndex = it.tipiIndex + 1) }
        } else {
            // RN02: só conclui com as 10 respostas; o cálculo devolve null se faltar alguma.
            val result = calculatePersonality(answers.filterNotNull())
            _uiState.update { it.copy(answers = answers, result = result) }
            if (result != null) goToNextStep() else _uiState.update { it.copy(tipiIndex = answers.indexOfFirst { a -> a == null }) }
        }
    }

    // ---------- Gostos ----------

    fun onToggleGenre(genre: Genre) = _uiState.update {
        it.copy(genres = if (genre in it.genres) it.genres - genre else it.genres + genre)
    }

    fun onArtistInputChange(value: String) = _uiState.update { it.copy(artistInput = value) }

    /** Adiciona o artista digitado, sem repetir nomes iguais após normalização (RN15). */
    fun onAddArtist() = _uiState.update { state ->
        val name = state.artistInput.trim().replace(Regex("\\s+"), " ")
        val alreadyAdded = state.artists.any { TextNormalizer.normalize(it) == TextNormalizer.normalize(name) }
        if (name.isEmpty() || state.artists.size >= OnboardingUiState.MAX_ARTISTS) return@update state
        state.copy(artists = if (alreadyAdded) state.artists else state.artists + name, artistInput = "")
    }

    fun onRemoveArtist(name: String) = _uiState.update { it.copy(artists = it.artists - name) }

    fun onToggleEra(era: Era) = _uiState.update {
        it.copy(eras = if (era in it.eras) it.eras - era else it.eras + era)
    }

    fun onSelectOrigin(origin: OriginPreference) = _uiState.update { it.copy(origin = origin) }

    // ---------- Navegação entre etapas ----------

    /** Botão principal da etapa: avança ou, na última, salva. */
    fun onContinue() {
        val state = _uiState.value
        if (!state.canContinue) return
        val isLast = state.steps.last() == state.step
        if (isLast) save() else goToNextStep()
    }

    /**
     * Voltar dentro do onboarding. Devolve `false` quando já está na primeira tela,
     * para a navegação do app decidir (fechar a edição, por exemplo).
     */
    fun onBack(): Boolean {
        val state = _uiState.value
        if (state.step == OnboardingStep.TIPI && state.tipiIndex > 0) {
            _uiState.update { it.copy(tipiIndex = it.tipiIndex - 1) }
            return true
        }
        val index = state.steps.indexOf(state.step)
        if (index <= 0) return false
        val previous = state.steps[index - 1]
        _uiState.update {
            it.copy(
                step = previous,
                tipiIndex = if (previous == OnboardingStep.TIPI) TipiCalculator.ITEM_COUNT - 1 else it.tipiIndex,
                errorRes = null,
            )
        }
        return true
    }

    private fun goToNextStep() = _uiState.update { state ->
        val index = state.steps.indexOf(state.step)
        state.copy(step = state.steps.getOrElse(index + 1) { state.step }, errorRes = null)
    }

    private fun save() {
        val state = _uiState.value
        if (state.isSaving) return
        val profile = buildProfile(state) ?: return
        _uiState.update { it.copy(isSaving = true, errorRes = null) }
        viewModelScope.launch {
            when (val result = saveProfile(profile)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(isSaving = false) }
                    // No modo FULL, a raiz do app troca de tela sozinha quando o perfil aparece.
                    if (mode != OnboardingMode.FULL) _closeEvents.send(Unit)
                }
                is AppResult.Failure -> _uiState.update {
                    it.copy(isSaving = false, errorRes = ErrorMessageMapper.messageFor(result.error))
                }
            }
        }
    }

    private fun buildProfile(state: OnboardingUiState): UserProfile? {
        val personality = state.result ?: currentProfile?.personality ?: return null
        val taste = when (mode) {
            OnboardingMode.RETAKE_TEST -> currentProfile?.taste ?: return null
            else -> TasteProfile(
                genres = state.genres,
                favoriteArtists = state.artists,
                eras = state.eras,
                origin = state.origin ?: return null,
            )
        }
        return UserProfile(personality, taste)
    }
}
