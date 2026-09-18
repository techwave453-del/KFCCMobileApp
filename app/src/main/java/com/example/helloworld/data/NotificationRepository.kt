package com.example.helloworld.data

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.*

@Serializable
data class AppNotification(
    val id: String,
    @SerialName("user_id") val userId: String? = null,
    val title: String,
    val message: String,
    val type: String = "general",
    @SerialName("created_at") val createdAt: String,
    @SerialName("read_at") val readAt: String? = null,
)

@Serializable
private data class NotificationRead(
    @SerialName("notification_id") val notificationId: String,
    @SerialName("read_at") val readAt: String,
)

class NotificationRepository {
    private val client = SupabaseProvider.client

    suspend fun getNotifications(): Result<List<AppNotification>> = runCatching {
        val userId = client.auth.currentUserOrNull()?.id
            ?: error("Please sign in to view notifications.")

        val notifications = client.from("app_notifications")
            .select()
            .decodeList<AppNotification>()
            .filter { it.userId == null || it.userId == userId }
            .sortedByDescending { it.createdAt }

        val reads = client.from("notification_reads")
            .select { filter { eq("user_id", userId) } }
            .decodeList<NotificationRead>()
            .associateBy { it.notificationId }

        val withReadState = notifications.map { notification ->
            notification.copy(readAt = reads[notification.id]?.readAt)
        }

        if (withReadState.isEmpty()) {
            listOf(
                AppNotification(
                    id = "welcome",
                    title = "Welcome to KFCC!",
                    message = "We're glad to have you here. Explore our community and stay connected.",
                    type = "welcome",
                    createdAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())
                )
            )
        } else withReadState
    }

    suspend fun markAsRead(notificationId: String): Result<Unit> = runCatching {
        val userId = client.auth.currentUserOrNull()?.id
            ?: error("Please sign in to mark notifications as read.")
        if (notificationId == "welcome") return@runCatching

        client.from("notification_reads").upsert(
            mapOf("notification_id" to notificationId, "user_id" to userId),
            onConflict = "notification_id,user_id"
        )
    }

    fun observeNotifications(): Flow<PostgresAction> {
        val channel = client.realtime.channel("notifications_" + System.currentTimeMillis())
        return channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "app_notifications"
        }.onStart {
            channel.subscribe()
        }.onCompletion {
            client.realtime.removeChannel(channel)
        }
    }
}
