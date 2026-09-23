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
private data class ManagementRequest(
    val action: String,
    val id: Long? = null,
    val active: Boolean? = null,
    val role: String? = null,
    val permissions: List<String>? = null
)

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

    private suspend fun call(request: ManagementRequest): ManagementResponse {
        val token = client.auth.currentAccessTokenOrNull()
            ?: error("Administrator session has expired. Please login again.")
        return http.post("$FUNCTIONS_URL/admin-management") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun users(): Result<List<AdminManagedUser>> = runCatching {
        val r = call(ManagementRequest("list")); if (r.error != null) error(r.error); r.users
    }

    suspend fun accessRequests(): Result<List<AdminAccessRequest>> = runCatching {
        val r = call(ManagementRequest("requests")); if (r.error != null) error(r.error); r.requests
    }

    suspend fun approveRequest(id: Long, role: String, permissions: List<String>): Result<String?> = runCatching {
        val r = call(ManagementRequest("approve", id = id, role = role, permissions = permissions))
        if (r.error != null) error(r.error); r.activation_code
    }

    suspend fun rejectRequest(id: Long): Result<Unit> = runCatching {
        val r = call(ManagementRequest("reject", id = id)); if (r.error != null) error(r.error)
    }

    suspend fun setStatus(id: Long, active: Boolean): Result<Unit> = runCatching {
        val r = call(ManagementRequest("status", id = id, active = active)); if (r.error != null) error(r.error)
    }

    suspend fun deleteUser(id: Long): Result<Unit> = runCatching {
        val r = call(ManagementRequest("delete", id = id)); if (r.error != null) error(r.error)
    }

    suspend fun setPermissions(id: Long, permissions: List<String>): Result<Unit> = runCatching {
        val r = call(ManagementRequest("permissions", id = id, permissions = permissions)); if (r.error != null) error(r.error)
    }

    companion object {
        private const val FUNCTIONS_URL = "https://uhzfjuquhqxhqtppispq.supabase.co/functions/v1"
    }
}
