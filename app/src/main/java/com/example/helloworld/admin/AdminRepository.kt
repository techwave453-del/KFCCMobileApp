package com.example.helloworld.admin

import android.content.Context
import com.example.helloworld.data.ChurchContent
import com.example.helloworld.data.ChurchInfo
import com.example.helloworld.data.LiveStream
import com.example.helloworld.data.offline.KfccDatabase
import com.example.helloworld.data.offline.KfccOutboxRepository
import com.example.helloworld.data.offline.NotificationEntity
import com.example.helloworld.data.offline.SiteContentEntity
import com.example.helloworld.data.SiteContentRow
import com.example.helloworld.data.SupabaseProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.user.UserSession
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.*
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.statement.HttpResponse
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

@Serializable
private data class AdminSessionResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_in") val expiresIn: Int,
    @SerialName("token_type") val tokenType: String = "bearer",
    val user: AdminSessionUser
)

@Serializable
private data class AdminSessionUser(
    val id: String,
    val username: String,
    val email: String? = null,
    val role: String,
    val is_active: Boolean,
    val permissions: List<String> = emptyList()
)

class AdminRepository(context: Context) {
    private val appContext = context.applicationContext
    private val offlineDb = KfccDatabase.getInstance(appContext)
    private val outbox = KfccOutboxRepository(appContext, offlineDb)
    private val client = SupabaseProvider.client

    // The admin-login edge function returns the authoritative administrator
    // record separately from the Supabase Auth user. The imported session may
    // legitimately contain user = null, so keep the authoritative admin record
    // here until restoreSession() can reconstruct it from the authenticated app.
    @Volatile
    private var authenticatedAdmin: AdminUser? = null

    private val adminLoginClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    private val adminApiClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    private suspend fun accessToken(): String =
        client.auth.currentAccessTokenOrNull()
            ?: error("Your administrator session has expired. Please login again.")

    private fun apiUrl(path: String): String =
        "$ADMIN_API_BASE_URL/${path.trimStart('/')}"

    suspend fun authenticatedGet(path: String): HttpResponse =
        adminApiClient.get(apiUrl(path)) {
            bearerAuth(accessToken())
            accept(ContentType.Application.Json)
        }

    suspend fun authenticatedPost(path: String): HttpResponse =
        adminApiClient.post(apiUrl(path)) {
            bearerAuth(accessToken())
            accept(ContentType.Application.Json)
        }

    suspend fun authenticatedPost(path: String, body: Any): HttpResponse =
        adminApiClient.post(apiUrl(path)) {
            bearerAuth(accessToken())
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            setBody(body)
        }

    suspend fun authenticatedPut(path: String, body: Any): HttpResponse =
        adminApiClient.put(apiUrl(path)) {
            bearerAuth(accessToken())
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            setBody(body)
        }

    suspend fun authenticatedPatch(path: String, body: Any): HttpResponse =
        adminApiClient.patch(apiUrl(path)) {
            bearerAuth(accessToken())
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            setBody(body)
        }

    suspend fun authenticatedDelete(path: String): HttpResponse =
        adminApiClient.delete(apiUrl(path)) {
            bearerAuth(accessToken())
            accept(ContentType.Application.Json)
        }

    suspend fun authenticatedMultipartUpload(
        path: String,
        bytes: ByteArray,
        fileName: String,
        mimeType: String,
        fields: Map<String, String>
    ): HttpResponse =
        adminApiClient.post(apiUrl(path)) {
            bearerAuth(accessToken())
            setBody(
                MultiPartFormDataContent(
                    formData {
                        fields.forEach { (key, value) -> append(key, value) }
                        append(
                            key = "file",
                            value = bytes,
                            headers = Headers.build {
                                append(
                                    HttpHeaders.ContentDisposition,
                                    "form-data; name=\"file\"; filename=\"$fileName\""
                                )
                                append(HttpHeaders.ContentType, mimeType)
                            }
                        )
                    }
                )
            )
        }

    suspend fun restoreSession(): AdminUser? {
        // 1. Check if we have an authoritative admin from the current session's memory.
        authenticatedAdmin?.let { cached ->
            if (client.auth.currentAccessTokenOrNull() != null && cached.is_active) {
                return cached
            }
        }

        // 2. Check if there is an active session in the Supabase SDK.
        val session = client.auth.currentSessionOrNull() ?: return null
        val user = session.user
        
        // 3. Extract identity from metadata. Administrators have specific app_metadata.
        val metadata = user?.appMetadata ?: return null
        if (metadata["kfcc_admin"]?.toString()?.trim('"') != "true") return null

        val username = metadata["admin_username"]?.toString()?.trim('"').orEmpty()
        val role = metadata["admin_role"]?.toString()?.trim('"').orEmpty()
        val permissions = metadata["admin_permissions"]?.toString()
            ?.let { raw -> runCatching { Json.decodeFromString<List<String>>(raw) }.getOrNull() }
            .orEmpty()

        if (username.isBlank() || role.isBlank()) return null

        return AdminUser(
            id = metadata["admin_user_id"]?.toString()?.trim('"').orEmpty(),
            username = username,
            email = user.email,
            role = role,
            is_active = true,
            permissions = permissions
        ).also { authenticatedAdmin = it }
    }

