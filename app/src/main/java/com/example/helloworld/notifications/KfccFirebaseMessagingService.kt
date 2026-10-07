package com.example.helloworld.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.request.ImageRequest
import com.example.helloworld.MainActivity
import com.example.helloworld.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class KfccFirebaseMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        CoroutineScope(Dispatchers.IO).launch {
            DeviceTokenRepository().registerToken(token).onFailure { Log.e(TAG, "Unable to register refreshed FCM token", it) }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        CoroutineScope(Dispatchers.IO).launch { handleIncomingMessage(message) }
    }

    private suspend fun handleIncomingMessage(message: RemoteMessage) {
        val body = message.data["message"]?.takeIf { it.isNotBlank() } ?: message.notification?.body ?: return
        val title = message.data["title"]?.takeIf { it.isNotBlank() }
            ?: message.notification?.title
            ?: NotificationBrandRepository().getChurchName().ifBlank { "Church Notification" }

        ensureChannel(title)

        val type = message.data["type"].orEmpty()
        val isChat = type.equals("chat", true) || type.equals("chat_message", true)
        val roomId = message.data["room_id"].orEmpty()
        val notificationId = message.data["notification_id"]?.takeIf { it.isNotBlank() } ?: body
        val image = message.data["image_url"]?.let { loadNotificationBitmap(it) }
        val senderAvatar = message.data["sender_avatar_url"]?.let { loadNotificationBitmap(it) }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (isChat && roomId.isNotBlank()) {
                putExtra(MainActivity.EXTRA_OPEN_CHAT, true)
                putExtra(MainActivity.EXTRA_CHAT_ROOM_ID, roomId)
            } else {
                putExtra(MainActivity.EXTRA_OPEN_NOTIFICATIONS, true)
            }
            putExtra(MainActivity.EXTRA_NOTIFICATION_ID, notificationId)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, notificationId.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, KfccNotificationWorker.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(if (isChat) NotificationCompat.CATEGORY_MESSAGE else NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        if (isChat) {
            val senderBuilder = Person.Builder().setName(title)
            if (senderAvatar != null) {
                senderBuilder.setIcon(IconCompat.createWithBitmap(senderAvatar))
                builder.setLargeIcon(senderAvatar)
            }
            val sender = senderBuilder.build()
            builder.setStyle(
                NotificationCompat.MessagingStyle(Person.Builder().setName("Kanisa").build())
                    .addMessage(NotificationCompat.MessagingStyle.Message(body, System.currentTimeMillis(), sender))
            )
            builder.addPerson(sender)
        } else if (image != null) {
            builder.setStyle(
                NotificationCompat.BigPictureStyle().bigPicture(image).bigLargeIcon(null as android.graphics.Bitmap?)
            )
        } else {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(body))
        }

        if (!isChat && senderAvatar != null) builder.setLargeIcon(senderAvatar)
        NotificationManagerCompat.from(this).notify(notificationId.hashCode(), builder.build())
    }

    private suspend fun loadNotificationBitmap(url: String): android.graphics.Bitmap? =
        runCatching {
            val request = ImageRequest.Builder(this).data(url).allowHardware(false).build()
            ImageLoader(this).execute(request).drawable?.toBitmap()
        }.getOrNull()

    private fun ensureChannel(churchName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(KfccNotificationWorker.CHANNEL_ID, "$churchName Notifications", NotificationManager.IMPORTANCE_HIGH)
            )
        }
    }

    private companion object { const val TAG = "KfccPush" }
}
