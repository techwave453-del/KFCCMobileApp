package com.example.helloworld.data

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class ChatProfile(
    val user_id: String,
    val username: String,
    val display_name: String? = null,
    val avatar_url: String? = null,
)

data class ChatAuthResult(
    val success: Boolean,
    val message: String? = null,
    val needsEmailVerification: Boolean = false,
)

class ChatAuthRepository {
    private val auth get() = SupabaseProvider.client.auth

    fun isSignedIn(): Boolean = auth.currentUserOrNull() != null
    fun currentUserId(): String? = auth.currentUserOrNull()?.id
    fun currentEmail(): String? = auth.currentUserOrNull()?.email

    suspend fun getProfile(): Result<ChatProfile?> = runCatching {
        val userId = currentUserId() ?: return@runCatching null
        SupabaseProvider.client
            .from("chat_profiles")
            .select { filter { eq("user_id", userId) } }
            .decodeList<ChatProfile>()
            .firstOrNull()
    }

    suspend fun signUp(email: String, password: String, username: String): ChatAuthResult {
        val normalizedEmail = email.trim()
        val normalizedUsername = username.trim().removePrefix("@").lowercase()
        if (!EMAIL_REGEX.matches(normalizedEmail)) return ChatAuthResult(false, "Enter a valid email address.")
        if (!USERNAME_REGEX.matches(normalizedUsername)) return ChatAuthResult(false, "Username must be 3–20 characters using letters, numbers, _ or .")
        if (password.length < 8) return ChatAuthResult(false, "Password must be at least 8 characters.")
        return try {
            auth.signUpWith(Email) {
                this.email = normalizedEmail
                this.password = password
                data = buildJsonObject { put("chat_username", normalizedUsername) }
            }
            ChatAuthResult(true, "Check your email to verify your account, then return to KFCC Chat.", true)
        } catch (error: Exception) {
            ChatAuthResult(false, error.message ?: "Unable to create your account.")
        }
    }

    suspend fun signIn(email: String, password: String): ChatAuthResult = try {
        auth.signInWith(Email) {
            this.email = email.trim()
            this.password = password
        }
        ChatAuthResult(true)
    } catch (error: Exception) {
        ChatAuthResult(false, error.message ?: "Unable to sign in.")
    }

    suspend fun completeProfile(username: String? = null): ChatAuthResult {
        val userId = currentUserId() ?: return ChatAuthResult(false, "Please sign in first.")
        val metadataUsername = auth.currentUserOrNull()?.userMetadata?.get("chat_username")?.toString()?.trim('"')
        val normalizedUsername = (username ?: metadataUsername.orEmpty()).trim().removePrefix("@").lowercase()
        if (!USERNAME_REGEX.matches(normalizedUsername)) return ChatAuthResult(false, "Choose a username to continue.")
        return try {
            val existing = SupabaseProvider.client.from("chat_profiles")
                .select { filter { eq("username", normalizedUsername) } }
                .decodeList<ChatProfile>()
            if (existing.any { it.user_id != userId }) return ChatAuthResult(false, "That username is already in use.")
            SupabaseProvider.client.from("chat_profiles")
                .upsert(ChatProfile(user_id = userId, username = normalizedUsername))
            SupabaseProvider.client.postgrest.rpc("join_kfcc_community")
            ChatAuthResult(true)
        } catch (error: Exception) {
            ChatAuthResult(false, error.message ?: "Unable to finish your chat profile.")
        }
    }

    suspend fun signOut() { auth.signOut() }

    companion object {
        private val EMAIL_REGEX = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
        private val USERNAME_REGEX = Regex("^[a-z0-9_.]{3,20}$")
    }
}
