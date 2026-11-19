package com.leticia.moodiva.core.network

import kotlinx.coroutines.flow.Flow

/**
 * Informa, em tempo real, se o aparelho tem conexão com a internet.
 *
 * É uma interface em Kotlin puro para que o domain possa usá-la sem conhecer
 * o Android. A implementação real (`AndroidNetworkMonitor`) fica neste mesmo
 * pacote; nos testes, usa-se uma versão falsa.
 */
interface NetworkMonitor {

    /** Emite `true` quando há conexão e `false` quando não há. */
    val isOnline: Flow<Boolean>
}
