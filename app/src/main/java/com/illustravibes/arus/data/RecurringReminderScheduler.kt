package com.illustravibes.arus.data

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object RecurringReminderScheduler {
    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<RecurringReminderWorker>(1, TimeUnit.DAYS).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "recurring-reminder", ExistingPeriodicWorkPolicy.KEEP, request,
        )
    }
}
