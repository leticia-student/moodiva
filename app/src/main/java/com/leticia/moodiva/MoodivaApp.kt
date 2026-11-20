package com.leticia.moodiva

import android.app.Application
import com.leticia.moodiva.di.appModule
import com.leticia.moodiva.di.dataModule
import com.leticia.moodiva.di.domainModule
import com.leticia.moodiva.di.presentationModule
import com.leticia.moodiva.notification.NotificationChannels
import com.leticia.moodiva.notification.NotificationScheduler
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

/**
 * Ponto de entrada do processo do app: roda antes de qualquer tela.
 * 1. Inicia o Koin com todos os módulos de dependências.
 * 2. Cria o canal de notificação.
 * 3. Agenda o lembrete diário (KEEP: reabrir o app não duplica o agendamento).
 */
class MoodivaApp : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@MoodivaApp)
            modules(appModule, dataModule, domainModule, presentationModule)
        }
        NotificationChannels.create(this)
        get<NotificationScheduler>().schedule()
    }
}
