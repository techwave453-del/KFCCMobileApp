package com.example.helloworld.notifications

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object KfccNotificationScheduler {
    private const val PERIODIC_NAME = "kfcc_notification_poll"
    private const val SIGN_IN_WORK = "kfcc_sign_in_notification"
    const val KEY_MODE = "notification_mode"
    const val MODE_SIGN_IN = "sign_in"

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
        enqueue(context, null)
    }

    fun deliverSignInDefault(context: Context) {
        enqueue(context, MODE_SIGN_IN)
    }

    private fun enqueue(context: Context, mode: String?) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val builder = OneTimeWorkRequestBuilder<KfccNotificationWorker>()
        if (mode != null) {
            builder.setInputData(Data.Builder().putString(KEY_MODE, mode).build())
        }
        WorkManager.getInstance(context).enqueueUniqueWork(
            if (mode == MODE_SIGN_IN) SIGN_IN_WORK else "kfcc_notification_sync_now",
            ExistingWorkPolicy.REPLACE,
            builder.setConstraints(constraints).build()
        )
    }
}
