package com.leticia.moodiva.data.repository

import com.leticia.moodiva.core.result.AppError
import com.leticia.moodiva.core.result.AppResult
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException

/**
 * Executa uma chamada de rede e converte qualquer exceção em [AppError] (planejamento, 7.1).
 *
 * A ordem dos `catch` importa:
 * - [CancellationException] é relançada: cancelar a coroutine (ex.: sair da tela) não é erro,
 *   e engoli-la quebraria o cancelamento estruturado.
 * - [IOException]: sem conexão, timeout ou DNS.
 * - [HttpException]: resposta 4xx/5xx. O 403 do Last.fm significa chave inválida.
 * - [SerializationException]: JSON que não pôde ser convertido.
 */
suspend fun <T> safeApiCall(call: suspend () -> T): AppResult<T> =
    try {
        AppResult.Success(call())
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        AppResult.Failure(AppError.Network)
    } catch (e: HttpException) {
        AppResult.Failure(if (e.code() == HTTP_FORBIDDEN) AppError.InvalidApiKey else AppError.Http(e.code()))
    } catch (e: SerializationException) {
        AppResult.Failure(AppError.Serialization)
    } catch (e: Exception) {
        AppResult.Failure(AppError.Unknown)
    }

/**
 * Executa uma operação no banco e converte falhas (disco cheio, chave repetida…)
 * em [AppError.Unknown]. Como em [safeApiCall], a [CancellationException] é relançada.
 */
suspend fun <T> safeDbCall(call: suspend () -> T): AppResult<T> =
    try {
        AppResult.Success(call())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        AppResult.Failure(AppError.Unknown)
    }

/** Código de erro do Last.fm (no corpo do JSON) → [AppError]. */
fun lastFmError(code: Int): AppError = when (code) {
    10, 26 -> AppError.InvalidApiKey // chave inválida ou suspensa
    29 -> AppError.RateLimited
    else -> AppError.Api(code)
}

private const val HTTP_FORBIDDEN = 403
