package com.example.helloworld.notifications

import com.example.helloworld.data.SupabaseProvider
import io.github.jan.supabase.auth.auth
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class DeviceTokenRepository {
    private val client = SupabaseProvider.client

    suspend fun registerToken(token: String): Result<Unit> = runCatching {
        val userId = client.auth.currentUserOrNull()?.id ?: return@runCatching
        client.from("device_tokens").upsert(
            DeviceTokenUpsert(
                userId = userId,
                token = token,
                platform = "android"
            )
        )
    }

    @Serializable
    private data class DeviceTokenUpsert(
        @SerialName("user_id") val userId: String,
        val token: String,
        val platform: String
    )
}
