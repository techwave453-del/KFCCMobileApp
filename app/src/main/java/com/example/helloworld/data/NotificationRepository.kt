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

class NotificationRepository {
    private val client get() = SupabaseProvider.client

    suspend fun getNotifications(): Result<List<AppNotification>> = runCatching {
        val userId = client.auth.currentUserOrNull()?.id
        client.from("notifications")
            .select {
                filter {
                    or {
                        is("user_id", null)
                        if (userId != null) eq("user_id", userId)
                    }
                }
            }
            .decodeList<AppNotification>()
            .sortedByDescending { it.createdAt }
    }

    suspend fun markAsRead(notificationId: String): Result<Unit> = runCatching {
        client.from("notifications").update(
            { set("read_at", java.time.Instant.now().toString()) }
        ) {
            filter { eq("id", notificationId) }
        }
    }
}
