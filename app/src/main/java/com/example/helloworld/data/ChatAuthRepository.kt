package com.example.helloworld.data

import android.util.Base64
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.user.UserSession
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
private data class ChatAuthJwtPayload(val sub: String)

@Serializable
data class ChatProfile(
    val user_id: String,
    val username: String,
    val display_name: String? = null,
    val avatar_url: String? = null,
    val admin_role: String? = null,
    val is_admin_visible: Boolean = false,
)

data class ChatAuthResult(
    val success: Boolean,
    val message: String? = null,
    val needsEmailVerification: Boolean = false,
)

@Serializable
private data class UsernameLoginRequest(
    val username: String,
    val password: String,
)

@Serializable
private data class UsernameLoginResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_in") val expiresIn: Int,
    @SerialName("token_type") val tokenType: String = "Bearer",
)

class ChatAuthRepository {
    private val auth get() = SupabaseProvider.client.auth

    private val usernameLoginClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    fun isSignedIn(): Boolean = auth.currentSessionOrNull() != null
    fun currentUserId(): String? {
        val user = auth.currentUserOrNull() ?: auth.currentSessionOrNull()?.user
        if (user != null) return user.id

        // Fallback: Decode the 'sub' claim from the JWT if the user object is not yet loaded.
        // This is critical for Edge Function based login where the user object is initially null.
        val token = auth.currentSessionOrNull()?.accessToken ?: return null
        return try {
            val parts = token.split(".")
            if (parts.size != 3) return null
            val payload = String(Base64.decode(parts[1], Base64.URL_SAFE))
            Json.decodeFromString<ChatAuthJwtPayload>(payload).sub
        } catch (_: Exception) { null }
    }
    fun currentEmail(): String? = auth.currentUserOrNull()?.email ?: auth.currentSessionOrNull()?.user?.email

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

    /**
     * Member authentication by username. The username is resolved on the
     * Supabase Edge Function; the user's email is never exposed to the app.
     * Supabase Auth still performs the actual password verification.
     */
    suspend fun signInWithUsername(username: String, password: String): ChatAuthResult {
        val normalizedUsername = username.trim().removePrefix("@").lowercase()
        if (!USERNAME_REGEX.matches(normalizedUsername)) {
            return ChatAuthResult(false, "Enter your username.")
        }
        if (password.isBlank()) return ChatAuthResult(false, "Enter your password.")

        return try {
            val response = usernameLoginClient.post(
                "${SUPABASE_FUNCTIONS_URL}/chat-login"
            ) {
                contentType(ContentType.Application.Json)
                setBody(UsernameLoginRequest(normalizedUsername, password))
            }

            if (response.status.value !in 200..299) {
                ChatAuthResult(false, "Invalid username or password.")
            } else {
                val session = response.body<UsernameLoginResponse>()
                auth.importSession(
                    UserSession(
                        accessToken = session.accessToken,
                        refreshToken = session.refreshToken,
                        expiresIn = session.expiresIn.toLong(),
                        tokenType = session.tokenType,
                        user = null
                    )
                )
                ChatAuthResult(true)
            }
        } catch (error: Exception) {
            ChatAuthResult(false, error.message ?: "Unable to sign in.")
        }
    }

    /**
     * Kept for compatibility with existing callers that still have an email.
     */
    suspend fun signIn(email: String, password: String): ChatAuthResult = try {
        auth.signInWith(Email) {
            this.email = email.trim()
            this.password = password
        }
        ChatAuthResult(true)
    } catch (error: Exception) {
        ChatAuthResult(false, error.message ?: "Unable to sign in.")
    }

