package com.leticia.moodiva

import android.app.Application
import com.leticia.moodiva.di.appModule
import com.leticia.moodiva.di.dataModule
import com.leticia.moodiva.di.domainModule
import com.leticia.moodiva.di.presentationModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

/**
 * Ponto de entrada do processo do app: roda antes de qualquer tela.
 * Inicia o Koin com todos os módulos de dependências.
 */
class MoodivaApp : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@MoodivaApp)
            modules(appModule, dataModule, domainModule, presentationModule)
        }
    }
}
