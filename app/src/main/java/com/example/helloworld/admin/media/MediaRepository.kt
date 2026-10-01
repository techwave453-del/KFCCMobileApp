package com.example.helloworld.admin.media

import android.content.Context
import com.example.helloworld.data.SupabaseProvider
import com.example.helloworld.data.offline.KfccDatabase
import com.example.helloworld.data.offline.MediaItemEntity
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import java.util.UUID
import kotlinx.serialization.Serializable

@Serializable
private data class MediaUrlInsertPayload(
    val title: String,
    val description: String,
    val category: String,
    val type: String,
    val url: String,
    val published: Boolean = true
)

@Serializable
private data class MediaUploadInsertPayload(
    val title: String,
    val description: String,
    val category: String,
    val type: String,
    val url: String,
    val storage_path: String,
    val published: Boolean = true
)

/** Direct Supabase Media Center repository with local Room cache + outbox mutations. */
class MediaRepository(context: Context) {
    private val appContext = context.applicationContext
    private val client = SupabaseProvider.client
    private val db = KfccDatabase.getInstance(appContext)

    suspend fun load(): Result<List<AdminMediaItem>> {
        return runCatching {
            client.from("media_items")
                .select()
                .decodeList<AdminMediaItem>()
                .sortedByDescending { it.created_at }
                .also { db.mediaItemDao().upsertAll(it.map(::toEntity)) }
        }.recoverCatching {
            val local = db.mediaItemDao().getAll()
            if (local.isEmpty()) throw it
            local.map(::toAdminItem)
        }
    }

    suspend fun update(
        id: Long,
        title: String,
        description: String,
        category: String,
        published: Boolean? = null,
        featured: Boolean? = null
    ): Result<AdminMediaItem> = runCatching {
        if (featured == true) {
            clearOtherFeaturedVideos(id)
        }

        client.from("media_items").update({
            set("title", title.trim())
            set("description", description.trim())
            set("category", category.trim())
            published?.let { set("published", it) }
            featured?.let { set("featured", it) }
        }) {
            select()
            filter { eq("id", id) }
        }.decodeSingle<AdminMediaItem>()
            .also { db.mediaItemDao().upsertAll(listOf(toEntity(it))) }
    }

    suspend fun setFeatured(id: Long, featured: Boolean): Result<Unit> = runCatching {
        if (featured) {
            clearOtherFeaturedVideos(id)
        }

        client.from("media_items").update({
            set("featured", featured)
        }) {
            filter { eq("id", id) }
        }

        val refreshed = client.from("media_items")
            .select()
            .decodeList<AdminMediaItem>()
            .firstOrNull { it.id == id }
            ?: error("Media item $id was not found after updating featured status.")

        db.mediaItemDao().upsertAll(listOf(toEntity(refreshed)))
    }

    suspend fun setHero(id: Long, hero: Boolean): Result<Unit> = runCatching {
        client.from("media_items").update({
            set("hero", hero)
        }) {
            filter { eq("id", id) }
        }

        val refreshed = client.from("media_items")
            .select()
            .decodeList<AdminMediaItem>()
            .firstOrNull { it.id == id }
            ?: error("Media item $id was not found after updating hero status.")

        db.mediaItemDao().upsertAll(listOf(toEntity(refreshed)))
    }

    private suspend fun clearOtherFeaturedVideos(exceptId: Long) {
        val featuredVideos = client.from("media_items")
            .select()
            .decodeList<AdminMediaItem>()
            .filter {
                it.id != exceptId &&
                    it.featured &&
                    it.type.equals("video", ignoreCase = true)
            }

        featuredVideos.forEach { item ->
            client.from("media_items").update({
                set("featured", false)
            }) {
                filter { eq("id", item.id) }
            }

            db.mediaItemDao().getAll()
                .firstOrNull { it.id == item.id }
                ?.let { current ->
                    db.mediaItemDao().upsertAll(listOf(current.copy(featured = false)))
                }
        }
    }

    suspend fun delete(id: Long): Result<Unit> = runCatching {
        val current = client.from("media_items")
            .select()
            .decodeList<AdminMediaItem>()
            .firstOrNull { it.id == id }
            ?: error("Media item $id was not found.")

        client.from("media_items").delete {
            filter { eq("id", id) }
        }

        current.storage_path?.takeIf { it.isNotBlank() }?.let { path ->
            client.storage.from("church-media").delete(path)
        }

        db.mediaItemDao().deleteById(id)
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
        val path = "${UUID.randomUUID()}_$fileName"
        val bucket = client.storage.from("church-media")
        bucket.upload(path, bytes) {
            upsert = true
            contentType = io.ktor.http.ContentType.parse(mimeType)
        }
        val publicUrl = bucket.publicUrl(path)
        val item = MediaUploadInsertPayload(
            title = title.trim().ifBlank { "Uploaded media" },
            description = description.trim(),
            category = category.trim().ifBlank { "videos" },
            type = type.trim().ifBlank { "video" },
            url = publicUrl,
            storage_path = path
        )
        client.from("media_items").insert(item) {
            select()
        }.decodeSingle<AdminMediaItem>()
            .also { db.mediaItemDao().upsertAll(listOf(toEntity(it))) }
    }

    suspend fun addUrl(
        title: String,
        url: String,
        description: String,
        category: String,
        type: String
    ): Result<AdminMediaItem> = runCatching {
        val item = MediaUrlInsertPayload(
            title = title.trim().ifBlank { "External media" },
            description = description.trim(),
            category = category.trim().ifBlank { "videos" },
            type = type.trim().ifBlank { "video" },
            url = url.trim()
        )
        client.from("media_items")
            .insert(item) {
                select()
            }
            .decodeSingle<AdminMediaItem>()
            .also { db.mediaItemDao().upsertAll(listOf(toEntity(it))) }
    }

    private fun toEntity(item: AdminMediaItem) = MediaItemEntity(
        id = item.id, legacyId = item.legacy_id, title = item.title, type = item.type,
        category = item.category, description = item.description, url = item.url,
        storagePath = item.storage_path, createdAt = item.created_at, published = item.published,
        thumbnailUrl = item.thumbnail_url, featured = item.featured, hero = item.hero
    )

    private fun toAdminItem(item: MediaItemEntity) = AdminMediaItem(
        id = item.id, title = item.title, type = item.type, category = item.category,
        description = item.description, url = item.url, storage_path = item.storagePath,
        featured = item.featured, hero = item.hero, published = item.published, thumbnail_url = item.thumbnailUrl,
        created_at = item.createdAt
    )
}
