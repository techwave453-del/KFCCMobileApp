package com.example.helloworld.data.offline

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object KfccContentSyncScheduler {
    private const val PERIODIC_NAME = "kfcc_content_sync"

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<KfccContentSyncWorker>(6, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun syncNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<KfccContentSyncWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            PERIODIC_NAME + "_now",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}
