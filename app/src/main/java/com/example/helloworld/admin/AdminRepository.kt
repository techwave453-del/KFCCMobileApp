package com.example.helloworld.admin

import android.content.Context
import com.example.helloworld.config.AppConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
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

    /**
     * Persist the Express session cookie returned by the web API.
     * Prefer the session cookie explicitly rather than assuming it is the
     * first Set-Cookie header returned by the server.
     */
    private fun saveSession(response: HttpResponse) {
        val cookie = response.headers.getAll(HttpHeaders.SetCookie)
            ?.asSequence()
            ?.map { it.substringBefore(';').trim() }
            ?.firstOrNull { it.startsWith("connect.sid=") && it.contains('=') }
            ?: response.headers.getAll(HttpHeaders.SetCookie)
                ?.asSequence()
                ?.map { it.substringBefore(';').trim() }
                ?.firstOrNull { it.contains('=') }

        if (!cookie.isNullOrBlank()) sessionStore.save(cookie)
    }

    private fun clearSession() = sessionStore.clear()

    /**
     * Android is not a browser, so it does not naturally participate in the
     * browser Origin/Referer checks used by the web admin. Send the API's
     * canonical origin explicitly and pass the persisted session cookie as a
     * raw Cookie header so Express receives the exact cookie value.
     */
    private fun HttpRequestBuilder.withApiHeaders() {
        header(HttpHeaders.Accept, ContentType.Application.Json.toString())
        header(HttpHeaders.Origin, AppConfig.ADMIN_API_ORIGIN)
    }

    private fun HttpRequestBuilder.withSessionCookie() {
        sessionCookie()?.let { header(HttpHeaders.Cookie, it) }
    }

    private fun HttpRequestBuilder.withJsonContentType() {
        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
    }

    private fun HttpRequestBuilder.withAuthenticatedHeaders() {
        withApiHeaders()
        withSessionCookie()
    }

    private fun url(path: String): String =
        AppConfig.ADMIN_API_BASE_URL.trimEnd('/') + "/" + path.trimStart('/')

    suspend fun authenticatedGet(path: String): HttpResponse =
        client.get(url(path)) { withAuthenticatedHeaders() }.also(::clearOnUnauthorized)

    suspend fun authenticatedPost(path: String, body: Any? = null): HttpResponse =
        client.post(url(path)) {
            withAuthenticatedHeaders()
            if (body != null) {
                withJsonContentType()
                setBody(body)
            }
        }.also(::clearOnUnauthorized)

    suspend fun authenticatedPut(path: String, body: Any): HttpResponse =
        client.put(url(path)) {
            withAuthenticatedHeaders()
            withJsonContentType()
            setBody(body)
        }.also(::clearOnUnauthorized)

    suspend fun authenticatedPatch(path: String, body: Any): HttpResponse =
        client.patch(url(path)) {
            withAuthenticatedHeaders()
            withJsonContentType()
            setBody(body)
        }.also(::clearOnUnauthorized)

    suspend fun authenticatedDelete(path: String): HttpResponse =
        client.delete(url(path)) { withAuthenticatedHeaders() }.also(::clearOnUnauthorized)

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
                withApiHeaders()
                withJsonContentType()
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
