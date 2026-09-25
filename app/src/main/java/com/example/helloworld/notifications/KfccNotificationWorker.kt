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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

class KfccNotificationWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        runCatching {
            val repository = NotificationRepository()
            val notifications = repository.syncFromServer().getOrThrow()
            if (notifications.isEmpty()) return@runCatching Result.success()

            val preferences = applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val delivered = preferences.getStringSet(KEY_DELIVERED, emptySet()).orEmpty().toMutableSet()
            val initialized = preferences.getBoolean(KEY_INITIALIZED, false)

            if (!initialized) {
                delivered.addAll(notifications.map(AppNotification::id))
                saveDelivered(preferences, delivered)
                preferences.edit().putBoolean(KEY_INITIALIZED, true).apply()
                return@runCatching Result.success()
            }

            ensureChannel()
            val manager = NotificationManagerCompat.from(applicationContext)
            if (!manager.areNotificationsEnabled()) return@runCatching Result.success()

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

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = applicationContext.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "KFCC Church Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Announcements and important updates from KFCC."
                }
            )
        }
    }

    private fun postNotification(notification: AppNotification) {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
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
        val array = JSONArray()
        trimmed.forEach(array::put)
        preferences.edit().putString(KEY_DELIVERED, trimmed.joinToString("\u0001")).apply()
    }

    companion object {
        const val CHANNEL_ID = "kfcc_church_notifications"
        private const val PREFS = "kfcc_notification_delivery"
        private const val KEY_DELIVERED = "delivered_ids"
        private const val KEY_INITIALIZED = "initialized"
        private const val MAX_DELIVERED_IDS = 200
    }
}
