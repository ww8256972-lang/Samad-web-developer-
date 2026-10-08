package com.example.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.R
import com.example.database.AppDatabase
import java.util.Calendar
import java.util.concurrent.TimeUnit

class FollowUpCheckWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val database = AppDatabase.getDatabase(appContext)
            val settings = database.settingsDao().getSettings()
            if (settings?.enableNotifications == false) {
                return Result.success()
            }

            val now = System.currentTimeMillis()
            val overdueList = database.customerDao().getOverdueCustomers(now)

            if (overdueList.isNotEmpty()) {
                showNotification(
                    title = "Overdue Follow-ups: ${overdueList.size} Customers",
                    message = "${overdueList.first().customerName} and ${overdueList.size - 1} other inquiry follow-ups are pending attention!"
                )
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun showNotification(title: String, message: String) {
        val channelId = "web_record_followups"
        val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Inquiry Follow-up Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminders for scheduled customer follow-up dates and overdue tasks"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(appContext, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1001, notification)
    }

    companion object {
        private const val WORK_NAME = "web_record_followup_periodic_worker"

        fun schedule(context: Context) {
            val workRequest = PeriodicWorkRequestBuilder<FollowUpCheckWorker>(
                15, TimeUnit.MINUTES
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }
    }
}
