package com.example.helloworld.notifications

import android.util.Log
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
        val userId = authenticatedUserId()
            ?: error("No authenticated Supabase user after session refresh")

        val token = getFcmToken()
        registerTokenForUser(userId, token)

        Log.i(TAG, "FCM device token registered for Supabase user $userId")
        Unit
    }.onFailure {
        Log.e(TAG, "FCM device token registration failed", it)
    }

    suspend fun registerToken(token: String): Result<Unit> = runCatching {
        val userId = authenticatedUserId()
            ?: error("No authenticated Supabase user while registering refreshed FCM token")

        registerTokenForUser(userId, token)
        Log.i(TAG, "Refreshed FCM device token registered for Supabase user $userId")
        Unit
    }.onFailure {
        Log.e(TAG, "Refreshed FCM device token registration failed", it)
    }

    private suspend fun authenticatedUserId(): String? {
        client.auth.currentUserOrNull()?.id?.let { return it }

        return runCatching {
            client.auth.retrieveUserForCurrentSession()
        }.onFailure {
            Log.e(TAG, "Unable to restore Supabase user for FCM registration", it)
        }.getOrNull()?.id
    }

    private suspend fun getFcmToken(): String =
        suspendCancellableCoroutine { continuation ->
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful && !task.result.isNullOrBlank()) {
                    continuation.resume(task.result)
                } else {
                    continuation.resumeWithException(
                        task.exception ?: IllegalStateException("Unable to obtain a valid FCM token")
                    )
                }
            }
        }

    private suspend fun registerTokenForUser(userId: String, token: String) {
        require(token.isNotBlank()) { "FCM token is blank" }

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

    private companion object {
        const val TAG = "KfccPush"
    }
}
