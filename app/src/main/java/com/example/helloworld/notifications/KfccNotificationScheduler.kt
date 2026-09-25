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
    private const val SIGN_UP_WORK = "kfcc_sign_up_notification"
    const val KEY_MODE = "notification_mode"
    const val KEY_USERNAME = "notification_username"
    const val MODE_SIGN_IN = "sign_in"
    const val MODE_SIGN_UP = "sign_up"

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
        enqueue(context, null, requireNetwork = true)
    }

    fun deliverSignInDefault(context: Context) {
        enqueue(context, MODE_SIGN_IN, requireNetwork = true)
    }

    fun deliverSignupWelcome(context: Context, username: String) {
        val data = Data.Builder()
            .putString(KEY_MODE, MODE_SIGN_UP)
            .putString(KEY_USERNAME, username)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            SIGN_UP_WORK,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<KfccNotificationWorker>()
                .setInputData(data)
                .build()
        )
    }

    private fun enqueue(context: Context, mode: String?, requireNetwork: Boolean) {
        val builder = OneTimeWorkRequestBuilder<KfccNotificationWorker>()
        if (mode != null) {
            builder.setInputData(Data.Builder().putString(KEY_MODE, mode).build())
        }
        if (requireNetwork) {
            builder.setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
        }

        WorkManager.getInstance(context).enqueueUniqueWork(
            when (mode) {
                MODE_SIGN_IN -> SIGN_IN_WORK
                MODE_SIGN_UP -> SIGN_UP_WORK
                else -> "kfcc_notification_sync_now"
            },
            ExistingWorkPolicy.REPLACE,
            builder.build()
        )
    }
}
