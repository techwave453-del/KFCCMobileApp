package com.example.helloworld.admin.users

import android.content.Context
import com.example.helloworld.admin.AdminRepository
import io.ktor.client.call.body
import io.ktor.http.HttpStatusCode

/** Server-authoritative Users & Permissions API client. */
class AdminUsersRepository(context: Context) {
    private val adminRepository = AdminRepository(context.applicationContext)

    private suspend fun check(response: io.ktor.client.statement.HttpResponse) {
        if (response.status !in 200..299) {
            val message = try { response.body<Map<String, String>>()["error"] } catch (_: Exception) { null }
            error(message ?: "Administrator service returned ${response.status.value}.")
        }
    }

    suspend fun users(): Result<List<AdminManagedUser>> = runCatching {
        val response = adminRepository.authenticatedGet("api/admin/users")
        check(response)
        response.body()
    }

    suspend fun accessRequests(): Result<List<AdminAccessRequest>> = runCatching {
        val response = adminRepository.authenticatedGet("api/admin/access/requests")
        check(response)
        response.body()
    }

    suspend fun approveRequest(id: Long, role: String, permissions: List<String>): Result<Unit> = runCatching {
        val response = adminRepository.authenticatedPost(
            "api/admin/access/requests/$id/approve",
            mapOf("role" to role, "permissions" to permissions)
        )
        check(response)
    }

    suspend fun rejectRequest(id: Long): Result<Unit> = runCatching {
        val response = adminRepository.authenticatedPost("api/admin/access/requests/$id/reject")
        check(response)
    }

    suspend fun setStatus(id: Long, active: Boolean): Result<Unit> = runCatching {
        val response = adminRepository.authenticatedPatch(
            "api/admin/users/$id/status",
            mapOf("is_active" to active)
        )
        check(response)
    }

    suspend fun deleteUser(id: Long): Result<Unit> = runCatching {
        val response = adminRepository.authenticatedDelete("api/admin/users/$id")
        check(response)
    }

    suspend fun setPermissions(id: Long, permissions: List<String>): Result<Unit> = runCatching {
        val response = adminRepository.authenticatedPut(
            "api/admin/users/$id/permissions",
            mapOf("permissions" to permissions)
        )
        check(response)
    }
}
