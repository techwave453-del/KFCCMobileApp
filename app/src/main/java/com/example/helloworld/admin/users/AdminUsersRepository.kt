package com.example.helloworld.admin.users

import android.content.Context
import com.example.helloworld.data.SupabaseProvider
import io.github.jan.supabase.auth.auth
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class ManagementResponse(
    val users: List<AdminManagedUser> = emptyList(),
    val requests: List<AdminAccessRequest> = emptyList(),
    val ok: Boolean = false,
    val error: String? = null,
    val activation_code: String? = null
)

class AdminUsersRepository(context: Context) {
    private val client = SupabaseProvider.client
    private val http = HttpClient(CIO) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }

    private suspend fun call(action: String, extra: Map<String, Any?> = emptyMap()): ManagementResponse {
        val token = client.auth.currentAccessTokenOrNull()
            ?: error("Administrator session has expired. Please login again.")
        return http.post("$FUNCTIONS_URL/admin-management") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(mapOf("action" to action) + extra)
        }.body()
    }

    suspend fun users(): Result<List<AdminManagedUser>> = runCatching {
        val r = call("list"); if (r.error != null) error(r.error); r.users
    }

    suspend fun accessRequests(): Result<List<AdminAccessRequest>> = runCatching {
        val r = call("requests"); if (r.error != null) error(r.error); r.requests
    }

    suspend fun approveRequest(id: Long, role: String, permissions: List<String>): Result<String?> = runCatching {
        val r = call("approve", mapOf("id" to id, "role" to role, "permissions" to permissions))
        if (r.error != null) error(r.error); r.activation_code
    }

    suspend fun rejectRequest(id: Long): Result<Unit> = runCatching {
        val r = call("reject", mapOf("id" to id)); if (r.error != null) error(r.error)
    }

    suspend fun setStatus(id: Long, active: Boolean): Result<Unit> = runCatching {
        val r = call("status", mapOf("id" to id, "active" to active)); if (r.error != null) error(r.error)
    }

    suspend fun deleteUser(id: Long): Result<Unit> = runCatching {
        val r = call("delete", mapOf("id" to id)); if (r.error != null) error(r.error)
    }

    suspend fun setPermissions(id: Long, permissions: List<String>): Result<Unit> = runCatching {
        val r = call("permissions", mapOf("id" to id, "permissions" to permissions)); if (r.error != null) error(r.error)
    }

    companion object {
        private const val FUNCTIONS_URL = "https://uhzfjuquhqxhqtppispq.supabase.co/functions/v1"
    }
}
