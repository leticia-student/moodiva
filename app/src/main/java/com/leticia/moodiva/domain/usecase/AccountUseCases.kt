package com.leticia.moodiva.domain.usecase

import com.leticia.moodiva.core.network.NetworkMonitor
import com.leticia.moodiva.domain.model.SignInResult
import com.leticia.moodiva.domain.model.SignUpResult
import com.leticia.moodiva.domain.model.User
import com.leticia.moodiva.domain.repository.AuthRepository
import com.leticia.moodiva.domain.rule.CredentialRules
import com.leticia.moodiva.domain.rule.NotificationWindow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime

/** Usuário logado, ou `null` (RF02, RF04). */
class ObserveSessionUseCase(private val authRepository: AuthRepository) {
    operator fun invoke(): Flow<User?> = authRepository.session
}

/** Cadastro local (RF01): valida os campos antes de chegar ao repositório. */
class SignUpUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(email: String, password: String, confirmation: String): SignUpResult {
        val problems = CredentialRules.validateSignUp(email, password, confirmation)
        if (problems.isNotEmpty()) return SignUpResult.Invalid(problems)
        return authRepository.signUp(CredentialRules.normalizeEmail(email), password)
    }
}

/** Login local (RF02). */
class SignInUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(email: String, password: String): SignInResult {
        val problems = CredentialRules.validateSignIn(email, password)
        if (problems.isNotEmpty()) return SignInResult.Invalid(problems)
        return authRepository.signIn(CredentialRules.normalizeEmail(email), password)
    }
}

/** Logout (RF03). Os dados da conta continuam no aparelho, isolados pelo uid (RNF08). */
class SignOutUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke() = authRepository.signOut()
}

/** Estado da conexão para o banner "sem internet" (RF22). */
class ObserveConnectivityUseCase(private val networkMonitor: NetworkMonitor) {
    operator fun invoke(): Flow<Boolean> = networkMonitor.isOnline
}

/** Atraso até a próxima janela do lembrete diário (RN34), com o [Clock] injetado. */
class CalculateNotificationDelayUseCase(private val clock: Clock) {
    operator fun invoke(): Duration = NotificationWindow.delayUntilNextWindow(LocalDateTime.now(clock))
}

/** uid do usuário logado no momento, ou `null`. */
internal suspend fun AuthRepository.currentUserId(): String? = session.first()?.id
