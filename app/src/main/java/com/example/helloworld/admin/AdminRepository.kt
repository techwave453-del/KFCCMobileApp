package com.example.helloworld.admin

import android.content.Context
import com.example.helloworld.config.AppConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
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
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
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

    private fun HttpRequestBuilder.withSessionCookie() {
        sessionCookie()?.let {
            cookie(it.substringBefore('='), it.substringAfter('='))
        }
    }

    private fun url(path: String): String =
        AppConfig.ADMIN_API_BASE_URL.trimEnd('/') + "/" + path.trimStart('/')

    suspend fun authenticatedGet(path: String): HttpResponse =
        client.get(url(path)) { withSessionCookie() }.also(::clearOnUnauthorized)

    suspend fun authenticatedPost(path: String, body: Any? = null): HttpResponse =
        client.post(url(path)) {
            withSessionCookie()
            if (body != null) {
                contentType(ContentType.Application.Json)
                setBody(body)
            }
        }.also(::clearOnUnauthorized)

    suspend fun authenticatedPut(path: String, body: Any): HttpResponse =
        client.put(url(path)) {
            withSessionCookie()
            contentType(ContentType.Application.Json)
            setBody(body)
        }.also(::clearOnUnauthorized)

    suspend fun authenticatedPatch(path: String, body: Any): HttpResponse =
        client.patch(url(path)) {
            withSessionCookie()
            contentType(ContentType.Application.Json)
            setBody(body)
        }.also(::clearOnUnauthorized)

    suspend fun authenticatedDelete(path: String): HttpResponse =
        client.delete(url(path)) { withSessionCookie() }.also(::clearOnUnauthorized)

    private fun clearOnUnauthorized(response: HttpResponse) {
        if (response.status == HttpStatusCode.Unauthorized) clearSession()
    }

    fun clearStoredSession() = clearSession()

    suspend fun restoreSession(): AdminUser? {
        return try {
            val response = authenticatedGet(AppConfig.ADMIN_ME_PATH)
            if (response.status == HttpStatusCode.OK) response.body<AdminMeResponse>().user else null
        } catch (_: Exception) {
            null
        }
    }

    suspend fun login(username: String, password: String): AdminLoginResponse {
        return try {
            val response = client.post(url(AppConfig.ADMIN_LOGIN_PATH)) {
                contentType(ContentType.Application.Json)
                setBody(AdminLoginRequest(username.trim(), password))
            }
            if (response.status == HttpStatusCode.OK) {
                saveSession(response)
                response.body<AdminLoginResponse>()
            } else {
                try { response.body<AdminLoginResponse>() }
                catch (_: Exception) { AdminLoginResponse(error = "Login failed (${response.status.value}).") }
            }
        } catch (e: Exception) {
            AdminLoginResponse(error = e.message ?: "Unable to connect to the administrator service.")
        }
    }

    suspend fun logout() {
        try { authenticatedPost(AppConfig.ADMIN_LOGOUT_PATH) }
        catch (_: Exception) { }
        finally { clearSession() }
    }
}
