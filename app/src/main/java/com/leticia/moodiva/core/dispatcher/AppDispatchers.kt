package com.leticia.moodiva.core.dispatcher

import kotlinx.coroutines.CoroutineDispatcher

/**
 * Dispatchers usados pelo app, injetados via Koin.
 *
 * Em produção recebem `Dispatchers.IO`, `Dispatchers.Default` e `Dispatchers.Main`.
 * Nos testes, todos são substituídos por um `StandardTestDispatcher`, o que torna
 * a execução das coroutines determinística.
 */
data class AppDispatchers(
    val io: CoroutineDispatcher,
    val default: CoroutineDispatcher,
    val main: CoroutineDispatcher,
)
