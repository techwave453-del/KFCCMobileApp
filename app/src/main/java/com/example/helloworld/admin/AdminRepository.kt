package com.example.helloworld.admin

import android.content.Context
import com.example.helloworld.config.AppConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.ContentNegotiation
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.cookie
import io.ktor.client.request.contentType
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.plugins.contentnegotiation.json
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.Json

class AdminRepository(context: Context) {
    private val sessionStore = SessionStore(context.applicationContext)
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; coerceInputValues = true })
        }
        install(HttpCookies)
    }

    private fun sessionCookie(): String? = sessionStore.get()

    private fun saveSession(response: HttpResponse) {
        val cookie = response.headers.getAll(HttpHeaders.SetCookie)
            ?.firstOrNull()
            ?.substringBefore(';')
            ?.takeIf { it.contains('=') }
        if (!cookie.isNullOrBlank()) sessionStore.save(cookie)
    }

    private fun clearSession() = sessionStore.clear()

    private fun io.ktor.client.request.HttpRequestBuilder.withSessionCookie() {
        sessionCookie()?.let {
            cookie(it.substringBefore('='), it.substringAfter('='))
        }
    }

    /** Authenticated GET shared by every admin feature module. */
    suspend fun authenticatedGet(path: String): HttpResponse {
        val response = client.get(AppConfig.ADMIN_API_BASE_URL + path.trimStart('/')) {
            withSessionCookie()
        }
        if (response.status == HttpStatusCode.Unauthorized) clearSession()
        return response
    }

    /** Authenticated JSON PUT shared by admin feature modules. */
    suspend fun authenticatedPut(path: String, body: Map<String, String>): HttpResponse {
        val response = client.put(AppConfig.ADMIN_API_BASE_URL + path.trimStart('/')) {
            withSessionCookie()
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        if (response.status == HttpStatusCode.Unauthorized) clearSession()
        return response
    }

    suspend fun restoreSession(): AdminUser? {
        return try {
            val response = authenticatedGet(AppConfig.ADMIN_ME_PATH)
            when (response.status) {
                HttpStatusCode.OK -> response.body<AdminMeResponse>().user
                HttpStatusCode.Unauthorized -> null
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun login(username: String, password: String): AdminLoginResponse {
        return try {
            val response = client.post(AppConfig.ADMIN_API_BASE_URL + AppConfig.ADMIN_LOGIN_PATH) {
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
            client.post(AppConfig.ADMIN_API_BASE_URL + AppConfig.ADMIN_LOGOUT_PATH) {
                withSessionCookie()
            }
        } catch (_: Exception) {
        } finally {
            clearSession()
        }
    }
}
