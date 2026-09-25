package com.example.helloworld.events

import com.example.helloworld.data.KfccDataContext
import com.example.helloworld.data.SupabaseProvider
import com.example.helloworld.data.offline.KfccContentRepository
import com.example.helloworld.data.offline.KfccDatabase
import com.example.helloworld.data.offline.KfccOutboxRepository
import com.example.helloworld.data.offline.EventEntity
import kotlinx.serialization.json.Json
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Events data access.
 *
 * Public reads are offline-first through Room. Admin mutations continue to
 * use the authenticated Supabase boundary.
 */
class EventsRepository(private val adminRepository: AdminRepository? = null) {

    private val context = KfccDataContext.appContext
    private val db = KfccDatabase.getInstance(context)
    private val outbox = KfccOutboxRepository(context, db)
    private val json = Json { ignoreUnknownKeys = true }

    private val offline by lazy {
        KfccContentRepository(KfccDatabase.getInstance(KfccDataContext.appContext))
    }

    fun observePublicEvents(): Flow<List<Event>> =
        offline.observeEvents().map { items -> items.map(::toEvent) }

    suspend fun getPublicEvents(): Result<List<Event>> = runCatching {
        offline.getEvents().map(::toEvent)
    }

    suspend fun getAdminEvents(): Result<List<Event>> = runCatching {
        SupabaseProvider.client
            .from("events")
            .select()
            .decodeList<Event>()
            .sortedWith(compareBy<Event> { it.display_order }.thenBy { it.start_at })
    }

    suspend fun create(input: EventInput): Result<Event> = runCatching {
        val localId = -System.currentTimeMillis()
        val optimistic = Event(
            id = localId,
            title = input.title,
            category = input.category,
            short_description = input.short_description,
            description = input.description,
            image = input.image,
            flyer_url = input.flyer_url,
            start_at = input.start_at,
            end_at = input.end_at,
            all_day = input.all_day,
            location = input.location,
            address = input.address,
            attendance_type = input.attendance_type,
            registration_url = input.registration_url,
            contact = input.contact,
            livestream_url = input.livestream_url,
            featured = input.featured,
            status = input.status,
            display_order = input.display_order
        )
        db.eventDao().upsertAll(listOf(toEventEntity(optimistic)))
        outbox.enqueue("events", "INSERT", null, json.encodeToString(input))
        optimistic
    }

    suspend fun update(id: Long, input: EventInput): Result<Event> = runCatching {
        val optimistic = Event(
            id = id,
            title = input.title,
            category = input.category,
            short_description = input.short_description,
            description = input.description,
            image = input.image,
            flyer_url = input.flyer_url,
            start_at = input.start_at,
            end_at = input.end_at,
            all_day = input.all_day,
            location = input.location,
            address = input.address,
            attendance_type = input.attendance_type,
            registration_url = input.registration_url,
            contact = input.contact,
            livestream_url = input.livestream_url,
            featured = input.featured,
            status = input.status,
            display_order = input.display_order
        )
        db.eventDao().upsertAll(listOf(toEventEntity(optimistic)))
        outbox.enqueue("events", "UPDATE", id.toString(), json.encodeToString(input))
        optimistic
    }

    suspend fun delete(id: Long): Result<Unit> = runCatching {
        db.eventDao().deleteById(id)
        outbox.enqueue("events", "DELETE", id.toString(), json.encodeToString(EventInput(
            title = "",
            start_at = ""
        )))
    }

    private fun toEventEntity(x: Event) = EventEntity(
        id = x.id,
        slug = x.slug,
        title = x.title,
        category = x.category,
        shortDescription = x.short_description,
        description = x.description,
        image = x.image,
        flyerUrl = x.flyer_url,
        startAt = x.start_at,
        endAt = x.end_at,
        allDay = x.all_day,
        location = x.location,
        address = x.address,
        attendanceType = x.attendance_type,
        registrationUrl = x.registration_url,
        contact = x.contact,
        livestreamUrl = x.livestream_url,
        featured = x.featured,
        status = x.status,
        displayOrder = x.display_order,
        createdAt = "",
        updatedAt = ""
    )

    private fun toEvent(x: com.example.helloworld.data.EventItem) = Event(
        id = x.id,
        slug = x.slug,
        title = x.title,
        category = x.category,
        short_description = x.shortDescription,
        description = x.description,
        image = x.image,
        flyer_url = x.flyerUrl,
        start_at = x.startAt,
        end_at = x.endAt,
        all_day = x.allDay,
        location = x.location,
        address = x.address,
        attendance_type = x.attendanceType,
        registration_url = x.registrationUrl,
        contact = x.contact,
        livestream_url = x.livestreamUrl,
        featured = x.featured,
        status = x.status,
        display_order = x.displayOrder
    )
}
