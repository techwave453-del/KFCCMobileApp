package com.example.helloworld.data

import com.example.helloworld.data.offline.KfccContentRepository
import com.example.helloworld.data.offline.KfccDatabase
import com.example.helloworld.data.offline.NotificationReadEntity
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class NotificationReadRecord(
    @SerialName("notification_id") val notificationId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("read_at") val readAt: String
)

class NotificationRepository {
    private val client = SupabaseProvider.client
    private val offline = KfccContentRepository(KfccDatabase.getInstance(KfccDataContext.appContext))

    fun observeNotifications(): Flow<List<AppNotification>> {
        val userId = client.auth.currentUserOrNull()?.id
        return offline.observeNotifications(userId)
    }

    suspend fun getNotifications(): Result<List<AppNotification>> = runCatching {
        syncFromServer().getOrElse {
            val userId = client.auth.currentUserOrNull()?.id
            offline.getNotifications(userId)
        }
    }

    suspend fun syncFromServer(): Result<List<AppNotification>> = runCatching {
        val userId = client.auth.currentUserOrNull()?.id ?: error("No signed-in user")
        val rows = client.from("app_notifications")
            .select()
            .decodeList<AppNotification>()
            .filter { it.isEnabled && (it.userId == null || it.userId == userId) }
            .sortedByDescending { it.createdAt }

        val reads = client.from("notification_reads")
            .select(Columns.list("notification_id", "user_id", "read_at"))
            .decodeList<NotificationReadRecord>()
            .filter { it.userId == userId }
            .associateBy { it.notificationId }

        val resolved = rows.map { notification ->
            notification.copy(readAt = reads[notification.id]?.readAt)
        }

        cache(resolved)
        resolved
    }

    suspend fun markAsRead(notificationId: String): Result<Unit> = runCatching {
        val userId = client.auth.currentUserOrNull()?.id ?: error("No signed-in user")
        val now = java.time.Instant.now().toString()
        client.from("notification_reads").upsert(
            NotificationReadRecord(
                notificationId = notificationId,
                userId = userId,
                readAt = now
            )
        )
        offlineCache().notificationDao().upsertRead(
            NotificationReadEntity(notificationId, userId, now)
        )
    }

    suspend fun markAllAsRead(notificationIds: List<String>): Result<Unit> = runCatching {
        val userId = client.auth.currentUserOrNull()?.id ?: error("No signed-in user")
        val now = java.time.Instant.now().toString()
        val records = notificationIds.distinct().map {
            NotificationReadRecord(
                notificationId = it,
                userId = userId,
                readAt = now
            )
        }
        if (records.isNotEmpty()) {
            client.from("notification_reads").upsert(records)
            records.forEach {
                offlineCache().notificationDao().upsertRead(
                    NotificationReadEntity(it.notificationId, userId, it.readAt)
                )
            }
        }
    }

    suspend fun getPublicDefault(onInstall: Boolean): Result<AppNotification?> = runCatching {
        val rows = client.from("app_notifications")
            .select(Columns.list("id", "title", "message", "type", "created_at", "user_id", "is_enabled", "show_on_install", "show_on_sign_in", "updated_at"))
            .decodeList<AppNotification>()
        rows.firstOrNull {
            it.userId == null &&
                it.isEnabled &&
                if (onInstall) it.showOnInstall else it.showOnSignIn
        }
    }

    private suspend fun cache(rows: List<AppNotification>) {
        offlineCache().notificationDao().upsertAll(rows.map {
            com.example.helloworld.data.offline.NotificationEntity(
                it.id, it.userId, it.title, it.message, it.type, it.createdAt
            )
        })
    }

    private fun offlineCache() = KfccDatabase.getInstance(KfccDataContext.appContext)
}
