package com.example.helloworld.data

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.filter
import kotlinx.serialization.json.Json

/**
 * Public church data repository.
 *
 * Public/read operations talk directly to Supabase. The website server is no
 * longer required for the Home experience. Administrative mutations remain
 * behind the authenticated administration boundary.
 */
class ChurchRepository {
    suspend fun getSiteContent(): ChurchInfo {
        return try {
            val rows = SupabaseProvider.client
                .from("site_content")
                .select(Columns.list("key", "value"))
                .decodeList<SiteContentRow>()

            decodeChurchInfo(rows) ?: ChurchContent.default
        } catch (_: Exception) {
            ChurchContent.default
        }
    }

    suspend fun getMedia(): List<MediaItem> {
        return try {
            SupabaseProvider.client
                .from("media_items")
                .select {
                    filter {
                        eq("published", true)
                    }
                }
                .decodeList<MediaItem>()
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** Legacy admin entry points are intentionally unavailable from the public repository. */
    suspend fun login(username: String, password: String): LoginResponse =
        LoginResponse(ok = false, error = "Use the administrator authentication flow.")

    suspend fun logout() = Unit

    suspend fun updateSiteContent(content: ChurchInfo): Boolean = false

    suspend fun uploadMedia(
        title: String,
        description: String,
        category: String,
        type: String,
        fileBytes: ByteArray,
        fileName: String
    ): Boolean = false

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
    } catch (_: Exception) {
        fallback
    }
}

@kotlinx.serialization.Serializable
data class SiteContentRow(
    val key: String,
    val value: String
)

@kotlinx.serialization.Serializable
data class LoginRequest(val username: String, val password: String)

@kotlinx.serialization.Serializable
data class LoginResponse(
    val ok: Boolean = false,
    val user: UserInfo? = null,
    val error: String? = null
)

@kotlinx.serialization.Serializable
data class UserInfo(val id: Long, val username: String, val role: String)
