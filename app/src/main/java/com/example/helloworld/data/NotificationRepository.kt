package com.example.helloworld.data

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
data class NotificationRead(
    @SerialName("notification_id") val notificationId: String,
    @SerialName("user_id") val userId: String,
)

class NotificationRepository {
    private val client get() = SupabaseProvider.client

    suspend fun getNotifications(): Result<List<AppNotification>> = runCatching {
        val userId = client.auth.currentUserOrNull()?.id ?: return@runCatching emptyList()
        val notifications = client.from("app_notifications").select().decodeList<AppNotification>()
        val reads = client.from("notification_reads")
            .select { filter { eq("user_id", userId) } }
            .decodeList<NotificationRead>()
            .map { it.notificationId }
            .toSet()
        notifications
            .map { it.copy(readAt = if (it.id in reads) "read" else null) }
            .sortedByDescending { it.createdAt }
    }

    suspend fun markAsRead(notificationId: String): Result<Unit> = runCatching {
        val userId = client.auth.currentUserOrNull()?.id ?: error("Please sign in first.")
        client.from("notification_reads").upsert(
            mapOf("notification_id" to notificationId, "user_id" to userId)
        )
    }

    suspend fun markAllAsRead(notifications: List<AppNotification>): Result<Unit> = runCatching {
        val userId = client.auth.currentUserOrNull()?.id ?: error("Please sign in first.")
        notifications.filter { it.readAt == null }.forEach { notification ->
            client.from("notification_reads").upsert(
                mapOf("notification_id" to notification.id, "user_id" to userId)
            )
        }
    }
}