    suspend fun login(username: String, password: String): AdminLoginResponse {
        val normalized = username.trim()

        if (normalized.isBlank() || password.isBlank()) {
            return AdminLoginResponse(
                ok = false,
                error = "Enter your administrator username and password."
            )
        }

        return try {
            val response = adminLoginClient.post("$SUPABASE_FUNCTIONS_URL/admin-login") {
                contentType(ContentType.Application.Json)
                setBody(AdminLoginRequest(normalized, password))
            }

            if (response.status.value !in 200..299) {
                val error = runCatching {
                    response.body<AdminErrorResponse>().error
                }.getOrNull()

                AdminLoginResponse(
                    ok = false,
                    error = error ?: "Invalid administrator username or password."
                )
            } else {
                val session = response.body<AdminSessionResponse>()
                val adminUser = AdminUser(
                    id = session.user.id,
                    username = session.user.username,
                    email = session.user.email,
                    role = session.user.role,
                    is_active = session.user.is_active,
                    permissions = session.user.permissions
                )

                client.auth.importSession(
                    UserSession(
                        accessToken = session.accessToken,
                        refreshToken = session.refreshToken,
                        expiresIn = session.expiresIn.toLong(),
                        tokenType = session.tokenType,
                        user = null
                    )
                )

                // Force fetching the user object so client.auth.currentUserOrNull() 
                // is correctly populated for other repositories (like Chat).
                runCatching { client.auth.retrieveUserForCurrentSession() }

                // Store the exact authoritative user returned by admin-login.
                // AdminViewModel.restoreSession() will read this same repository
                // instance when the AdminShell is entered after unified sign-in.
                authenticatedAdmin = adminUser

                AdminLoginResponse(
                    ok = true,
                    user = adminUser
                )
            }
        } catch (e: Exception) {
            AdminLoginResponse(
                ok = false,
                error = e.message ?: "Unable to sign in as administrator."
            )
        }
    }

    suspend fun logout() {
        authenticatedAdmin = null
        try {
            client.auth.signOut()
        } catch (_: Exception) {
            // Signing out locally is best-effort.
        }
    }

    suspend fun loadSiteContent(): Result<ChurchInfo> = runCatching {
        val rows = client.from("site_content")
            .select(Columns.list("key", "value"))
            .decodeList<SiteContentRow>()

        decodeChurchInfo(rows) ?: ChurchContent.default
    }

    suspend fun saveSiteContent(content: ChurchInfo): Result<ChurchInfo> = runCatching {
        val rows = listOf(
            SiteContentEntity("churchName", content.churchName),
            SiteContentEntity("tagline", content.tagline),
            SiteContentEntity("title", content.title),
            SiteContentEntity("subtitle", content.subtitle),
            SiteContentEntity("aboutTitle", content.aboutTitle),
            SiteContentEntity("aboutText", content.aboutText),
            SiteContentEntity("givingUrl", content.givingUrl),
            SiteContentEntity("services", Json.encodeToString(content.services)),
            SiteContentEntity("links", Json.encodeToString(content.links)),
            SiteContentEntity("membershipClasses", Json.encodeToString(content.membershipClasses))
        )
        offlineDb.siteContentDao().upsertAll(rows)
        rows.forEach { row ->
            outbox.enqueue(
                entityType = "site_content",
                operationType = "UPSERT",
                entityId = row.key,
                payload = Json.encodeToString(mapOf("key" to row.key, "value" to row.value))
            )
        }
        content
    }

    private fun decodeChurchInfo(rows: List<SiteContentRow>): ChurchInfo? {
        val values = rows.associate { it.key to it.value }
        if (values.isEmpty()) return null

        return ChurchInfo(
            churchName = values["churchName"].orEmpty(),
            tagline = values["tagline"].orEmpty(),
            title = values["title"].orEmpty(),
            subtitle = values["subtitle"].orEmpty(),
            aboutTitle = values["aboutTitle"].orEmpty(),
            aboutText = values["aboutText"].orEmpty(),
            phone = values["phone"].orEmpty(),
            email = values["email"].orEmpty(),
            givingUrl = values["givingUrl"].orEmpty(),
            services = decode(values["services"], emptyList()),
            links = decode(values["links"], emptyList()),
            membershipClasses = decode(values["membershipClasses"], emptyList()),
            liveStream = decode(values["liveStream"], LiveStream())
        )
    }

    private inline fun <reified T> decode(raw: String?, fallback: T): T =
        try {
            if (raw.isNullOrBlank()) fallback else Json.decodeFromString(raw)
        } catch (_: Exception) {
            fallback
        }

    suspend fun updateSiteContent(key: String, value: String): Result<Unit> = runCatching {
        offlineDb.siteContentDao().upsertAll(listOf(SiteContentEntity(key, value)))
        outbox.enqueue(
            entityType = "site_content",
            operationType = "UPSERT",
            entityId = key,
            payload = Json.encodeToString(mapOf("key" to key, "value" to value))
        )
    }

    suspend fun postAnnouncement(
        title: String,
        message: String,
        type: String
    ): Result<Unit> = runCatching {
        val id = UUID.randomUUID().toString()
        val createdAt = java.time.Instant.now().toString()
        offlineDb.notificationDao().upsertAll(
            listOf(NotificationEntity(id, null, title, message, type, createdAt))
        )
        outbox.enqueue(
            entityType = "app_notifications",
            operationType = "INSERT",
            entityId = id,
            payload = Json.encodeToString(
                mapOf("title" to title, "message" to message, "type" to type)
            )
        )
    }

    companion object {
        private const val SUPABASE_FUNCTIONS_URL =
            "https://uhzfjuquhqxhqtppispq.supabase.co/functions/v1"

        private const val ADMIN_API_BASE_URL =
            "https://kingdomfellowshipchristianchurch.onrender.com"
    }
}

@Serializable
private data class AdminErrorResponse(
    val error: String? = null
)
