package com.example.helloworld.data.offline

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.helloworld.data.EventItem
import com.example.helloworld.data.MediaItem
import com.example.helloworld.data.SiteContentRow
import com.example.helloworld.data.AppNotification
import com.example.helloworld.data.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns

class KfccContentSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    private val db = KfccDatabase.getInstance(appContext)

    override suspend fun doWork(): Result = runCatching {
        syncSiteContent()
        syncMedia()
        syncEvents()
        syncNotifications()
        Result.success()
    }.getOrElse {
        Result.retry()
    }

    private suspend fun syncSiteContent() {
        val rows = SupabaseProvider.client
            .from("site_content")
            .select(Columns.list("key", "value"))
            .decodeList<SiteContentRow>()
        db.siteContentDao().upsertAll(rows.map { SiteContentEntity(it.key, it.value) })
    }

    private suspend fun syncMedia() {
        val rows = SupabaseProvider.client
            .from("media_items")
            .select()
            .decodeList<MediaItem>()
        db.mediaItemDao().clear()
        db.mediaItemDao().upsertAll(rows.map {
            MediaItemEntity(
                id = it.id,
                legacyId = null,
                title = it.title,
                type = it.type,
                category = it.category,
                description = it.description,
                url = it.url,
                storagePath = null,
                createdAt = it.createdAt,
                published = true,
                thumbnailUrl = null,
                featured = it.featured
            )
        })
    }

    private suspend fun syncEvents() {
        val rows = SupabaseProvider.client
            .from("events")
            .select()
            .decodeList<EventItem>()
        db.eventDao().clear()
        db.eventDao().upsertAll(rows.map {
            EventEntity(
                id = it.id,
                slug = it.slug,
                title = it.title,
                category = it.category,
                shortDescription = it.shortDescription,
                description = it.description,
                image = it.image,
                flyerUrl = it.flyerUrl,
                startAt = it.startAt,
                endAt = it.endAt,
                allDay = it.allDay,
                location = it.location,
                address = it.address,
                attendanceType = it.attendanceType,
                registrationUrl = it.registrationUrl,
                contact = it.contact,
                livestreamUrl = it.livestreamUrl,
                featured = it.featured,
                status = it.status,
                displayOrder = it.displayOrder,
                createdAt = it.createdAt,
                updatedAt = it.updatedAt
            )
        })
    }

    private suspend fun syncNotifications() {
        val rows = SupabaseProvider.client
            .from("app_notifications")
            .select()
            .decodeList<AppNotification>()
        db.notificationDao().upsertAll(rows.map {
            NotificationEntity(it.id, it.userId, it.title, it.message, it.type, it.createdAt)
        })
    }
}
