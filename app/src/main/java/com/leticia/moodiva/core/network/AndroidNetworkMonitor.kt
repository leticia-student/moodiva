package com.leticia.moodiva.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Implementação Android do [NetworkMonitor].
 *
 * Usa o `ConnectivityManager` para acompanhar a rede padrão do aparelho e
 * converte os callbacks do sistema em um [Flow] com `callbackFlow`.
 * Só considera "online" a rede que o sistema validou como tendo acesso real
 * à internet (`NET_CAPABILITY_VALIDATED`), e não apenas um Wi-Fi conectado.
 */
class AndroidNetworkMonitor(context: Context) : NetworkMonitor {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    override val isOnline: Flow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities,
            ) {
                trySend(networkCapabilities.hasValidatedInternet())
            }

            override fun onLost(network: Network) {
                trySend(false)
            }
        }

        // Emite o estado atual logo de início: o primeiro callback pode demorar.
        trySend(isCurrentlyOnline())
        connectivityManager.registerDefaultNetworkCallback(callback)

        // Ao parar de coletar, remove o callback para não vazar memória.
        awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
    }
        .distinctUntilChanged()
        .conflate()

    private fun isCurrentlyOnline(): Boolean {
        val capabilities = connectivityManager
            .getNetworkCapabilities(connectivityManager.activeNetwork)
        return capabilities?.hasValidatedInternet() ?: false
    }

    private fun NetworkCapabilities.hasValidatedInternet(): Boolean =
        hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}
