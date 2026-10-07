package com.example.helloworld.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.request.ImageRequest
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
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
            DeviceTokenRepository().registerToken(token).onFailure {
                Log.e(TAG, "Unable to register refreshed FCM token", it)
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        CoroutineScope(Dispatchers.IO).launch {
            handleIncomingMessage(message)
        }
    }

    private suspend fun handleIncomingMessage(message: RemoteMessage) {
        val body = message.notification?.body
            ?: message.data["message"]
            ?: return

        val configuredTitle = message.notification?.title
            ?: message.data["title"]
        val title = configuredTitle ?: NotificationBrandRepository().getChurchName().ifBlank {
            "Church Notification"
        }

        ensureChannel(title)

        val isChat = message.data["type"].equals("chat", true) || message.data["type"].equals("chat_message", true)
        val roomId = message.data["room_id"].orEmpty()
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (isChat && roomId.isNotBlank()) {
                putExtra(MainActivity.EXTRA_OPEN_CHAT, true)
                putExtra(MainActivity.EXTRA_CHAT_ROOM_ID, roomId)
            } else {
                putExtra(MainActivity.EXTRA_OPEN_NOTIFICATIONS, true)
            }
            putExtra(MainActivity.EXTRA_NOTIFICATION_ID, message.data["notification_id"] ?: body)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            (message.data["notification_id"] ?: body).hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, KfccNotificationWorker.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setLargeIcon(null as android.graphics.Bitmap?)
            .setContentText(body)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        val imageUrl = message.data["image_url"]
        val image = imageUrl?.let { loadNotificationBitmap(it) }
        val senderAvatar = message.data["sender_avatar_url"]?.let { loadNotificationBitmap(it) }
        if (senderAvatar != null) builder.setLargeIcon(senderAvatar)
        if (isChat && roomId.isNotBlank()) {
            val replyIntent = Intent(this, ChatNotificationReplyReceiver::class.java).apply {
                putExtra(ChatNotificationReplyReceiver.EXTRA_ROOM_ID, roomId)
                putExtra(ChatNotificationReplyReceiver.EXTRA_NOTIFICATION_ID, message.data["notification_id"] ?: body)
            }
            val replyPendingIntent = PendingIntent.getBroadcast(
                this,
                ("reply:" + (message.data["notification_id"] ?: body)).hashCode(),
                replyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            val remoteInput = RemoteInput.Builder(ChatNotificationReplyReceiver.EXTRA_REPLY)
                .setLabel("Reply")
                .build()
            builder.addAction(
                NotificationCompat.Action.Builder(
                    android.R.drawable.ic_menu_send,
                    "Reply",
                    replyPendingIntent
                ).addRemoteInput(remoteInput).build()
            )
        }

        if (image != null) {
            builder.setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(image)
                    .bigLargeIcon(null as android.graphics.Bitmap?)
            )
        } else {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(body))
        }

        NotificationManagerCompat.from(this).notify(
            (message.data["notification_id"] ?: body).hashCode(),
            builder.build()
        )
    }

    private suspend fun loadNotificationBitmap(url: String): android.graphics.Bitmap? {
        return runCatching {
            val request = ImageRequest.Builder(this)
                .data(url)
                .allowHardware(false)
                .build()
            ImageLoader(this).execute(request).drawable?.toBitmap()
        }.getOrNull()
    }

    private fun ensureChannel(churchName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(
                    KfccNotificationWorker.CHANNEL_ID,
                    "$churchName Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            )
        }
    }

    private companion object {
        const val TAG = "KfccPush"
    }
}
