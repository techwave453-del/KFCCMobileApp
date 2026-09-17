package com.example.helloworld.admin

import android.content.Context
import com.example.helloworld.data.ChurchContent
import com.example.helloworld.data.ChurchInfo
import com.example.helloworld.data.LiveStream
import com.example.helloworld.data.SiteContentRow
import com.example.helloworld.data.SupabaseProvider
import io.github.jan.supabase.auth.UserSession
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
private data class AdminLoginRequest(
    val username: String,
    val password: String
)

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
    val role: String,
    val is_active: Boolean,
    val permissions: List<String> = emptyList()
)

class AdminRepository(context: Context) {
    private val client = SupabaseProvider.client
    private val adminLoginClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    suspend fun restoreSession(): AdminUser? {
        val user = client.auth.currentUserOrNull() ?: return null
        val metadata = user.appMetadata
        val isAdmin = metadata["kfcc_admin"]?.toString()?.trim('"') == "true"
        if (!isAdmin) return null

        val username = metadata["admin_username"]?.toString()?.trim('"').orEmpty()
        val role = metadata["admin_role"]?.toString()?.trim('"').orEmpty()
        val permissions = metadata["admin_permissions"]?.toString()
            ?.let { raw -> runCatching { Json.decodeFromString<List<String>>(raw) }.getOrNull() }
            .orEmpty()

        if (username.isBlank() || role.isBlank()) return null
        return AdminUser(
            id = metadata["admin_user_id"]?.toString()?.trim('"').orEmpty(),
            username = username,
            role = role,
            is_active = true,
            permissions = permissions
        )
    }

    suspend fun login(username: String, password: String): AdminLoginResponse {
        val normalized = username.trim()
        if (normalized.isBlank() || password.isBlank()) {
            return AdminLoginResponse(ok = false, error = "Enter your administrator username and password.")
        }

        return try {
            val response = adminLoginClient.post("${SUPABASE_FUNCTIONS_URL}/admin-login") {
                contentType(ContentType.Application.Json)
                setBody(AdminLoginRequest(normalized, password))
            }

            if (response.status.value !in 200..299) {
                val error = runCatching { response.body<AdminErrorResponse>().error }.getOrNull()
                AdminLoginResponse(ok = false, error = error ?: "Invalid administrator username or password.")
            } else {
                val session = response.body<AdminSessionResponse>()
                client.auth.importSession(
                    UserSession(
                        accessToken = session.accessToken,
                        refreshToken = session.refreshToken,
                        expiresIn = session.expiresIn.toLong(),
                        tokenType = session.tokenType,
                        user = null
                    )
                )
                AdminLoginResponse(
                    ok = true,
                    user = AdminUser(
                        id = session.user.id,
                        username = session.user.username,
                        role = session.user.role,
                        is_active = session.user.is_active,
                        permissions = session.user.permissions
                    )
                )
            }
        } catch (e: Exception) {
            AdminLoginResponse(ok = false, error = e.message ?: "Unable to sign in as administrator.")
        }
    }

    private suspend fun fetchAdminProfile(userId: String): AdminUser? = restoreSession()

    suspend fun logout() {
        try { client.auth.signOut() } catch (_: Exception) {}
    }

    suspend fun loadSiteContent(): Result<ChurchInfo> = runCatching {
        val rows = client.from("site_content")
            .select(Columns.list("key", "value"))
            .decodeList<SiteContentRow>()
        decodeChurchInfo(rows) ?: ChurchContent.default
    }

    suspend fun saveSiteContent(content: ChurchInfo): Result<ChurchInfo> = runCatching {
        val rows = listOf(
            mapOf("key" to "churchName", "value" to content.churchName),
            mapOf("key" to "tagline", "value" to content.tagline),
            mapOf("key" to "title", "value" to content.title),
            mapOf("key" to "subtitle", "value" to content.subtitle),
            mapOf("key" to "aboutTitle", "value" to content.aboutTitle),
            mapOf("key" to "aboutText", "value" to content.aboutText),
            mapOf("key" to "phone", "value" to content.phone),
            mapOf("key" to "email", "value" to content.email),
            mapOf("key" to "services", "value" to Json.encodeToString(content.services)),
            mapOf("key" to "links", "value" to Json.encodeToString(content.links)),
            mapOf("key" to "membershipClasses", "value" to Json.encodeToString(content.membershipClasses)),
            mapOf("key" to "liveStream", "value" to Json.encodeToString(content.liveStream))
        )
        client.from("site_content").upsert(rows)
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
            services = decode(values["services"], emptyList()),
            links = decode(values["links"], emptyList()),
            membershipClasses = decode(values["membershipClasses"], emptyList()),
            liveStream = decode(values["liveStream"], LiveStream())
        )
    }

    private inline fun <reified T> decode(raw: String?, fallback: T): T = try {
        if (raw.isNullOrBlank()) fallback else Json.decodeFromString(raw)
    } catch (_: Exception) { fallback }

    suspend fun updateSiteContent(key: String, value: String): Result<Unit> = runCatching {
        client.from("site_content").upsert(mapOf("key" to key, "value" to value))
    }

    suspend fun postAnnouncement(title: String, message: String, type: String): Result<Unit> = runCatching {
        client.from("app_notifications").insert(mapOf("title" to title, "message" to message, "type" to type))
    }

    companion object {
        private const val SUPABASE_FUNCTIONS_URL = "https://uhzfjuquhqxhqtppispq.supabase.co/functions/v1"
    }
}

@Serializable
private data class AdminErrorResponse(val error: String? = null)
