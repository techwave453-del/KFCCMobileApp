package com.example.helloworld.notifications

import android.util.Log
import com.example.helloworld.data.AppPreferences
import com.example.helloworld.data.KfccDataContext
import com.example.helloworld.data.SupabaseProvider
import com.google.firebase.messaging.FirebaseMessaging
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class DeviceTokenRepository {
    private val client = SupabaseProvider.client
    private val preferences = AppPreferences(KfccDataContext.appContext)

    suspend fun initializeInstallationPushRegistration(): Result<Unit> = runCatching {
        ensureInstallationId()
        val token = getFcmToken()
        preferences.fcmToken = token
        Log.i(TAG, "FCM token stored for installation " + preferences.installationId)
        Unit
    }.onFailure {
        Log.e(TAG, "Initial FCM installation registration failed", it)
    }

    suspend fun registerCurrentToken(): Result<Unit> = runCatching {
        val userId = authenticatedUserId()
            ?: error("No authenticated Supabase user after session refresh")

        val token = preferences.fcmToken ?: getFcmToken().also {
            preferences.fcmToken = it
        }

        registerTokenForUser(userId, token)
        Log.i(TAG, "FCM device token associated with Supabase user " + userId)
        Unit
    }.onFailure {
        Log.e(TAG, "FCM device token association failed", it)
    }

    suspend fun unregisterCurrentToken(): Result<Unit> = runCatching {
        val userId = authenticatedUserId() ?: return@runCatching Unit
        val token = preferences.fcmToken ?: return@runCatching Unit

        client.from("device_tokens").delete {
            filter {
                eq("user_id", userId)
                eq("token", token)
            }
        }
        Log.i(TAG, "FCM device token removed from Supabase user " + userId)
        Unit
    }.onFailure {
        Log.e(TAG, "FCM device token cleanup failed", it)
    }

    suspend fun registerToken(token: String): Result<Unit> = runCatching {
        require(token.isNotBlank()) { "FCM token is blank" }
        ensureInstallationId()
        preferences.fcmToken = token

        val userId = authenticatedUserId()
        if (userId != null) {
            registerTokenForUser(userId, token)
            Log.i(TAG, "Refreshed FCM token associated with Supabase user " + userId)
        } else {
            Log.i(TAG, "Refreshed FCM token stored for anonymous installation")
        }

        Unit
    }.onFailure {
        Log.e(TAG, "FCM token refresh handling failed", it)
    }

    private fun ensureInstallationId(): String {
        val existing = preferences.installationId
        if (!existing.isNullOrBlank()) return existing

        return UUID.randomUUID().toString().also {
            preferences.installationId = it
            Log.i(TAG, "Created KFCC installation identity")
        }
    }

    private suspend fun authenticatedUserId(): String? {
        client.auth.currentUserOrNull()?.id?.let { return it }

        return runCatching {
            client.auth.retrieveUserForCurrentSession()
        }.onFailure {
            Log.e(TAG, "Unable to restore Supabase user for FCM association", it)
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