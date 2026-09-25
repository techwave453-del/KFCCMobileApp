package com.example.helloworld.data

import com.example.helloworld.data.offline.KfccContentRepository
import com.example.helloworld.data.offline.KfccDatabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.Flow

class NotificationRepository {
    private val client = SupabaseProvider.client
    private val offline = KfccContentRepository(KfccDatabase.getInstance(KfccDataContext.appContext))

    fun observeNotifications(): Flow<List<AppNotification>> {
        val userId = client.auth.currentUserOrNull()?.id
        return offline.observeNotifications(userId)
    }

    suspend fun getNotifications(): Result<List<AppNotification>> = runCatching {
        val userId = client.auth.currentUserOrNull()?.id
        offline.getNotifications(userId)
    }

    suspend fun syncFromServer(): Result<List<AppNotification>> = runCatching {
        val userId = client.auth.currentUserOrNull()?.id ?: error("No signed-in user")
        val rows = client.from("app_notifications")
            .select()
            .decodeList<AppNotification>()
            .filter { it.isEnabled && (it.userId == null || it.userId == userId) }
            .sortedByDescending { it.createdAt }
        cache(rows)
        rows
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
