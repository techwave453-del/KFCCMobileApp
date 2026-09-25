package com.example.helloworld.notifications

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object KfccNotificationScheduler {
    private const val PERIODIC_NAME = "kfcc_notification_poll"

    fun schedule(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<KfccNotificationWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()
        )
    }

    fun syncNow(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "kfcc_notification_sync_now",
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<KfccNotificationWorker>()
                .setConstraints(constraints)
                .build()
        )
    }
}
