package com.leticia.moodiva.notification

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.leticia.moodiva.MainActivity
import com.leticia.moodiva.R

/**
 * Publica o lembrete "Como você está hoje?" (RF23).
 *
 * Não contém regra de negócio: o horário vem do domain e o agendamento fica no
 * [NotificationScheduler]. Sem permissão ou com notificações desligadas, apenas
 * termina com sucesso, sem notificar (RN35).
 */
class DailyReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (canNotify()) showNotification()
        return Result.success()
    }

    private fun canNotify(): Boolean {
        val permissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        return permissionGranted && NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()
    }

    // A permissão já foi conferida em canNotify(); o lint não consegue enxergar isso.
    @SuppressLint("MissingPermission")
    private fun showNotification() {
        // Tocar na notificação abre o app do zero, na conversa (CA33).
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val text = applicationContext.getString(R.string.notification_text)
        val notification = NotificationCompat.Builder(applicationContext, NotificationChannels.DAILY_REMINDER)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(applicationContext.getString(R.string.notification_title))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val NOTIFICATION_ID = 1001
    }
}
