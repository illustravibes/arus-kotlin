package com.illustravibes.arus.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.time.LocalDate

class RecurringReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return try {
            if (!applicationContext.getSharedPreferences("arus_reminders", Context.MODE_PRIVATE)
                    .getBoolean("recurring_enabled", true)) return Result.success()
            val database = DatabaseFactory.open(applicationContext)
            val count = try {
                database.recurringDao().runDue(LocalDate.now().toEpochDay())
                database.recurringDao().pendingOccurrences().size
            } finally {
                database.close()
            }
            if (count > 0) showReminder(applicationContext, count)
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    private fun showReminder(context: Context, count: Int) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(NotificationChannel("recurring", "Transaksi rutin", NotificationManager.IMPORTANCE_DEFAULT))
        }
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        try {
            NotificationManagerCompat.from(context).notify(8401, NotificationCompat.Builder(context, "recurring")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Transaksi rutin menunggu konfirmasi")
                .setContentText("Ada $count occurrence yang perlu ditinjau di Arus.")
                .setAutoCancel(true).build())
        } catch (_: SecurityException) { }
    }
}
