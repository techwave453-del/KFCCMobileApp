package com.example.helloworld.data

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from

class ProfileRepository {
    private val client get() = SupabaseProvider.client

    suspend fun getProfile(): Result<ChatProfile?> = runCatching {
        val userId = client.auth.currentUserOrNull()?.id ?: return@runCatching null
        client.from("chat_profiles")
            .select {
                filter { eq("user_id", userId) }
            }
            .decodeList<ChatProfile>()
            .firstOrNull()
    }

    suspend fun updateProfile(
        username: String,
        displayName: String?,
        avatarUrl: String?,
    ): Result<ChatProfile> = runCatching {
        val userId = client.auth.currentUserOrNull()?.id
            ?: error("Please sign in first.")

        val normalizedUsername = username.trim().removePrefix("@").lowercase()
        require(Regex("^[a-z0-9_.]{3,20}$").matches(normalizedUsername)) {
            "Username must be 3–20 characters using letters, numbers, _ or ."
        }

        val duplicate = client.from("chat_profiles")
            .select {
                filter { eq("username", normalizedUsername) }
            }
            .decodeList<ChatProfile>()
            .firstOrNull { it.user_id != userId }

        require(duplicate == null) { "That username is already in use." }

        val updated = ChatProfile(
            user_id = userId,
            username = normalizedUsername,
            display_name = displayName?.trim()?.ifBlank { null },
            avatar_url = avatarUrl?.trim()?.ifBlank { null },
        )

        client.from("chat_profiles").upsert(updated)
        updated
    }
}
