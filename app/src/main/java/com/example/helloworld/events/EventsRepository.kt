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
            .update(input) {
                filter { eq("id", id) }
            }
            .decodeSingle<Event>()
    }

    suspend fun delete(id: Long): Result<Unit> = runCatching {
        SupabaseProvider.client
            .from("events")
            .delete {
                filter { eq("id", id) }
            }
    }
}
