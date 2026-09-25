package com.example.helloworld.admin.content

import com.example.helloworld.data.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

@Serializable
data class CmsPage(
    val id: Long,
    val slug: String,
    val internal_name: String,
    val menu_label: String,
    val status: String = "draft",
    val show_in_navigation: Boolean = false
)

@Serializable
private data class CmsSectionContentRow(
    val content: JsonObject = buildJsonObject { }
)

@Serializable
data class CmsSection(
    val id: Long,
    val page_id: Long,
    val section_type: String,
    val position: Int = 0,
    val content: JsonObject = buildJsonObject { }
)

class WebsiteContentRepository {
    private val client = SupabaseProvider.client

    suspend fun loadPages(): Result<List<CmsPage>> = runCatching {
        client.from("cms_pages")
            .select(Columns.list("id", "slug", "internal_name", "menu_label", "status", "show_in_navigation"))
            .decodeList<CmsPage>()
    }

    suspend fun loadSections(pageId: Long): Result<List<CmsSection>> = runCatching {
        client.from("cms_sections")
            .select(Columns.list("id", "page_id", "section_type", "position", "content"))
            .decodeList<CmsSection>()
            .filter { it.page_id == pageId }
            .sortedBy { it.position }
    }

    suspend fun updateSection(sectionId: Long, heading: String, body: String, media: String, eyebrow: String): Result<Unit> = runCatching {
        val existing = client.from("cms_sections")
            .select(Columns.list("content")) {
                filter { eq("id", sectionId) }
            }
            .decodeSingle<CmsSectionContentRow>()
        val payload = buildJsonObject {
            existing.content.forEach { (key, value) -> put(key, value) }
            put("heading", heading)
            put("body", body)
            put("media", media)
            put("eyebrow", eyebrow)
        }
        client.from("cms_sections").update(mapOf("content" to payload)) {
            filter { eq("id", sectionId) }
        }
    }

    suspend fun updatePageDetails(pageId: Long, menuLabel: String, showInNavigation: Boolean): Result<Unit> = runCatching {
        client.from("cms_pages").update(
            mapOf("menu_label" to menuLabel.trim(), "show_in_navigation" to showInNavigation)
        ) {
            filter { eq("id", pageId) }
        }
    }

    suspend fun updatePageStatus(pageId: Long, status: String): Result<Unit> = runCatching {
        require(status in setOf("draft", "published", "archived"))
        client.from("cms_pages").update(mapOf("status" to status)) {
            filter { eq("id", pageId) }
        }
    }
}
