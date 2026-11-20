package com.leticia.moodiva.notification

import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.leticia.moodiva.domain.usecase.CalculateNotificationDelayUseCase
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit

class NotificationSchedulerTest {

    private val workManager = mockk<WorkManager>(relaxed = true)

    private fun clockAt(hour: Int) = Clock.fixed(
        LocalDateTime.of(2026, 11, 19, hour, 0).toInstant(ZoneOffset.UTC),
        ZoneOffset.UTC,
    )

    @Test
    fun `deve agendar diariamente com KEEP e atraso ate as dezenove`() {
        val request = slot<PeriodicWorkRequest>()

        NotificationScheduler(workManager, CalculateNotificationDelayUseCase(clockAt(15))).schedule()

        verify {
            workManager.enqueueUniquePeriodicWork(
                NotificationScheduler.UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                capture(request),
            )
        }
        val spec = request.captured.workSpec
        assertEquals(TimeUnit.DAYS.toMillis(1), spec.intervalDuration)
        assertEquals(Duration.ofHours(4).toMillis(), spec.initialDelay)
        assertEquals(DailyReminderWorker::class.java.name, spec.workerClassName)
    }

    @Test
    fun `dentro da janela o primeiro lembrete nao deve esperar`() {
        val request = slot<PeriodicWorkRequest>()
        NotificationScheduler(workManager, CalculateNotificationDelayUseCase(clockAt(20))).schedule()
        verify { workManager.enqueueUniquePeriodicWork(any(), any(), capture(request)) }
        assertEquals(0L, request.captured.workSpec.initialDelay)
    }
}
