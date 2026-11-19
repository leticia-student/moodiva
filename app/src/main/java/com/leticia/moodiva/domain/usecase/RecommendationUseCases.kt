package com.leticia.moodiva.domain.usecase

import com.leticia.moodiva.core.network.NetworkMonitor
import com.leticia.moodiva.core.result.AppError
import com.leticia.moodiva.core.result.AppResult
import com.leticia.moodiva.domain.model.ConversationMode
import com.leticia.moodiva.domain.model.ListenStatus
import com.leticia.moodiva.domain.model.Mood
import com.leticia.moodiva.domain.model.Recommendations
import com.leticia.moodiva.domain.model.Track
import com.leticia.moodiva.domain.repository.AuthRepository
import com.leticia.moodiva.domain.repository.MusicRepository
import com.leticia.moodiva.domain.repository.ProfileRepository
import com.leticia.moodiva.domain.repository.RecommendationRepository
import com.leticia.moodiva.domain.repository.SavedTrackRepository
import com.leticia.moodiva.domain.rule.CachePolicy
import com.leticia.moodiva.domain.rule.MoodTagMapper
import com.leticia.moodiva.domain.rule.RecommendationMerger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import java.time.Clock

/**
 * Recomendações em cache de (conta, humor, modo), sem as músicas já ouvidas (RN24).
 * Reage sozinho quando o cache muda ou quando uma música é marcada como ouvida.
 */
class ObserveRecommendationsUseCase(
    private val authRepository: AuthRepository,
    private val recommendationRepository: RecommendationRepository,
    private val savedTrackRepository: SavedTrackRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class) // flatMapLatest
    operator fun invoke(mood: Mood, mode: ConversationMode): Flow<Recommendations> =
        authRepository.session.flatMapLatest { user ->
            if (user == null) return@flatMapLatest flowOf(Recommendations(emptyList(), null))
            val effectiveMode = MoodTagMapper.effectiveMode(mood, mode)
            combine(
                recommendationRepository.observeRecommendations(user.id, mood, effectiveMode),
                savedTrackRepository.observeSavedTracks(user.id),
            ) { recommendations, saved ->
                val listened = saved.filter { it.status == ListenStatus.LISTENED }.map { it.track.key }.toSet()
                recommendations.copy(tracks = RecommendationMerger.withoutListened(recommendations.tracks, listened))
            }
        }
}

/**
 * Decide se busca recomendações novas (Offline First, planejamento seção 6).
 *
 * - Sem `force` e com cache válido (< 24 h, RN25): não chama a API.
 * - Sem conexão: falha com [AppError.Network], sem tocar no cache.
 * - Senão gera as recomendações e substitui o cache numa transação.
 *
 * @param force `true` no pull-to-refresh (RF16): ignora a validade do cache.
 */
class RefreshRecommendationsUseCase(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
    private val recommendationRepository: RecommendationRepository,
    private val generateRecommendations: GenerateRecommendationsUseCase,
    private val networkMonitor: NetworkMonitor,
    private val clock: Clock,
) {
    suspend operator fun invoke(mood: Mood, mode: ConversationMode, force: Boolean = false): AppResult<Unit> {
        val userId = authRepository.currentUserId() ?: return AppResult.Failure(AppError.NotSignedIn)
        val effectiveMode = MoodTagMapper.effectiveMode(mood, mode)

        if (!force) {
            val cached = recommendationRepository.observeRecommendations(userId, mood, effectiveMode).first()
            if (!CachePolicy.isExpired(cached.updatedAt, clock.instant())) return AppResult.Success(Unit)
        }
        if (!networkMonitor.isOnline.first()) return AppResult.Failure(AppError.Network)

        val profile = profileRepository.observeProfile(userId).first() ?: return AppResult.Failure(AppError.Unknown)
        return when (val generated = generateRecommendations(profile, mood, effectiveMode)) {
            is AppResult.Success -> recommendationRepository.replaceRecommendations(
                userId = userId,
                mood = mood,
                mode = effectiveMode,
                tracks = generated.data,
                updatedAt = clock.instant(),
            )
            is AppResult.Failure -> generated
        }
    }
}

/**
 * Busca a capa de uma música da fonte principal quando o card aparece (RN28).
 *
 * Só chama a API se a capa nunca foi consultada e houver conexão. O resultado vai para
 * o cache de capas (`null` = indisponível, RN27), e o Flow das recomendações o reflete.
 */
class LoadCoverUseCase(
    private val recommendationRepository: RecommendationRepository,
    private val musicRepository: MusicRepository,
    private val networkMonitor: NetworkMonitor,
) {
    suspend operator fun invoke(track: Track): AppResult<Unit> {
        if (recommendationRepository.isCoverChecked(track.key)) return AppResult.Success(Unit)
        if (!networkMonitor.isOnline.first()) return AppResult.Failure(AppError.Network)
        return when (val details = musicRepository.trackDetails(track)) {
            is AppResult.Success -> recommendationRepository.saveCover(track.key, details.data?.coverUrl)
            is AppResult.Failure -> details
        }
    }
}
