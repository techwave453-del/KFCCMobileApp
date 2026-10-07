package com.example.helloworld.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.example.helloworld.data.ChatRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ChatNotificationReplyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val roomId = intent.getStringExtra(EXTRA_ROOM_ID).orEmpty()
        val notificationId = intent.getStringExtra(EXTRA_NOTIFICATION_ID).orEmpty()
        val reply = androidx.core.app.RemoteInput.getResultsFromIntent(intent)
            ?.getCharSequence(EXTRA_REPLY)
            ?.toString()
            ?.trim()
            .orEmpty()

        if (roomId.isBlank() || reply.isBlank()) return

        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                ChatRepository().sendMessage(roomId, reply)
                if (notificationId.isNotBlank()) {
                    NotificationManagerCompat.from(appContext).cancel(notificationId.hashCode())
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_ROOM_ID = "kfcc.reply_room_id"
        const val EXTRA_REPLY = "kfcc.reply_text"
        const val EXTRA_NOTIFICATION_ID = "kfcc.reply_notification_id"
    }
}
