package com.leticia.moodiva.core.result

/**
 * Falhas técnicas (de infraestrutura) que uma operação pode ter (planejamento, seção 7.1).
 *
 * Recusas de regra de negócio (por exemplo, música já salva ou e-mail já cadastrado)
 * não entram aqui: os casos de uso têm tipos de resultado próprios para elas.
 */
sealed interface AppError {

    /** Sem conexão, timeout ou falha de DNS. */
    data object Network : AppError

    /** A chave do Last.fm está ausente ou é inválida (HTTP 403, erro 10 ou 26). */
    data object InvalidApiKey : AppError

    /** O Last.fm recusou por excesso de requisições (erro 29). */
    data object RateLimited : AppError

    /** O servidor respondeu com um código HTTP de erro (4xx ou 5xx). */
    data class Http(val code: Int) : AppError

    /** O Last.fm respondeu com um código de erro no corpo do JSON. */
    data class Api(val code: Int) : AppError

    /** A resposta chegou, mas não pôde ser convertida para os modelos do app. */
    data object Serialization : AppError

    /** Não há usuário logado para uma operação que depende da conta. */
    data object NotSignedIn : AppError

    /** Qualquer outra falha inesperada. */
    data object Unknown : AppError
}
