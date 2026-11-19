package com.leticia.moodiva.domain.usecase

import com.leticia.moodiva.core.result.AppError
import com.leticia.moodiva.core.result.AppResult
import com.leticia.moodiva.domain.model.PersonalityProfile
import com.leticia.moodiva.domain.model.UserProfile
import com.leticia.moodiva.domain.repository.AuthRepository
import com.leticia.moodiva.domain.repository.ProfileRepository
import com.leticia.moodiva.domain.repository.RecommendationRepository
import com.leticia.moodiva.domain.rule.TipiCalculator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf

/** Perfil da conta logada; `null` enquanto o onboarding não foi concluído (RN33). */
class ObserveProfileUseCase(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class) // flatMapLatest
    operator fun invoke(): Flow<UserProfile?> = authRepository.session.flatMapLatest { user ->
        if (user == null) flowOf(null) else profileRepository.observeProfile(user.id)
    }
}

/** Calcula os traços do TIPI (RN01–RN04); `null` se as respostas forem incompletas ou inválidas. */
class CalculatePersonalityUseCase {
    operator fun invoke(answers: List<Int>): PersonalityProfile? = TipiCalculator.calculate(answers)
}

/**
 * Salva o perfil da conta logada (RF05–RF12).
 *
 * Depois de salvar, descarta o cache de recomendações da conta (RN26): as próximas
 * recomendações já refletem o novo perfil. Se só a limpeza falhar, o perfil continua
 * salvo e o cache antigo vence em até 24 h, então o resultado ainda é sucesso.
 */
class SaveProfileUseCase(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
    private val recommendationRepository: RecommendationRepository,
) {
    suspend operator fun invoke(profile: UserProfile): AppResult<Unit> {
        val userId = authRepository.currentUserId() ?: return AppResult.Failure(AppError.NotSignedIn)
        val result = profileRepository.saveProfile(userId, profile)
        if (result is AppResult.Success) recommendationRepository.clearRecommendations(userId)
        return result
    }
}
