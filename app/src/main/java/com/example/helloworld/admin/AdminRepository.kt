package com.example.helloworld.admin

import android.content.Context
import com.example.helloworld.data.SupabaseProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.Serializable

@Serializable
private data class AdminProfileRow(
    val auth_user_id: String,
    val legacy_admin_user_id: Long? = null,
    val display_name: String? = null,
    val login_email: String? = null,
    val role: String,
    val is_active: Boolean
)

@Serializable
private data class AdminPermissionRow(
    val permission: String
)

class AdminRepository(@Suppress("UNUSED_PARAMETER") context: Context) {
    private val client get() = SupabaseProvider.client
    private val auth get() = client.auth

    suspend fun restoreSession(): AdminUser? {
        val authUser = auth.currentUserOrNull() ?: return null
        return loadAdminUser(authUser.id)
    }

    suspend fun login(email: String, password: String): AdminLoginResponse {
        return try {
            val normalizedEmail = email.trim()
            if (normalizedEmail.isBlank() || password.isBlank()) {
                return AdminLoginResponse(error = "Enter your administrator email and password.")
            }

            auth.signInWith(Email) {
                this.email = normalizedEmail
                this.password = password
            }

            val authUser = auth.currentUserOrNull()
                ?: return AdminLoginResponse(error = "Administrator authentication did not create a session.")

            val admin = loadAdminUser(authUser.id)
            if (admin == null || !admin.is_active) {
                auth.signOut()
                return AdminLoginResponse(error = "This account is not authorized for KFCC administration.")
            }

            AdminLoginResponse(ok = true, user = admin)
        } catch (error: Exception) {
            AdminLoginResponse(error = error.message ?: "Unable to sign in to KFCC administration.")
        }
    }

    suspend fun logout() {
        runCatching { auth.signOut() }
    }

    private suspend fun loadAdminUser(userId: String): AdminUser? {
        val profile = client
            .from("admin_profiles")
            .select {
                filter { eq("auth_user_id", userId) }
            }
            .decodeList<AdminProfileRow>()
            .firstOrNull()
            ?: return null

        val permissions = client
            .from("admin_user_permissions")
            .select {
                filter { eq("auth_user_id", userId) }
            }
            .decodeList<AdminPermissionRow>()
            .map { it.permission }

        return AdminUser(
            id = profile.legacy_admin_user_id ?: 0L,
            username = profile.display_name.orEmpty(),
            email = profile.login_email.orEmpty(),
            role = profile.role,
            is_active = profile.is_active,
            permissions = permissions
        )
    }

    suspend fun postAnnouncement(title: String, message: String, type: String): Result<Unit> = runCatching {
        if (title.isBlank() || message.isBlank()) error("Title and message are required.")
        client.from("app_notifications").insert(
            mapOf(
                "user_id" to null,
                "title" to title.trim(),
                "message" to message.trim(),
                "type" to type.trim().ifBlank { "general" }
            )
        )
    }
}
