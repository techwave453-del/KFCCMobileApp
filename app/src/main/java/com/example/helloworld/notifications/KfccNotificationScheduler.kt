package com.example.helloworld.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.helloworld.MainActivity
import com.example.helloworld.data.AppNotification
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalTime
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
    private const val SIGNUP_CHANNEL_ID = "kfcc_church_notifications"
    private const val DAILY_SCRIPTURE_WORK = "kfcc_daily_scripture_notification"
    const val KEY_MODE = "notification_mode"
    const val KEY_USERNAME = "notification_username"
    const val MODE_SIGN_IN = "sign_in"
    const val MODE_INSTALL = "install"
    const val MODE_SIGN_UP = "sign_up"
    const val MODE_DAILY_SCRIPTURE = "daily_scripture"

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

        scheduleInstallDelivery(context)
        scheduleDailyScripture(context)
    }

    fun scheduleInstallDelivery(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            INSTALL_WORK,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<KfccNotificationWorker>()
                .setInputData(
                    Data.Builder()
                        .putString(KEY_MODE, MODE_INSTALL)
                        .build()
                )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
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
                .setInputData(
                    Data.Builder()
                        .putString(KEY_MODE, MODE_DAILY_SCRIPTURE)
                        .build()
                )
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
        )
    }

    fun syncNow(context: Context) {
        enqueue(context, null, requireNetwork = true)
    }

    fun deliverSignInDefault(context: Context) {
        enqueue(context, MODE_SIGN_IN, requireNetwork = true)
    }

    /**
     * Signup is a foreground user action, so the welcome notification must be
     * posted immediately instead of waiting for WorkManager. WorkManager is
     * intentionally retained for background/install/daily delivery only.
     */
    suspend fun deliverSignupWelcome(context: Context, username: String) = withContext(Dispatchers.IO) {
        val normalizedUsername = username.trim().removePrefix("@")
        if (normalizedUsername.isBlank()) return@withContext

        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return@withContext

        val churchName = NotificationBrandRepository().getChurchName().ifBlank { "Church" }
        ensureSignupChannel(context, churchName)

        val greeting = when (LocalTime.now().hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            else -> "Good evening"
        }

        val notification = AppNotification(
            id = "signup-${System.currentTimeMillis()}-${normalizedUsername.hashCode()}",
            title = "$greeting, $normalizedUsername",
            message = "Your $churchName account has been created successfully. Please verify your email, then sign in to continue.",
            type = "welcome",
            createdAt = Instant.now().toString()
        )

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_NOTIFICATIONS, true)
            putExtra(MainActivity.EXTRA_NOTIFICATION_ID, notification.id)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notification.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        manager.notify(
            notification.id.hashCode(),
            NotificationCompat.Builder(context, SIGNUP_CHANNEL_ID)
                .setSmallIcon(com.example.helloworld.R.mipmap.ic_launcher)
                .setContentTitle(notification.title)
                .setContentText(notification.message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(notification.message))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()
        )
    }

    private fun ensureSignupChannel(context: Context, churchName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(
                    SIGNUP_CHANNEL_ID,
                    "$churchName Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Welcome and account notifications from $churchName."
                }
            )
        }
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
