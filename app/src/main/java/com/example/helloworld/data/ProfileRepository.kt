package com.example.helloworld.data

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    @SerialName("user_id") val userId: String,
    val username: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val bio: String? = null,
)

class ProfileRepository {
    private val client get() = SupabaseProvider.client

    fun currentUserId(): String? = client.auth.currentUserOrNull()?.id

    fun currentEmail(): String? = client.auth.currentUserOrNull()?.email

    suspend fun getProfile(): Result<UserProfile?> = runCatching {
        val userId = currentUserId() ?: return@runCatching null
        client.from("profiles")
            .select { filter { eq("user_id", userId) } }
            .decodeList<UserProfile>()
            .firstOrNull()
    }

    suspend fun saveProfile(profile: UserProfile): Result<Unit> = runCatching {
        client.from("profiles").upsert(profile)
    }

    suspend fun signOut(): Result<Unit> = runCatching {
        client.auth.signOut()
    }
}
