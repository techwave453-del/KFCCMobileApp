package com.example.helloworld.admin.users

import android.content.Context
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.ContentNegotiation
import io.ktor.client.plugins.cookies.HttpCookies
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

class AdminUsersRepository(context: Context) {
    companion object {
        private const val BASE_URL = "https://kingdomfellowshipchristianchurch.onrender.com/"
        private const val PREFS = "kfcc_admin_session"
        private const val COOKIE_KEY = "session_cookie"
    }

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; coerceInputValues = true }) }
        install(HttpCookies)
    }

    private fun addSession(builder: io.ktor.client.request.HttpRequestBuilder) {
        val raw = prefs.getString(COOKIE_KEY, null) ?: return
        val name = raw.substringBefore('=')
        val value = raw.substringAfter('=', "")
        if (name.isNotBlank()) builder.cookie(name, value)
    }

    private suspend fun check(response: io.ktor.client.statement.HttpResponse): String? {
        if (response.status.value in 200..299) return null
        return try { response.body<Map<String, String>>() ["error"] } catch (_: Exception) { null }
    }

    suspend fun users(): Result<List<AdminManagedUser>> = runCatching {
        val response = client.get(BASE_URL + "api/admin/users") { addSession(this) }
        check(response)?.let { error(it) }
        response.body()
    }

    suspend fun accessRequests(): Result<List<AdminAccessRequest>> = runCatching {
        val response = client.get(BASE_URL + "api/admin/access/requests") { addSession(this) }
        check(response)?.let { error(it) }
        response.body()
    }

    suspend fun approveRequest(id: Long, role: String, permissions: List<String>): Result<Unit> = runCatching {
        val response = client.post(BASE_URL + "api/admin/access/requests/$id/approve") {
            addSession(this); contentType(ContentType.Application.Json)
            setBody(mapOf("role" to role, "permissions" to permissions))
        }
        check(response)?.let { error(it) }
    }

    suspend fun rejectRequest(id: Long): Result<Unit> = runCatching {
        val response = client.post(BASE_URL + "api/admin/access/requests/$id/reject") { addSession(this) }
        check(response)?.let { error(it) }
    }

    suspend fun setStatus(id: Long, active: Boolean): Result<Unit> = runCatching {
        val response = client.patch(BASE_URL + "api/admin/users/$id/status") {
            addSession(this); contentType(ContentType.Application.Json); setBody(mapOf("is_active" to active))
        }
        check(response)?.let { error(it) }
    }

    suspend fun deleteUser(id: Long): Result<Unit> = runCatching {
        val response = client.delete(BASE_URL + "api/admin/users/$id") { addSession(this) }
        check(response)?.let { error(it) }
    }

    suspend fun setPermissions(id: Long, permissions: List<String>): Result<Unit> = runCatching {
        val response = client.put(BASE_URL + "api/admin/users/$id/permissions") {
            addSession(this); contentType(ContentType.Application.Json); setBody(mapOf("permissions" to permissions))
        }
        check(response)?.let { error(it) }
    }
}
