package com.illustravibes.arus.data

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

object RecurringReminderScheduler {
    private const val PREFERENCES = "arus_reminders"
    private const val ENABLED = "recurring_enabled"
    private const val HOUR = "recurring_hour"
    private const val MINUTE = "recurring_minute"

    fun schedule(context: Context) {
        val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        if (!preferences.getBoolean(ENABLED, true)) {
            WorkManager.getInstance(context).cancelUniqueWork("recurring-reminder")
            return
        }
        val now = LocalDateTime.now()
        var nextRun = now.toLocalDate().atTime(
            preferences.getInt(HOUR, 9).coerceIn(0, 23),
            preferences.getInt(MINUTE, 0).coerceIn(0, 59),
        )
        if (!nextRun.isAfter(now)) nextRun = nextRun.plusDays(1)
        val delay = Duration.between(now, nextRun).toMillis().coerceAtLeast(0L)
        val request = PeriodicWorkRequestBuilder<RecurringReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "recurring-reminder", ExistingPeriodicWorkPolicy.REPLACE, request,
        )
    }
}
