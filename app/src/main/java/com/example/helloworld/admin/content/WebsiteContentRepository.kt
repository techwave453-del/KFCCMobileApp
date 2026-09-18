package com.example.helloworld.admin.content

import com.example.helloworld.data.ChurchInfo
import com.example.helloworld.data.ChurchRepository
import com.example.helloworld.data.ChurchService
import com.example.helloworld.data.ChurchLink
import com.example.helloworld.data.MembershipClass
import com.example.helloworld.data.LiveStream
import com.example.helloworld.data.SiteContentRow
import com.example.helloworld.data.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.json.Json

class WebsiteContentRepository {
    private val client get() = SupabaseProvider.client
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun load(): Result<ChurchInfo> = runCatching {
        val rows = client
            .from("site_content")
            .select()
            .decodeList<SiteContentRow>()
        decode(rows) ?: error("Website Content is empty.")
    }

    suspend fun save(content: ChurchInfo): Result<ChurchInfo> = runCatching {
        val rows = listOf(
            SiteContentRow("tagline", content.tagline),
            SiteContentRow("title", content.title),
            SiteContentRow("subtitle", content.subtitle),
            SiteContentRow("aboutTitle", content.aboutTitle),
            SiteContentRow("aboutText", content.aboutText),
            SiteContentRow("phone", content.phone),
            SiteContentRow("email", content.email),
            SiteContentRow("services", json.encodeToString(content.services)),
            SiteContentRow("links", json.encodeToString(content.links)),
            SiteContentRow("membershipClasses", json.encodeToString(content.membershipClasses)),
            SiteContentRow("liveStream", json.encodeToString(content.liveStream))
        )
        rows.forEach { row ->
            client.from("site_content").upsert(row)
        }
        content
    }

    private fun decode(rows: List<SiteContentRow>): ChurchInfo? {
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
            services = decodeJson(values["services"], emptyList()),
            links = decodeJson(values["links"], emptyList()),
            membershipClasses = decodeJson(values["membershipClasses"], emptyList()),
            liveStream = decodeJson(values["liveStream"], LiveStream())
        )
    }

    private inline fun <reified T> decodeJson(raw: String?, fallback: T): T =
        runCatching { if (raw.isNullOrBlank()) fallback else json.decodeFromString<T>(raw) }.getOrDefault(fallback)
}
