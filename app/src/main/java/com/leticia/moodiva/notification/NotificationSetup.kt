package com.leticia.moodiva.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.leticia.moodiva.R
import com.leticia.moodiva.domain.usecase.CalculateNotificationDelayUseCase
import java.util.concurrent.TimeUnit

/**
 * Canal "Lembretes diários" (planejamento, seção 8).
 *
 * Desde o Android 8 toda notificação pertence a um canal, que o usuário pode silenciar.
 * Criar um canal que já existe não tem efeito, então é seguro chamar a cada abertura.
 */
object NotificationChannels {

    const val DAILY_REMINDER = "daily_reminder"

    fun create(context: Context) {
        // Antes do Android 8 (API 26) não existem canais: a notificação usa as configurações do app.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            DAILY_REMINDER,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = context.getString(R.string.notification_channel_description) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}

/**
 * Agenda o lembrete diário com o WorkManager (RF23, RN34).
 *
 * - Repete a cada 1 dia.
 * - A primeira execução espera até a próxima janela das 19h às 21h (cálculo do domain).
 * - `KEEP`: se já estiver agendado, o novo pedido é ignorado; abrir o app várias vezes
 *   não duplica o agendamento.
 *
 * Limitação: o WorkManager não garante horário exato (o sistema pode adiar para poupar bateria).
 */
class NotificationScheduler(
    private val workManager: WorkManager,
    private val calculateDelay: CalculateNotificationDelayUseCase,
) {

    fun schedule() {
        val request = PeriodicWorkRequestBuilder<DailyReminderWorker>(REPEAT_INTERVAL_DAYS, TimeUnit.DAYS)
            .setInitialDelay(calculateDelay().toMillis(), TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniquePeriodicWork(UNIQUE_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    companion object {
        const val UNIQUE_WORK_NAME = "daily_reminder"
        const val REPEAT_INTERVAL_DAYS = 1L
    }
}

/**
 * Lembra se o app já perguntou sobre a permissão de notificação (RN35).
 * Se o usuário disser "Agora não" ou negar, o app não pergunta de novo.
 */
class NotificationPermissionPrefs(context: Context) {

    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var wasAsked: Boolean
        get() = preferences.getBoolean(KEY_ASKED, false)
        set(value) = preferences.edit().putBoolean(KEY_ASKED, value).apply()

    private companion object {
        const val PREFS_NAME = "moodiva_prefs"
        const val KEY_ASKED = "notification_permission_asked"
    }
}
