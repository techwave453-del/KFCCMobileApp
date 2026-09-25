package com.example.helloworld.data

import com.example.helloworld.data.offline.KfccContentRepository
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
}
