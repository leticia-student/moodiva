package com.leticia.moodiva.core.result

/**
 * Resultado de uma operação que pode falhar (rede, banco, leitura de dados).
 *
 * Os repositórios devolvem [AppResult] em vez de lançar exceções: assim o erro
 * vira um valor comum, e o `when` sobre esta interface selada obriga quem chama
 * a tratar tanto o sucesso quanto a falha.
 */
sealed interface AppResult<out T> {

    data class Success<out T>(val data: T) : AppResult<T>

    data class Failure(val error: AppError) : AppResult<Nothing>
}
