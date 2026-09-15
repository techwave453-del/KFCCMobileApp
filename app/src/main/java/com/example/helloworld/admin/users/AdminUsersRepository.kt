package com.example.helloworld.admin.users

import android.content.Context
import com.example.helloworld.admin.AdminRepository
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

@Serializable
private data class ApproveRequestBody(
    val role: String,
    val permissions: List<String>
)

@Serializable
private data class StatusRequestBody(val is_active: Boolean)

@Serializable
private data class PermissionsRequestBody(val permissions: List<String>)

/** Server-authoritative Users & Permissions API client. */
class AdminUsersRepository(context: Context) {
    private val adminRepository = AdminRepository(context.applicationContext)

    private suspend fun check(response: HttpResponse) {
        if (response.status.value !in 200..299) {
            val message = try {
                response.body<JsonObject>()["error"]?.jsonPrimitive?.contentOrNull
            } catch (_: Exception) {
                null
            }
            error(message ?: "Administrator service returned ${response.status.value}.")
        }
    }

    suspend fun users(): Result<List<AdminManagedUser>> {
        return try {
            val response = adminRepository.authenticatedGet("api/admin/users")
            check(response)
            val users: List<AdminManagedUser> = response.body()
            Result.success<List<AdminManagedUser>>(users)
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    suspend fun accessRequests(): Result<List<AdminAccessRequest>> {
        return try {
            val response = adminRepository.authenticatedGet("api/admin/access/requests")
            check(response)
            val requests: List<AdminAccessRequest> = response.body()
            Result.success<List<AdminAccessRequest>>(requests)
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    suspend fun approveRequest(id: Long, role: String, permissions: List<String>): Result<Unit> {
        return try {
            val response = adminRepository.authenticatedPost(
                "api/admin/access/requests/$id/approve",
                ApproveRequestBody(role, permissions)
            )
            check(response)
            Result.success<Unit>(Unit)
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    suspend fun rejectRequest(id: Long): Result<Unit> {
        return try {
            val response = adminRepository.authenticatedPost("api/admin/access/requests/$id/reject")
            check(response)
            Result.success<Unit>(Unit)
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    suspend fun setStatus(id: Long, active: Boolean): Result<Unit> {
        return try {
            val response = adminRepository.authenticatedPatch(
                "api/admin/users/$id/status",
                StatusRequestBody(active)
            )
            check(response)
            Result.success<Unit>(Unit)
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    suspend fun deleteUser(id: Long): Result<Unit> {
        return try {
            val response = adminRepository.authenticatedDelete("api/admin/users/$id")
            check(response)
            Result.success<Unit>(Unit)
        } catch (error: Exception) {
            Result.failure(error)
        }
    }

    suspend fun setPermissions(id: Long, permissions: List<String>): Result<Unit> {
        return try {
            val response = adminRepository.authenticatedPut(
                "api/admin/users/$id/permissions",
                PermissionsRequestBody(permissions)
            )
            check(response)
            Result.success<Unit>(Unit)
        } catch (error: Exception) {
            Result.failure(error)
        }
    }
}
