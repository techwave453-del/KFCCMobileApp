package com.example.helloworld.admin

import android.content.Context
import com.example.helloworld.data.ChurchContent
import com.example.helloworld.data.ChurchInfo
import com.example.helloworld.data.LiveStream
import com.example.helloworld.data.SiteContentRow
import com.example.helloworld.data.SupabaseProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.user.UserSession
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.storage.storage
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
    val email: String = "",
    val role: String,
    val is_active: Boolean,
    val permissions: List<String> = emptyList()
)

class AdminRepository(context: Context) {
    private val client = SupabaseProvider.client

    @Volatile
    private var authenticatedAdmin: AdminUser? = null

    private val adminLoginClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    private suspend fun accessToken(): String =
        client.auth.currentAccessTokenOrNull()
            ?: error("Your administrator session has expired. Please login again.")

    /**
     * Restores the administrator from Supabase Auth + admin_profiles/admin_user_permissions.
     * The website/Render server is not consulted for authorization or module access.
     */
    suspend fun restoreSession(): AdminUser? {        val session = client.auth.currentSessionOrNull() ?: return null
        val user = runCatching {
            client.auth.currentUserOrNull() ?: client.auth.retrieveUserForCurrentSession()
        }.getOrNull() ?: session.user ?: return null

        val profile = client.from("admin_profiles")
            .select()
            .decodeList<AdminProfileRow>()
            .firstOrNull { it.auth_user_id == user.id && it.is_active }

        profile ?: return null

        val permissions = client.from("admin_user_permissions")
            .select(Columns.list("permission"))
            .decodeList<AdminPermissionRow>()
            .map { it.permission }

        return AdminUser(
            id = profile.auth_user_id,
            username = profile.display_name?.takeIf { it.isNotBlank() } ?: user.email.orEmpty(),
            email = user.email ?: profile.login_email.orEmpty(),
            role = profile.role,
            is_active = profile.is_active,
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
            // Authentication remains in the dedicated Supabase Edge Function because it
            // performs the legacy username/password verification and creates the Auth session.
            // All post-login admin data access is direct Supabase Data API access.
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

                runCatching { client.auth.retrieveUserForCurrentSession() }
                authenticatedAdmin = adminUser

                // Re-read the authoritative admin profile/permissions from Supabase.
                val restored = restoreSession()
                AdminLoginResponse(
                    ok = restored != null,
                    user = restored,
                    error = if (restored == null) {
                        "Your administrator session could not be verified."
                    } else null
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
        }
    }

    suspend fun loadSiteContent(): Result<ChurchInfo> = runCatching {
        val rows = client.from("site_content")
            .select(Columns.list("key", "value"))
            .decodeList<SiteContentRow>()

        decodeChurchInfo(rows) ?: ChurchContent.default
    }

    /**
     * Saves only normal Website Content fields. Protected contact and live-stream
     * fields are saved through their own permission-gated operations below.
     */
    suspend fun saveSiteContent(
        content: ChurchInfo,
        canEditIdentity: Boolean = false,
        canManageLive: Boolean = false
    ): Result<ChurchInfo> = runCatching {
        val rows = mutableListOf(
            mapOf("key" to "tagline", "value" to content.tagline),
            mapOf("key" to "title", "value" to content.title),
            mapOf("key" to "subtitle", "value" to content.subtitle),
            mapOf("key" to "aboutTitle", "value" to content.aboutTitle),
            mapOf("key" to "aboutText", "value" to content.aboutText),
            mapOf("key" to "givingUrl", "value" to content.givingUrl),
            mapOf("key" to "services", "value" to Json.encodeToString(content.services)),
            mapOf("key" to "links", "value" to Json.encodeToString(content.links)),
            mapOf("key" to "membershipClasses", "value" to Json.encodeToString(content.membershipClasses))
        )

        if (canEditIdentity) {
            rows += mapOf("key" to "churchName", "value" to content.churchName)
            rows += mapOf("key" to "phone", "value" to content.phone)
            rows += mapOf("key" to "email", "value" to content.email)
        }

        if (canManageLive) {
            rows += mapOf("key" to "liveStream", "value" to Json.encodeToString(content.liveStream))
        }

        client.from("site_content").upsert(rows)
        content
    }

    suspend fun saveLiveStream(liveStream: LiveStream): Result<Unit> = runCatching {
        client.from("site_content").upsert(
            mapOf("key" to "liveStream", "value" to Json.encodeToString(liveStream))
        )
    }

    suspend fun updateSiteContent(key: String, value: String): Result<Unit> = runCatching {
        client.from("site_content").upsert(mapOf("key" to key, "value" to value))
    }

    suspend fun postAnnouncement(
        title: String,
        message: String,
        type: String
    ): Result<Unit> = runCatching {
        client.from("app_notifications").insert(
            mapOf("title" to title, "message" to message, "type" to type)
        )
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

    companion object {
        private const val SUPABASE_FUNCTIONS_URL =
            "https://uhzfjuquhqxhqtppispq.supabase.co/functions/v1"
    }
}

@Serializable
private data class AdminProfileRow(
    val auth_user_id: String,
    val display_name: String? = null,
    val role: String,
    val is_active: Boolean,
    val login_email: String? = null
)

@Serializable
private data class AdminPermissionRow(
    val permission: String
)

@Serializable
private data class AdminErrorResponse(
    val error: String? = null
)
