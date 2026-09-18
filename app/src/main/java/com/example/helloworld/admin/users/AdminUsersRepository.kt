package com.example.helloworld.admin.users

import android.content.Context
import com.example.helloworld.data.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.Serializable

@Serializable
private data class ProfileRow(
    val auth_user_id: String,
    val legacy_admin_user_id: Long? = null,
    val display_name: String? = null,
    val role: String,
    val is_active: Boolean,
    val login_email: String? = null,
    val created_at: String? = null
)

@Serializable
private data class PermissionRow(val permission: String)

class AdminUsersRepository(@Suppress("UNUSED_PARAMETER") context: Context) {
    private val client get() = SupabaseProvider.client

    suspend fun users(): Result<List<AdminManagedUser>> = runCatching {
        val profiles = client.from("admin_profiles").select().decodeList<ProfileRow>()
        profiles.map { profile ->
            val permissions = client.from("admin_user_permissions").select {
                filter { eq("auth_user_id", profile.auth_user_id) }
            }.decodeList<PermissionRow>().map { it.permission }

            AdminManagedUser(
                id = profile.legacy_admin_user_id ?: 0L,
                username = profile.display_name.orEmpty(),
                role = profile.role,
                is_active = profile.is_active,
                permissions = permissions,
                created_at = profile.created_at
            )
        }
    }

    suspend fun accessRequests(): Result<List<AdminAccessRequest>> =
        Result.success(emptyList())

    suspend fun approveRequest(id: Long, role: String, permissions: List<String>): Result<Unit> =
        Result.failure(UnsupportedOperationException(
            "New administrator approval is now handled by Auth account identity and will be connected to this screen next."
        ))

    suspend fun rejectRequest(id: Long): Result<Unit> =
        Result.failure(UnsupportedOperationException(
            "Legacy access requests are no longer used by the direct Supabase administrator boundary."
        ))

    suspend fun setStatus(id: Long, active: Boolean): Result<Unit> = runCatching {
        client.from("admin_profiles").update({
            set("is_active", active)
        }) {
            filter { eq("legacy_admin_user_id", id) }
        }
    }

    suspend fun deleteUser(id: Long): Result<Unit> = runCatching {
        client.from("admin_profiles").delete {
            filter { eq("legacy_admin_user_id", id) }
        }
    }

    suspend fun setPermissions(id: Long, permissions: List<String>): Result<Unit> = runCatching {
        val profile = client.from("admin_profiles").select {
            filter { eq("legacy_admin_user_id", id) }
        }.decodeList<ProfileRow>().firstOrNull()
            ?: error("Administrator account not found.")

        client.from("admin_user_permissions").delete {
            filter { eq("auth_user_id", profile.auth_user_id) }
        }

        permissions.distinct().forEach { permission ->
            client.from("admin_user_permissions").insert(
                mapOf("auth_user_id" to profile.auth_user_id, "permission" to permission)
            )
        }
    }
}
