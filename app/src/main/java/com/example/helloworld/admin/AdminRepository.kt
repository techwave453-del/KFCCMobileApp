package com.example.helloworld.admin

import android.content.Context
import com.example.helloworld.data.ChurchContent
import com.example.helloworld.data.ChurchInfo
import com.example.helloworld.data.LiveStream
import com.example.helloworld.data.SiteContentRow
import com.example.helloworld.data.SupabaseProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.ktor.client.statement.HttpResponse
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class AdminRepository(context: Context) {
    private val client = SupabaseProvider.client

    suspend fun restoreSession(): AdminUser? {
        val user = client.auth.currentUserOrNull() ?: return null
        val profile = fetchAdminProfile(user.id)
        if (user.email?.lowercase() == "denniesofts@gmail.com") {
            return AdminUser(
                id = user.id,
                username = profile?.username?.takeIf { it.isNotBlank() } ?: "denniesofts",
                role = "super_admin",
                is_active = true,
                permissions = emptyList()
            )
        }
        return profile
    }

    suspend fun login(username: String, password: String): AdminLoginResponse {
        return try {
            // Attempt Supabase sign in. 
            // In a truly independent setup, administrators sign in via Supabase Auth.
            client.auth.signInWith(Email) {
                this.email = if (username.contains("@")) username else "$username@kfcc.internal"
                this.password = password
            }
            
            val user = client.auth.currentUserOrNull()
            if (user != null) {
                val profile = fetchAdminProfile(user.id)
                if (user.email?.lowercase() == "denniesofts@gmail.com") {
                    val adminUser = AdminUser(
                        id = user.id,
                        username = profile?.username?.takeIf { it.isNotBlank() } ?: "denniesofts",
                        role = "super_admin",
                        is_active = true,
                        permissions = emptyList()
                    )
                    return AdminLoginResponse(ok = true, user = adminUser)
                }
                if (profile != null) {
                    AdminLoginResponse(ok = true, user = profile)
                } else {
                    AdminLoginResponse(ok = false, error = "You do not have administrator permissions.")
                }
            } else {
                AdminLoginResponse(ok = false, error = "Login failed.")
            }
        } catch (e: Exception) {
            AdminLoginResponse(ok = false, error = e.message ?: "Login failed.")
        }
    }

    private suspend fun fetchAdminProfile(userId: String): AdminUser? {
        return try {
            client.from("admin_profiles")
                .select { filter { eq("id", userId) } }
                .decodeSingle<AdminUser>()
        } catch (e: Exception) {
            null
        }
    }

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

    // Helper for independent mutations via Supabase
    suspend fun updateSiteContent(key: String, value: String): Result<Unit> = runCatching {
        client.from("site_content").upsert(mapOf("key" to key, "value" to value))
    }
    
    suspend fun postAnnouncement(title: String, message: String, type: String): Result<Unit> = runCatching {
        client.from("app_notifications").insert(mapOf(
            "title" to title,
            "message" to message,
            "type" to type
        ))
    }
    
    // Compatibility helpers for existing code
    suspend fun authenticatedGet(path: String): HttpResponse {
        // This is now a stub or should be removed. 
        // Real logic should move to fetchAdminProfile or similar Supabase calls.
        error("Independent mode enabled. Use Supabase directly.")
    }
    
    suspend fun authenticatedPost(path: String, body: Any? = null): HttpResponse {
        error("Independent mode enabled. Use Supabase directly.")
    }

    suspend fun authenticatedPut(path: String, body: Any? = null): HttpResponse {
        error("Independent mode enabled. Use Supabase directly.")
    }

    suspend fun authenticatedPatch(path: String, body: Any? = null): HttpResponse {
        error("Independent mode enabled. Use Supabase directly.")
    }

    suspend fun authenticatedDelete(path: String): HttpResponse {
        error("Independent mode enabled. Use Supabase directly.")
    }

    suspend fun authenticatedMultipartUpload(
        path: String,
        bytes: ByteArray,
        fileName: String,
        mimeType: String,
        fields: Map<String, String>
    ): HttpResponse {
        error("Independent mode enabled. Use Supabase directly.")
    }
}
