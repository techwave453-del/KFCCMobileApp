package com.example.helloworld.events

import com.example.helloworld.data.SupabaseProvider
import io.github.jan.supabase.postgrest.from

class EventsRepository(@Suppress("UNUSED_PARAMETER") adminRepository: com.example.helloworld.admin.AdminRepository? = null) {
    suspend fun getPublicEvents(): Result<List<Event>> = runCatching {
        SupabaseProvider.client.from("events").select {
            filter { eq("status", "published") }
        }.decodeList<Event>().sortedWith(compareBy<Event> { it.display_order }.thenBy { it.start_at })
    }

    suspend fun getAdminEvents(): Result<List<Event>> = runCatching {
        SupabaseProvider.client.from("events").select()
            .decodeList<Event>()
            .sortedWith(compareBy<Event> { it.display_order }.thenBy { it.start_at })
    }

    suspend fun create(input: EventInput): Result<Event> = runCatching {
        val nextId = SupabaseProvider.client.from("events").select().decodeList<Event>().maxOfOrNull { it.id }?.plus(1) ?: 1L
        val slug = input.title.trim().lowercase()
            .replace(Regex("[^a-z0-9]+"), "-").trim('-')
            .ifBlank { "event-$nextId" }
        val row = Event(
            id = nextId,
            slug = slug,
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
        SupabaseProvider.client.from("events").insert(row) { select() }.decodeList<Event>().first()
    }

    suspend fun update(id: Long, input: EventInput): Result<Event> = runCatching {
        SupabaseProvider.client.from("events").update({
            set("title", input.title)
            set("category", input.category)
            set("short_description", input.short_description)
            set("description", input.description)
            set("image", input.image)
            set("flyer_url", input.flyer_url)
            set("start_at", input.start_at)
            set("end_at", input.end_at)
            set("all_day", input.all_day)
            set("location", input.location)
            set("address", input.address)
            set("attendance_type", input.attendance_type)
            set("registration_url", input.registration_url)
            set("contact", input.contact)
            set("livestream_url", input.livestream_url)
            set("featured", input.featured)
            set("status", input.status)
            set("display_order", input.display_order)
        }) {
            filter { eq("id", id) }
            select()
        }.decodeList<Event>().first()
    }

    suspend fun delete(id: Long): Result<Unit> = runCatching {
        SupabaseProvider.client.from("events").delete {
            filter { eq("id", id) }
        }
    }
}
