package com.leticia.moodiva.di

import com.leticia.moodiva.core.dispatcher.AppDispatchers
import com.leticia.moodiva.core.network.AndroidNetworkMonitor
import com.leticia.moodiva.core.network.NetworkMonitor
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import java.time.Clock

/**
 * Dependências de infraestrutura usadas por várias camadas.
 * Todas são `single`: uma única instância durante toda a vida do app.
 */
val appModule = module {
    single {
        AppDispatchers(
            io = Dispatchers.IO,
            default = Dispatchers.Default,
            main = Dispatchers.Main,
        )
    }
    single<Clock> { Clock.systemDefaultZone() }
    single<NetworkMonitor> { AndroidNetworkMonitor(androidContext()) }
}
