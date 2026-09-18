package com.example.helloworld.admin.media

import com.example.helloworld.data.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
private data class MediaRow(
    val id: Long,
    val title: String,
    val type: String,
    val category: String,
    val description: String? = null,
    val url: String,
    val storage_path: String? = null,
    val featured: Boolean = false,
    val published: Boolean = true,
    val created_at: String = ""
)

@Serializable
private data class MediaInsert(
    val id: Long,
    val title: String,
    val type: String,
    val category: String,
    val description: String,
    val url: String,
    val storage_path: String? = null,
    val published: Boolean = true,
    val featured: Boolean = false
)

class MediaRepository(@Suppress("UNUSED_PARAMETER") context: android.content.Context) {
    private val client get() = SupabaseProvider.client
    private val bucket get() = client.storage.from("church-media")

    suspend fun load(): Result<List<AdminMediaItem>> = runCatching {
        client.from("media_items").select()
            .decodeList<MediaRow>()
            .map(::toModel)
            .sortedByDescending { it.created_at }
    }

    suspend fun addUrl(title: String, type: String, category: String, url: String, description: String): Result<AdminMediaItem> = runCatching {
        val id = nextId()
        val row = MediaInsert(id, title.trim(), type, category, description.trim(), url.trim())
        client.from("media_items").insert(row) { select() }
            .decodeList<MediaRow>().first().let(::toModel)
    }

    suspend fun update(id: Long, title: String, description: String, category: String, published: Boolean? = null, featured: Boolean? = null): Result<AdminMediaItem> = runCatching {
        client.from("media_items").update({
            set("title", title.trim())
            set("description", description.trim())
            set("category", category)
            if (published != null) set("published", published)
            if (featured != null) set("featured", featured)
        }) {
            filter { eq("id", id) }
            select()
        }.decodeList<MediaRow>().first().let(::toModel)
    }

    suspend fun setFeatured(id: Long, featured: Boolean): Result<Unit> = runCatching {
        client.from("media_items").update({
            set("featured", featured)
        }) {
            filter { eq("id", id) }
        }
    }

    suspend fun delete(id: Long): Result<Unit> = runCatching {
        val row = client.from("media_items").select {
            filter { eq("id", id) }
        }.decodeList<MediaRow>().firstOrNull()
        if (row?.storage_path?.isNotBlank() == true) {
            bucket.delete(row.storage_path)
        }
        client.from("media_items").delete {
            filter { eq("id", id) }
        }
    }

    suspend fun upload(
        bytes: ByteArray,
        fileName: String,
        mimeType: String,
        title: String,
        description: String,
        category: String,
        type: String
    ): Result<AdminMediaItem> = runCatching {
        val safeName = fileName
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .ifBlank { "media-file" }
        val path = "admin/${UUID.randomUUID()}-$safeName"
        bucket.upload(path, bytes) { upsert = false }
        val url = bucket.publicUrl(path)
        val id = nextId()
        client.from("media_items").insert(
            MediaInsert(id, title.trim(), type, category, description.trim(), url, path)
        ) { select() }.decodeList<MediaRow>().first().let(::toModel)
    }

    private suspend fun nextId(): Long =
        client.from("media_items").select().decodeList<MediaRow>().maxOfOrNull { it.id }?.plus(1) ?: 1L

    private fun toModel(row: MediaRow) = AdminMediaItem(
        id = row.id,
        title = row.title,
        type = row.type,
        category = row.category,
        description = row.description.orEmpty(),
        url = row.url,
        featured = row.featured,
        published = row.published,
        created_at = row.created_at
    )
}
