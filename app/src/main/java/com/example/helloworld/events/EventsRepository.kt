package com.example.helloworld.events

import com.example.helloworld.admin.AdminRepository
import com.example.helloworld.data.KfccDataContext
import com.example.helloworld.data.SupabaseProvider
import com.example.helloworld.data.offline.KfccContentRepository
import com.example.helloworld.data.offline.KfccDatabase
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Events data access.
 *
 * Public reads are offline-first through Room. Admin mutations continue to
 * use the authenticated Supabase boundary.
 */
class EventsRepository(private val adminRepository: AdminRepository? = null) {

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
        SupabaseProvider.client
            .from("events")
            .insert(input)
            .decodeSingle<Event>()
    }

    suspend fun update(id: Long, input: EventInput): Result<Event> = runCatching {
        SupabaseProvider.client
            .from("events")
            .update(input) { filter { eq("id", id) } }
            .decodeSingle<Event>()
    }

    suspend fun delete(id: Long): Result<Unit> = runCatching {
        SupabaseProvider.client
            .from("events")
            .delete { filter { eq("id", id) } }
    }

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
