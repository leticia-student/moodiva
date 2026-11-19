package com.leticia.moodiva.domain.model

import com.leticia.moodiva.core.result.AppError

/** Conta local do aparelho. O [id] é um UUID gerado no cadastro (RN32). */
data class User(
    val id: String,
    val email: String,
)

/** Resultado do cadastro (RF01): recusas de negócio separadas de falhas técnicas. */
sealed interface SignUpResult {
    data class Success(val user: User) : SignUpResult
    data object EmailAlreadyUsed : SignUpResult
    data class Invalid(val problems: Set<CredentialProblem>) : SignUpResult
    data class Failure(val error: AppError) : SignUpResult
}

/** Resultado do login (RF02). */
sealed interface SignInResult {
    data class Success(val user: User) : SignInResult
    data object InvalidCredentials : SignInResult
    data class Invalid(val problems: Set<CredentialProblem>) : SignInResult
    data class Failure(val error: AppError) : SignInResult
}

/** Problemas de preenchimento detectados pela `CredentialRules` (RF01). */
enum class CredentialProblem {
    EMAIL_BLANK,
    EMAIL_INVALID,
    PASSWORD_BLANK,
    PASSWORD_TOO_SHORT,
    CONFIRMATION_MISMATCH,
}