    suspend fun completeProfile(
        username: String? = null,
        avatarUrl: String? = null,
        adminRole: String? = null,
        isAdminVisible: Boolean? = null
    ): ChatAuthResult {
        val userId = currentUserId() ?: return ChatAuthResult(false, "Please sign in first.")
        val metadataUsername = auth.currentUserOrNull()?.userMetadata?.get("chat_username")?.toString()?.trim('"')
        val normalizedUsername = (username ?: metadataUsername.orEmpty()).trim().removePrefix("@").lowercase()
        if (!USERNAME_REGEX.matches(normalizedUsername)) return ChatAuthResult(false, "Choose a username to continue.")
        
        return try {
            val existing = SupabaseProvider.client.from("chat_profiles")
                .select { filter { eq("username", normalizedUsername) } }
                .decodeList<ChatProfile>()
            
            if (existing.any { it.user_id != userId }) return ChatAuthResult(false, "That username is already in use.")
            
            val current = existing.firstOrNull { it.user_id == userId }
            val newProfile = ChatProfile(
                user_id = userId,
                username = normalizedUsername,
                avatar_url = avatarUrl ?: current?.avatar_url,
                admin_role = adminRole ?: current?.admin_role,
                is_admin_visible = isAdminVisible ?: current?.is_admin_visible ?: false
            )
            
            SupabaseProvider.client.from("chat_profiles").upsert(newProfile)
            SupabaseProvider.client.postgrest.rpc("join_kfcc_community")
            ChatAuthResult(true)
        } catch (error: Exception) {
            ChatAuthResult(false, error.message ?: "Unable to finish your chat profile.")
        }
    }

    suspend fun updateAdminRole(role: String): ChatAuthResult = runCatching {
        val userId = currentUserId() ?: return ChatAuthResult(false, "Not signed in.")
        SupabaseProvider.client.from("chat_profiles")
            .update(mapOf("admin_role" to role)) {
                filter { eq("user_id", userId) }
            }
        ChatAuthResult(true)
    }.getOrElse { ChatAuthResult(false, it.message) }

    suspend fun updateAdminVisibility(visible: Boolean): ChatAuthResult = runCatching {
        val userId = currentUserId() ?: return ChatAuthResult(false, "Not signed in.")
        SupabaseProvider.client.from("chat_profiles")
            .update(mapOf("is_admin_visible" to visible)) {
                filter { eq("user_id", userId) }
            }
        ChatAuthResult(true)
    }.getOrElse { ChatAuthResult(false, it.message) }

    suspend fun updateEmail(newEmail: String): ChatAuthResult = try {
        auth.updateUser {
            email = newEmail.trim()
        }
        ChatAuthResult(true, "A confirmation email has been sent to your new address. Please verify to complete the change.")
    } catch (e: Exception) {
        ChatAuthResult(false, e.message ?: "Unable to update email.")
    }

    suspend fun resetPassword(username: String, email: String): ChatAuthResult = try {
        val normalizedUsername = username.trim().removePrefix("@").lowercase()
        val normalizedEmail = email.trim()

        // 1. Secure server-side verification: Confirm the username matches the specific email.
        // We call a secure Edge Function that bypasses client RLS to verify the mapping 
        // without exposing any private account data.
        val response = usernameLoginClient.post(
            "${SUPABASE_FUNCTIONS_URL}/chat-reset-verify"
        ) {
            contentType(ContentType.Application.Json)
            setBody(buildJsonObject {
                put("username", normalizedUsername)
                put("email", normalizedEmail)
            })
        }

        if (response.status.value !in 200..299) {
            return ChatAuthResult(false, "The username and email address provided do not match our records.")
        }

        // 2. Only after successful server-side match verification do we trigger the link delivery.
        auth.resetPasswordForEmail(email = normalizedEmail)
        ChatAuthResult(true, "A password reset link has been sent to your email address.")
    } catch (e: Exception) {
        ChatAuthResult(false, e.message ?: "Unable to send password reset email.")
    }

    suspend fun signOut() { auth.signOut() }

    companion object {
        private const val SUPABASE_FUNCTIONS_URL = "https://uhzfjuquhqxhqtppispq.supabase.co/functions/v1"
        private val EMAIL_REGEX = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
        private val USERNAME_REGEX = Regex("^[a-z0-9_.]{3,20}$")
    }
}
