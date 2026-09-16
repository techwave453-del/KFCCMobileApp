package com.example.helloworld.data

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppNotification(
    val id: String,
    val title: String,
    val body: String,
    val type: String = "general",
    @SerialName("created_at") val createdAt: String,
    @SerialName("read_at") val readAt: String? = null,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("action_route") val actionRoute: String? = null,
)

class NotificationRepository {
    private val client get() = SupabaseProvider.client

    suspend fun getNotifications(limit: Int = 50): Result<List<AppNotification>> = runCatching {
        val userId = client.auth.currentUserOrNull()?.id
        if (userId == null) emptyList()
        else client.from("notifications")
            .select {
                filter { eq("user_id", userId) }
            }
            .decodeList<AppNotification>()
            .sortedByDescending { it.createdAt }
            .take(limit)
    }

    suspend fun markRead(id: String): Result<Unit> = runCatching {
        client.from("notifications").update(
            mapOf("read_at" to java.time.Instant.now().toString())
        ) {
            filter { eq("id", id) }
        }
    }

    suspend fun markAllRead(): Result<Unit> = runCatching {
        val userId = client.auth.currentUserOrNull()?.id ?: return@runCatching
        client.from("notifications").update(
            mapOf("read_at" to java.time.Instant.now().toString())
        ) {
            filter {
                eq("user_id", userId)
            }
        }
    }
}
