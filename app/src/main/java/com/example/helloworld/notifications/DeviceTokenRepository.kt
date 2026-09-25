package com.example.helloworld.notifications

import com.example.helloworld.data.SupabaseProvider
import com.google.firebase.messaging.FirebaseMessaging
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class DeviceTokenRepository {
    private val client = SupabaseProvider.client

    suspend fun registerCurrentToken(): Result<Unit> = runCatching {
        val userId = client.auth.currentUserOrNull()?.id
            ?: error("No authenticated Supabase user")

        val token = suspendCancellableCoroutine<String> { continuation ->
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    continuation.resume(task.result)
                } else {
                    continuation.resumeWithException(
                        task.exception ?: IllegalStateException("Unable to obtain FCM token")
                    )
                }
            }
        }

        registerTokenForUser(userId, token)
    }

    suspend fun registerToken(token: String): Result<Unit> = runCatching {
        val userId = client.auth.currentUserOrNull()?.id
            ?: return@runCatching
        registerTokenForUser(userId, token)
    }

    private suspend fun registerTokenForUser(userId: String, token: String) {
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
