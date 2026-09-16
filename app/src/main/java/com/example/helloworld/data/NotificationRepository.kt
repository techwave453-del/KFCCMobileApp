package com.example.helloworld.data

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppNotification(
    val id: String,
    val title: String,
    @SerialName("message") val body: String,
    val type: String = "general",
    @SerialName("created_at") val createdAt: String,
    @SerialName("user_id") val userId: String? = null,
    @kotlinx.serialization.Transient var readAt: String? = null,
)

@Serializable
private data class NotificationRead(
    @SerialName("notification_id") val notificationId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("read_at") val readAt: String,
)

class NotificationRepository {
    private val client get() = SupabaseProvider.client

    suspend fun getNotifications(limit: Int = 50): Result<List<AppNotification>> = runCatching {
        val userId = client.auth.currentUserOrNull()?.id ?: return@runCatching emptyList()

        val notifications = client.from("app_notifications")
            .select()
            .decodeList<AppNotification>()
            .filter { it.userId == null || it.userId == userId }
            .sortedByDescending { it.createdAt }
            .take(limit)

        val reads = client.from("notification_reads")
            .select {
                filter { eq("user_id", userId) }
            }
            .decodeList<NotificationRead>()
            .associateBy { it.notificationId }

        notifications.map { notification ->
            notification.readAt = reads[notification.id]?.readAt
            notification
        }
    }

    suspend fun markRead(id: String): Result<Unit> = runCatching {
        val userId = client.auth.currentUserOrNull()?.id
            ?: return@runCatching

        client.from("notification_reads").upsert(
            mapOf(
                "notification_id" to id,
                "user_id" to userId,
            )
        )
    }

    suspend fun markAllRead(): Result<Unit> = runCatching {
        val userId = client.auth.currentUserOrNull()?.id
            ?: return@runCatching

        val notifications = client.from("app_notifications")
            .select()
            .decodeList<AppNotification>()
            .filter { it.userId == null || it.userId == userId }

        val existingReads = client.from("notification_reads")
            .select {
                filter { eq("user_id", userId) }
            }
            .decodeList<NotificationRead>()
            .map { it.notificationId }
            .toSet()

        val unread = notifications
            .filterNot { it.id in existingReads }
            .map {
                mapOf(
                    "notification_id" to it.id,
                    "user_id" to userId,
                )
            }

        if (unread.isNotEmpty()) {
            client.from("notification_reads").insert(unread)
        }
    }
}
