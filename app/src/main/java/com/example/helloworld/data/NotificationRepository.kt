package com.example.helloworld.data

import com.example.helloworld.data.offline.KfccContentRepository
import com.example.helloworld.data.offline.KfccDatabase
import com.example.helloworld.data.offline.KfccDatabase
import io.github.jan.supabase.auth.auth
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
            .filter { it.userId == null || it.userId == userId }
            .sortedByDescending { it.createdAt }
        offlineCache().notificationDao().upsertAll(rows.map {
            com.example.helloworld.data.offline.NotificationEntity(it.id, it.userId, it.title, it.message, it.type, it.createdAt)
        })
        rows
    }

    private fun offlineCache() = KfccDatabase.getInstance(KfccDataContext.appContext)
}
