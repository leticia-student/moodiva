package com.leticia.moodiva.domain.rule

import com.leticia.moodiva.domain.model.CredentialProblem
import com.leticia.moodiva.domain.model.ListenStatus
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime

/** Regras das músicas salvas (RN30, RN31). */
object SavedTrackRules {

    const val MIN_RATING = 1
    const val MAX_RATING = 5

    fun isValidRating(rating: Int): Boolean = rating in MIN_RATING..MAX_RATING

    /** Só músicas já ouvidas podem ser avaliadas. */
    fun canRate(status: ListenStatus): Boolean = status == ListenStatus.LISTENED
}

/**
 * Validação de e-mail e senha (RF01, RF02).
 *
 * - E-mail: obrigatório, no formato `algo@dominio.tld`, comparado sem diferença de
 *   maiúsculas e sem espaços nas pontas (RN32).
 * - Senha: obrigatória, com pelo menos [MIN_PASSWORD_LENGTH] caracteres.
 * - Confirmação (só no cadastro): igual à senha.
 */
object CredentialRules {

    const val MIN_PASSWORD_LENGTH = 6
    private val EMAIL_PATTERN = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

    fun normalizeEmail(email: String): String = email.trim().lowercase()

    fun validateSignIn(email: String, password: String): Set<CredentialProblem> =
        emailProblems(email) + if (password.isEmpty()) setOf(CredentialProblem.PASSWORD_BLANK) else emptySet()

    fun validateSignUp(email: String, password: String, confirmation: String): Set<CredentialProblem> = buildSet {
        addAll(emailProblems(email))
        when {
            password.isEmpty() -> add(CredentialProblem.PASSWORD_BLANK)
            password.length < MIN_PASSWORD_LENGTH -> add(CredentialProblem.PASSWORD_TOO_SHORT)
        }
        if (password.isNotEmpty() && confirmation != password) add(CredentialProblem.CONFIRMATION_MISMATCH)
    }

    private fun emailProblems(email: String): Set<CredentialProblem> {
        val normalized = normalizeEmail(email)
        return when {
            normalized.isEmpty() -> setOf(CredentialProblem.EMAIL_BLANK)
            !EMAIL_PATTERN.matches(normalized) -> setOf(CredentialProblem.EMAIL_INVALID)
            else -> emptySet()
        }
    }
}

/**
 * Validade do cache de recomendações: 24 horas (RN25).
 *
 * Bordas: nunca buscado (`null`) = expirado; idade **≥** 24 h = expirado;
 * `updatedAt` no futuro (relógio do aparelho voltou) = expirado, para corrigir o cache.
 */
object CachePolicy {

    val VALIDITY: Duration = Duration.ofHours(24)

    fun isExpired(updatedAt: Instant?, now: Instant): Boolean {
        if (updatedAt == null || updatedAt.isAfter(now)) return true
        return Duration.between(updatedAt, now) >= VALIDITY
    }
}

/**
 * Janela diária do lembrete: das 19h às 21h (RN34).
 *
 * - Antes das 19h: espera até as 19h do mesmo dia.
 * - Das 19h até antes das 21h: já está na janela, atraso zero.
 * - A partir das 21h: espera até as 19h do dia seguinte.
 */
object NotificationWindow {

    val WINDOW_START: LocalTime = LocalTime.of(19, 0)
    val WINDOW_END: LocalTime = LocalTime.of(21, 0)

    fun delayUntilNextWindow(now: LocalDateTime): Duration {
        val time = now.toLocalTime()
        val nextStart = when {
            time < WINDOW_START -> now.toLocalDate().atTime(WINDOW_START)
            time < WINDOW_END -> return Duration.ZERO
            else -> now.toLocalDate().plusDays(1).atTime(WINDOW_START)
        }
        return Duration.between(now, nextStart)
    }
}
