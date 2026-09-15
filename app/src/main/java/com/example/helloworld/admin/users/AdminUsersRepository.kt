package com.example.helloworld.admin.users

import android.content.Context
import com.example.helloworld.admin.SessionStore
import com.example.helloworld.config.AppConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.ContentNegotiation
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.cookie
import io.ktor.client.request.contentType
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.Json

/** Server-authoritative Users & Permissions API client. */
class AdminUsersRepository(context: Context) {
    private val sessionStore = SessionStore(context)
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; coerceInputValues = true })
        }
        install(HttpCookies)
    }

    private fun url(path: String): String =
        AppConfig.ADMIN_API_BASE_URL.trimEnd('/') + "/" + path.trimStart('/')

    private fun addSession(builder: HttpRequestBuilder) {
        val raw = sessionStore.get() ?: return
        val name = raw.substringBefore('=')
        val value = raw.substringAfter('=', "")
        if (name.isNotBlank() && value.isNotBlank()) builder.cookie(name, value)
    }

    private suspend fun check(response: io.ktor.client.statement.HttpResponse): String? {
        if (response.status == HttpStatusCode.Unauthorized) {
            sessionStore.clear()
        }
        if (response.status.value in 200..299) return null
        return try {
            response.body<Map<String, String>>()["error"]
        } catch (_: Exception) {
            null
        }
    }

    suspend fun users(): Result<List<AdminManagedUser>> = runCatching {
        val response = client.get(url("api/admin/users")) { addSession(this) }
        check(response)?.let { error(it) }
        response.body()
    }

    suspend fun accessRequests(): Result<List<AdminAccessRequest>> = runCatching {
        val response = client.get(url("api/admin/access/requests")) { addSession(this) }
        check(response)?.let { error(it) }
        response.body()
    }

    suspend fun approveRequest(
        id: Long,
        role: String,
        permissions: List<String>
    ): Result<Unit> = runCatching {
        val response = client.post(url("api/admin/access/requests/$id/approve")) {
            addSession(this)
            contentType(ContentType.Application.Json)
            setBody(mapOf("role" to role, "permissions" to permissions))
        }
        check(response)?.let { error(it) }
    }

    suspend fun rejectRequest(id: Long): Result<Unit> = runCatching {
        val response = client.post(url("api/admin/access/requests/$id/reject")) {
            addSession(this)
        }
        check(response)?.let { error(it) }
    }

    suspend fun setStatus(id: Long, active: Boolean): Result<Unit> = runCatching {
        val response = client.patch(url("api/admin/users/$id/status")) {
            addSession(this)
            contentType(ContentType.Application.Json)
            setBody(mapOf("is_active" to active))
        }
        check(response)?.let { error(it) }
    }

    suspend fun deleteUser(id: Long): Result<Unit> = runCatching {
        val response = client.delete(url("api/admin/users/$id")) { addSession(this) }
        check(response)?.let { error(it) }
    }

    suspend fun setPermissions(id: Long, permissions: List<String>): Result<Unit> = runCatching {
        val response = client.put(url("api/admin/users/$id/permissions")) {
            addSession(this)
            contentType(ContentType.Application.Json)
            setBody(mapOf("permissions" to permissions))
        }
        check(response)?.let { error(it) }
    }
}
