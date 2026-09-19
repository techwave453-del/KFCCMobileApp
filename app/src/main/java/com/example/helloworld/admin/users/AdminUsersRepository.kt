package com.example.helloworld.admin.users

import android.content.Context
import com.example.helloworld.data.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.Serializable

/** Server-authoritative Users & Permissions API client using Supabase directly. */
class AdminUsersRepository(context: Context) {
    private val client = SupabaseProvider.client

    suspend fun users(): Result<List<AdminManagedUser>> = runCatching {
        client.from("administrators")
            .select()
            .decodeList<AdminManagedUser>()
    }

    suspend fun accessRequests(): Result<List<AdminAccessRequest>> = runCatching {
        client.from("admin_access_requests")
            .select()
            .decodeList<AdminAccessRequest>()
    }

    suspend fun approveRequest(id: Long, role: String, permissions: List<String>): Result<Unit> = runCatching {
        // Approving a request involves complex logic (creating auth user or updating role).
        // For a direct Supabase implementation, we might need an Edge Function.
        // If we want to stay independent of the webserver, we should use a Supabase Function.
        // Assuming we have an 'approve_admin_request' RPC or function.
        client.from("admin_access_requests").update({
            set("status", "approved")
            set("role", role)
            set("permissions", permissions)
        }) {
            filter { eq("id", id) }
        }
    }

    suspend fun rejectRequest(id: Long): Result<Unit> = runCatching {
        client.from("admin_access_requests").update({
            set("status", "rejected")
        }) {
            filter { eq("id", id) }
        }
    }

    suspend fun setStatus(id: Long, active: Boolean): Result<Unit> = runCatching {
        client.from("administrators").update({
            set("is_active", active)
        }) {
            filter { eq("id", id) }
        }
    }

    suspend fun deleteUser(id: Long): Result<Unit> = runCatching {
        client.from("administrators").delete {
            filter { eq("id", id) }
        }
    }

    suspend fun setPermissions(id: Long, permissions: List<String>): Result<Unit> = runCatching {
        client.from("administrators").update({
            set("permissions", permissions)
        }) {
            filter { eq("id", id) }
        }
    }
}
