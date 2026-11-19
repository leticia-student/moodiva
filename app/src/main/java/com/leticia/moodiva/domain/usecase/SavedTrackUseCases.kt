package com.leticia.moodiva.domain.usecase

import com.leticia.moodiva.core.result.AppError
import com.leticia.moodiva.core.result.AppResult
import com.leticia.moodiva.domain.model.ListenStatus
import com.leticia.moodiva.domain.model.RecommendedTrack
import com.leticia.moodiva.domain.model.SavedTrack
import com.leticia.moodiva.domain.repository.AuthRepository
import com.leticia.moodiva.domain.repository.SavedTrackRepository
import com.leticia.moodiva.domain.rule.SavedTrackRules
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.Clock

/** Músicas salvas da conta logada, das mais recentes para as mais antigas (RF19). */
class ObserveSavedTracksUseCase(
    private val authRepository: AuthRepository,
    private val savedTrackRepository: SavedTrackRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class) // flatMapLatest
    operator fun invoke(): Flow<List<SavedTrack>> = authRepository.session.flatMapLatest { user ->
        if (user == null) flowOf(emptyList())
        else savedTrackRepository.observeSavedTracks(user.id).map { list -> list.sortedByDescending { it.savedAt } }
    }
}

sealed interface SaveTrackResult {
    data object Saved : SaveTrackResult
    data object AlreadySaved : SaveTrackResult
    data class Failure(val error: AppError) : SaveTrackResult
}

/** Salva uma recomendação como "Quero ouvir" (RF18). Nunca duplica (RN29). */
class SaveTrackUseCase(
    private val authRepository: AuthRepository,
    private val savedTrackRepository: SavedTrackRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(recommended: RecommendedTrack): SaveTrackResult {
        val userId = authRepository.currentUserId() ?: return SaveTrackResult.Failure(AppError.NotSignedIn)
        if (savedTrackRepository.getSavedTrack(userId, recommended.track.key) != null) return SaveTrackResult.AlreadySaved
        val saved = SavedTrack(
            track = recommended.track,
            coverUrl = recommended.coverUrl,
            source = recommended.source,
            status = ListenStatus.WANT_TO_LISTEN,
            rating = null,
            savedAt = clock.instant(),
        )
        return when (val result = savedTrackRepository.insert(userId, saved)) {
            is AppResult.Success -> SaveTrackResult.Saved
            is AppResult.Failure -> SaveTrackResult.Failure(result.error)
        }
    }
}

/** Marca como ouvida (RF20). A transição é de mão única (RN30); repetir não muda nada. */
class MarkAsListenedUseCase(
    private val authRepository: AuthRepository,
    private val savedTrackRepository: SavedTrackRepository,
) {
    suspend operator fun invoke(trackKey: String): AppResult<Unit> {
        val userId = authRepository.currentUserId() ?: return AppResult.Failure(AppError.NotSignedIn)
        val saved = savedTrackRepository.getSavedTrack(userId, trackKey) ?: return AppResult.Failure(AppError.Unknown)
        if (saved.status == ListenStatus.LISTENED) return AppResult.Success(Unit)
        return savedTrackRepository.update(userId, saved.copy(status = ListenStatus.LISTENED))
    }
}

sealed interface RateTrackResult {
    data object Rated : RateTrackResult
    data object InvalidRating : RateTrackResult
    data object NotListened : RateTrackResult
    data class Failure(val error: AppError) : RateTrackResult
}

/** Avalia de 1 a 5 estrelas uma música já ouvida (RF21, RN31). A nota pode ser trocada. */
class RateTrackUseCase(
    private val authRepository: AuthRepository,
    private val savedTrackRepository: SavedTrackRepository,
) {
    suspend operator fun invoke(trackKey: String, rating: Int): RateTrackResult {
        if (!SavedTrackRules.isValidRating(rating)) return RateTrackResult.InvalidRating
        val userId = authRepository.currentUserId() ?: return RateTrackResult.Failure(AppError.NotSignedIn)
        val saved = savedTrackRepository.getSavedTrack(userId, trackKey)
            ?: return RateTrackResult.Failure(AppError.Unknown)
        if (!SavedTrackRules.canRate(saved.status)) return RateTrackResult.NotListened
        return when (val result = savedTrackRepository.update(userId, saved.copy(rating = rating))) {
            is AppResult.Success -> RateTrackResult.Rated
            is AppResult.Failure -> RateTrackResult.Failure(result.error)
        }
    }
}

/** Remove uma música da lista (RF26). A confirmação é pedida pela tela. */
class RemoveSavedTrackUseCase(
    private val authRepository: AuthRepository,
    private val savedTrackRepository: SavedTrackRepository,
) {
    suspend operator fun invoke(trackKey: String): AppResult<Unit> {
        val userId = authRepository.currentUserId() ?: return AppResult.Failure(AppError.NotSignedIn)
        return savedTrackRepository.delete(userId, trackKey)
    }
}
