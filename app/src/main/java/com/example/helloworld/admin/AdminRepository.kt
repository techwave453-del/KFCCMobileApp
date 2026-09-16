package com.example.helloworld.admin

import android.content.Context
import com.example.helloworld.config.AppConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
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
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class AdminRepository(context: Context) {
    private val sessionStore = SessionStore(context.applicationContext)
    private val client = HttpClient(CIO) {
        expectSuccess = false

        // Render can cold-start the API and media uploads can legitimately take
        // longer than a normal JSON request. Keep a generous timeout at the
        // shared HTTP boundary so the mobile app does not abort an otherwise
        // valid upload while the server is processing it.
        install(HttpTimeout) {
            requestTimeoutMillis = 5 * 60 * 1000L
            connectTimeoutMillis = 30 * 1000L
            socketTimeoutMillis = 5 * 60 * 1000L
        }

        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; coerceInputValues = true })
        }
        install(HttpCookies)
    }

    private fun sessionCookie(): String? = sessionStore.get()

    private fun saveSession(response: HttpResponse) {
        val cookies = response.headers.getAll(HttpHeaders.SetCookie)
            .orEmpty()
            .asSequence()
            .map { it.substringBefore(';').trim() }
            .filter { it.contains('=') }
            .toList()

        val cookie = AppConfig.ADMIN_SESSION_COOKIE_NAMES
            .asSequence()
            .mapNotNull { name -> cookies.firstOrNull { it.startsWith("$name=") } }
            .firstOrNull()

        if (!cookie.isNullOrBlank()) sessionStore.save(cookie)
    }

    private fun clearSession() = sessionStore.clear()

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

    suspend fun authenticatedMultipartUpload(
        path: String,
        bytes: ByteArray,
        fileName: String,
        mimeType: String,
        fields: Map<String, String>
    ): HttpResponse {
        // Do not manually include `name="file"` in Content-Disposition.
        // Ktor's multipart builder writes the field name itself. Supplying the
        // name a second time can produce a malformed part that Multer/Express
        // treats as if no file was supplied, which surfaces as "Please select a
        // file to upload" on the server.
        val safeFileName = fileName
            .replace("\\", "_")
            .replace("\"", "_")
            .replace("\r", "_")
            .replace("\n", "_")
            .ifBlank { "media-file" }

        val safeMimeType = runCatching { ContentType.parse(mimeType) }
            .getOrElse { ContentType.Application.OctetStream }

        return client.post(url(path)) {
            withAuthenticatedHeaders()
            setBody(
                MultiPartFormDataContent(
                    formData {
                        fields.forEach { (name, value) -> append(name, value) }
                        append(
                            key = "file",
                            value = bytes,
                            headers = Headers.build {
                                append(HttpHeaders.ContentType, safeMimeType.toString())
                                append(HttpHeaders.ContentDisposition, "filename=\"$safeFileName\"")
                            }
                        )
                    }
                )
            )
        }.also(::clearOnUnauthorized)
    }

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
            authenticatedPost(AppConfig.ADMIN_LOGOUT_PATH)
        } catch (_: Exception) {
        } finally {
            clearSession()
        }
    }
}
