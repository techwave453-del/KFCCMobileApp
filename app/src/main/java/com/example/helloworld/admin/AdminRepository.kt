package com.example.helloworld.admin

import android.content.Context
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.ContentNegotiation
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.cookie
import io.ktor.client.request.contentType
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.plugins.contentnegotiation.json
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.Json

class AdminRepository(context: Context) {
    companion object {
        private const val PREFS = "kfcc_admin_session"
        private const val COOKIE_KEY = "session_cookie"
        private const val BASE_URL = "https://kingdomfellowshipchristianchurch.onrender.com/"
    }

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; coerceInputValues = true })
        }
        install(HttpCookies)
    }

    private fun sessionCookie(): String? = prefs.getString(COOKIE_KEY, null)

    private fun saveSession(response: HttpResponse) {
        val cookie = response.headers.getAll(HttpHeaders.SetCookie)
            ?.firstOrNull()
            ?.substringBefore(';')
            ?.takeIf { it.contains('=') }
        if (!cookie.isNullOrBlank()) prefs.edit().putString(COOKIE_KEY, cookie).apply()
    }

    private fun clearSession() = prefs.edit().remove(COOKIE_KEY).apply()

    private suspend fun <T> withSession(block: suspend () -> T): T = block()

    suspend fun restoreSession(): AdminUser? {
        return try {
            val response = client.get(BASE_URL + "api/admin/me") {
                sessionCookie()?.let { cookie(it.substringBefore('='), it.substringAfter('=')) }
            }
            if (response.status == HttpStatusCode.OK) response.body<AdminMeResponse>().user else null
        } catch (_: Exception) {
            null
        }
    }

    suspend fun login(username: String, password: String): AdminLoginResponse {
        return try {
            val response = client.post(BASE_URL + "api/admin/login") {
                contentType(ContentType.Application.Json)
                setBody(AdminLoginRequest(username.trim(), password))
            }
            if (response.status == HttpStatusCode.OK) {
                saveSession(response)
                response.body()
            } else {
                try {
                    response.body<AdminLoginResponse>()
                } catch (_: Exception) {
                    AdminLoginResponse(error = "Login failed (${response.status.value}).")
                }
            }
        } catch (e: Exception) {
            AdminLoginResponse(error = e.message ?: "Unable to connect to the administrator service.")
        }
    }

    suspend fun logout() {
        try {
            client.post(BASE_URL + "api/admin/logout") {
                sessionCookie()?.let { cookie(it.substringBefore('='), it.substringAfter('=')) }
            }
        } catch (_: Exception) {
        } finally {
            clearSession()
        }
    }
}
