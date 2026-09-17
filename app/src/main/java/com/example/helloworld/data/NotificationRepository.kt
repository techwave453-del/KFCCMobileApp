package com.example.helloworld.data

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.flow.Flow
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

class NotificationRepository {
    private val client = SupabaseProvider.client

    suspend fun getNotifications(): Result<List<AppNotification>> = runCatching {
        val userId = client.auth.currentUserOrNull()?.id
        val notifications = client.from("app_notifications")
            .select()
            .decodeList<AppNotification>()
            .filter { it.userId == null || it.userId == userId }
            .sortedByDescending { it.createdAt }

        if (notifications.isEmpty()) {
            listOf(
                AppNotification(
                    id = "welcome",
                    title = "Welcome to KFCC!",
                    message = "We're glad to have you here. Explore our community and stay connected.",
                    type = "welcome",
                    createdAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())
                )
            )
        } else notifications
    }

    fun observeNotifications(): Flow<PostgresAction> {
        val channel = client.realtime.channel("notifications")
        return channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "app_notifications"
        }
    }
}
