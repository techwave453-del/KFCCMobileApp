package com.example.helloworld.data.offline

import com.example.helloworld.data.AppNotification
import com.example.helloworld.data.ChurchContent
import com.example.helloworld.data.ChurchInfo
import com.example.helloworld.data.EventItem
import com.example.helloworld.data.MediaItem
import com.example.helloworld.data.SiteContentRow
import com.example.helloworld.data.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

class KfccContentRepository(private val db: KfccDatabase) {
    private val json = Json { ignoreUnknownKeys = true }

    fun observeChurchInfo(): Flow<ChurchInfo> =
        db.siteContentDao().observeAll().map { decodeChurchInfo(it) ?: ChurchContent.default }

    fun observeMedia(): Flow<List<MediaItem>> =
        db.mediaItemDao().observePublished().map { it.map(::toMedia) }

    fun observeEvents(): Flow<List<EventItem>> =
        db.eventDao().observePublished().map { it.map(::toEvent) }

    fun observeNotifications(userId: String?): Flow<List<AppNotification>> =
        db.notificationDao().observeForUser(userId).map { it.map(::toNotification) }

    suspend fun getChurchInfo(): ChurchInfo {
        val local = decodeChurchInfo(db.siteContentDao().getAll())
        if (local != null) return local
        return runCatching {
            val rows = SupabaseProvider.client.from("site_content")
                .select(Columns.list("key", "value")).decodeList<SiteContentRow>()
            db.siteContentDao().upsertAll(rows.map { SiteContentEntity(it.key, it.value) })
            decodeChurchInfo(rows.map { SiteContentEntity(it.key, it.value) }) ?: ChurchContent.default
        }.getOrElse { ChurchContent.default }
    }

    suspend fun getMedia(): List<MediaItem> {
        val local = db.mediaItemDao().getPublished()
        if (local.isNotEmpty()) return local.map(::toMedia)
        return runCatching {
            val rows = SupabaseProvider.client.from("media_items")
                .select { filter { eq("published", true) } }.decodeList<com.example.helloworld.admin.media.AdminMediaItem>()
            val entities = rows.map(::toMediaEntity)
            db.mediaItemDao().upsertAll(entities)
            entities.filter { it.published }.map(::toMedia)
        }.getOrElse { emptyList() }
    }

    suspend fun getEvents(): List<EventItem> {
        val local = db.eventDao().getPublished()
        if (local.isNotEmpty()) return local.map(::toEvent)
        return runCatching {
            val rows = SupabaseProvider.client.from("events")
                .select { filter { eq("status", "published") } }.decodeList<EventItem>()
            db.eventDao().upsertAll(rows.map(::toEventEntity))
            rows
        }.getOrElse { emptyList() }
    }

    suspend fun getNotifications(userId: String?): List<AppNotification> {
        val local = db.notificationDao().getForUser(userId)
        if (local.isNotEmpty()) return local.map(::toNotification)
        return runCatching {
            val rows = SupabaseProvider.client.from("app_notifications")
                .select().decodeList<AppNotification>()
                .filter { it.userId == null || it.userId == userId }
                .sortedByDescending { it.createdAt }
            db.notificationDao().upsertAll(rows.map(::toNotificationEntity))
            rows
        }.getOrElse { emptyList() }
    }

    private fun decodeChurchInfo(items: List<SiteContentEntity>): ChurchInfo? {
        val values = items.associate { it.key to it.value }
        if (values.isEmpty()) return null
        return ChurchInfo(
            churchName = values["churchName"].orEmpty(),
            logoUrl = values["logoUrl"] ?: values["logo"] ?: values["churchLogo"].orEmpty(),
            tagline = values["tagline"].orEmpty(), title = values["title"].orEmpty(),
            subtitle = values["subtitle"].orEmpty(), aboutTitle = values["aboutTitle"].orEmpty(),
            aboutText = values["aboutText"].orEmpty(), phone = values["phone"].orEmpty(),
            email = values["email"].orEmpty(), givingUrl = values["givingUrl"].orEmpty(),
            services = decode(values["services"], emptyList()), links = decode(values["links"], emptyList()),
            membershipClasses = decode(values["membershipClasses"], emptyList()),
            liveStream = decode(values["liveStream"], com.example.helloworld.data.LiveStream())
        )
    }

    private inline fun <reified T> decode(raw: String?, fallback: T): T =
        runCatching { if (raw.isNullOrBlank()) fallback else json.decodeFromString<T>(raw) }.getOrDefault(fallback)

    private fun toMediaEntity(x: com.example.helloworld.admin.media.AdminMediaItem) = MediaItemEntity(
        id = x.id, legacyId = x.legacy_id, title = x.title, type = x.type, category = x.category,
        description = x.description, url = x.url, storagePath = x.storage_path, createdAt = x.created_at,
        published = x.published, thumbnailUrl = x.thumbnail_url, featured = x.featured
    )

    private fun toMedia(x: MediaItemEntity) =
        MediaItem(x.id, x.title, x.type, x.category, x.description, x.url, x.featured, x.createdAt)

    private fun toEventEntity(x: EventItem) = EventEntity(
        x.id, x.slug, x.title, x.category, x.shortDescription, x.description, x.image, x.flyerUrl,
        x.startAt, x.endAt, x.allDay, x.location, x.address, x.attendanceType, x.registrationUrl,
        x.contact, x.livestreamUrl, x.featured, x.status, x.displayOrder, x.createdAt, x.updatedAt
    )

    private fun toEvent(x: EventEntity) = EventItem(
        x.id, x.slug, x.title, x.category, x.shortDescription, x.description, x.image, x.flyerUrl,
        x.startAt, x.endAt, x.allDay, x.location, x.address, x.attendanceType, x.registrationUrl,
        x.contact, x.livestreamUrl, x.featured, x.status, x.displayOrder, x.createdAt, x.updatedAt
    )

    private fun toNotificationEntity(x: AppNotification) =
        NotificationEntity(x.id, x.userId, x.title, x.message, x.type, x.createdAt)

    private fun toNotification(x: NotificationEntity) =
        AppNotification(
            id = x.id,
            title = x.title,
            message = x.message,
            type = x.type,
            createdAt = x.createdAt,
            userId = x.userId
        )
}
