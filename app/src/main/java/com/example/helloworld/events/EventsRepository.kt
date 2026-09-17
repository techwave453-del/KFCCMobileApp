package com.example.helloworld.events

import com.example.helloworld.admin.AdminRepository
import com.example.helloworld.data.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import io.ktor.client.call.body
import io.ktor.http.HttpStatusCode

/**
 * Events data access.
 *
 * Public event reads go directly to Supabase so the mobile app does not depend
 * on the church website/Render server. Administrative event mutations remain
 * behind the existing authenticated administration boundary until that layer
 * is migrated to Supabase Auth/RLS.
 */
class EventsRepository(private val adminRepository: AdminRepository? = null) {

    suspend fun getPublicEvents(): Result<List<Event>> = runCatching {
        SupabaseProvider.client
            .from("events")
            .select {
                filter {
                    eq("status", "published")
                }
            }
            .decodeList<Event>()
            .sortedWith(compareBy<Event> { it.display_order }.thenBy { it.start_at })
    }

    suspend fun getAdminEvents(): Result<List<Event>> = runCatching {
        val repo = adminRepository ?: error("Administrator repository is required.")
        val response = repo.authenticatedGet("api/events")
        if (response.status != HttpStatusCode.OK) error("Unable to load events (${response.status.value}).")
        response.body()
    }

    suspend fun create(input: EventInput): Result<Event> = runCatching {
        val repo = adminRepository ?: error("Administrator repository is required.")
        val response = repo.authenticatedPost("api/admin/events", input)
        if (response.status != HttpStatusCode.Created) error(response.body<String>())
        response.body()
    }

    suspend fun update(id: Long, input: EventInput): Result<Event> = runCatching {
        val repo = adminRepository ?: error("Administrator repository is required.")
        val response = repo.authenticatedPut("api/admin/events/$id", input)
        if (response.status != HttpStatusCode.OK) error(response.body<String>())
        response.body()
    }

    suspend fun delete(id: Long): Result<Unit> = runCatching {
        val repo = adminRepository ?: error("Administrator repository is required.")
        val response = repo.authenticatedDelete("api/admin/events/$id")
        if (response.status != HttpStatusCode.OK) error("Unable to delete event (${response.status.value}).")
    }
}
