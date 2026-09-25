package com.example.helloworld.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.helloworld.MainActivity
import com.example.helloworld.R
import com.example.helloworld.data.AppNotification
import com.example.helloworld.data.NotificationRepository
import io.github.jan.supabase.auth.auth
import com.example.helloworld.data.SupabaseProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class KfccNotificationWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        runCatching {
            val repository = NotificationRepository()
            val preferences = applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val delivered = preferences.getStringSet(KEY_DELIVERED, emptySet()).orEmpty().toMutableSet()
            val mode = inputData.getString(KfccNotificationScheduler.KEY_MODE)

            val churchName = NotificationBrandRepository().getChurchName().ifBlank { "Church" }
            ensureChannel(churchName)

            val manager = NotificationManagerCompat.from(applicationContext)
            if (!manager.areNotificationsEnabled()) return@runCatching Result.success()

            if (mode == KfccNotificationScheduler.MODE_SIGN_IN) {
                val defaultNotification = repository.getPublicDefault(onInstall = false).getOrNull()
                if (defaultNotification != null && defaultNotification.id !in delivered) {
                    postNotification(defaultNotification)
                    delivered.add(defaultNotification.id)
                    saveDelivered(preferences, delivered)
                }
                return@runCatching Result.success()
            }

            val initialized = preferences.getBoolean(KEY_INITIALIZED, false)
            if (!initialized) {
                val installDefault = repository.getPublicDefault(onInstall = true).getOrNull()
                if (installDefault != null && installDefault.id !in delivered) {
                    postNotification(installDefault)
                    delivered.add(installDefault.id)
                }
                saveDelivered(preferences, delivered)
                preferences.edit().putBoolean(KEY_INITIALIZED, true).apply()
            }

            if (SupabaseProvider.client.auth.currentUserOrNull() == null) {
                return@runCatching Result.success()
            }

            val notifications = repository.syncFromServer().getOrThrow()
            notifications.asReversed()
                .filter { it.id !in delivered }
                .forEach { notification ->
                    postNotification(notification)
                    delivered.add(notification.id)
                }

            saveDelivered(preferences, delivered)
            Result.success()
        }.getOrElse { Result.retry() }
    }

    private fun ensureChannel(churchName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = applicationContext.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "$churchName Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Announcements and important updates from $churchName."
                }
            )
        }
    }

    private fun postNotification(notification: AppNotification) {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_NOTIFICATIONS, true)
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            notification.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        NotificationManagerCompat.from(applicationContext).notify(
            notification.id.hashCode(),
            NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(notification.title)
                .setContentText(notification.message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(notification.message))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()
        )
    }

    private fun saveDelivered(
        preferences: android.content.SharedPreferences,
        ids: MutableSet<String>
    ) {
        val trimmed = ids.toList().takeLast(MAX_DELIVERED_IDS).toSet()
        preferences.edit().putStringSet(KEY_DELIVERED, trimmed).apply()
    }

    companion object {
        const val CHANNEL_ID = "kfcc_church_notifications"
        private const val PREFS = "kfcc_notification_delivery"
        private const val KEY_DELIVERED = "delivered_ids"
        private const val KEY_INITIALIZED = "initialized"
        private const val MAX_DELIVERED_IDS = 200
    }
}
