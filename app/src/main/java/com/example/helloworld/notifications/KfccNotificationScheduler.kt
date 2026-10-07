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
import java.time.Duration
import java.time.LocalDateTime

object KfccNotificationScheduler {
    private const val PERIODIC_NAME = "kfcc_notification_poll"
    private const val SIGN_IN_WORK = "kfcc_sign_in_notification"
    private const val INSTALL_WORK = "kfcc_install_notification"
    private const val SIGN_UP_WORK = "kfcc_sign_up_notification"
    private const val DAILY_SCRIPTURE_WORK = "kfcc_daily_scripture_notification"
    private const val DAILY_SCRIPTURE_NOW_WORK = "kfcc_daily_scripture_now"
    const val KEY_MODE = "notification_mode"
    const val KEY_USERNAME = "notification_username"
    const val MODE_SIGN_IN = "sign_in"
    const val MODE_INSTALL = "install"
    const val MODE_SIGN_UP = "sign_up"
    const val MODE_DAILY_SCRIPTURE = "daily_scripture"

    private fun connectedConstraints() = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    fun schedule(context: Context) {
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<KfccNotificationWorker>(15, TimeUnit.MINUTES)
                .setConstraints(connectedConstraints())
                .build()
        )

        scheduleInstallDelivery(context)
        scheduleDailyScripture(context)
    }

    fun scheduleInstallDelivery(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            INSTALL_WORK,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<KfccNotificationWorker>()
                .setInputData(Data.Builder().putString(KEY_MODE, MODE_INSTALL).build())
                .setConstraints(connectedConstraints())
                .build()
        )
    }

    fun scheduleDailyScripture(context: Context) {
        val now = LocalDateTime.now()
        var next = now.withHour(8).withMinute(0).withSecond(0).withNano(0)
        if (!next.isAfter(now)) next = next.plusDays(1)
        val delay = Duration.between(now, next).toMillis()

        WorkManager.getInstance(context).enqueueUniqueWork(
            DAILY_SCRIPTURE_WORK,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<KfccNotificationWorker>()
                .setInputData(Data.Builder().putString(KEY_MODE, MODE_DAILY_SCRIPTURE).build())
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setConstraints(connectedConstraints())
                .build()
        )
    }

    /**
     * Deliver today's scripture immediately when authentication succeeds.
     * The worker still requires internet, so an offline device waits until it
     * reconnects instead of silently losing the delivery.
     */
    fun deliverDailyScriptureNow(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            DAILY_SCRIPTURE_NOW_WORK,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<KfccNotificationWorker>()
                .setInputData(Data.Builder().putString(KEY_MODE, MODE_DAILY_SCRIPTURE).build())
                .setConstraints(connectedConstraints())
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
                .setConstraints(connectedConstraints())
                .build()
        )
    }

    private fun enqueue(context: Context, mode: String?, requireNetwork: Boolean) {
        val builder = OneTimeWorkRequestBuilder<KfccNotificationWorker>()
        if (mode != null) {
            builder.setInputData(Data.Builder().putString(KEY_MODE, mode).build())
        }
        if (requireNetwork) builder.setConstraints(connectedConstraints())

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
