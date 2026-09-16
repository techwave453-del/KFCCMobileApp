package com.example.helloworld.data

import com.example.helloworld.config.AppConfig
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

/**
 * Single Supabase client for non-privileged application data access.
 * The publishable/anon key is safe to ship in a client only when paired with
 * correct RLS policies. Never replace it with a service-role/secret key.
 *
 * Realtime is intentionally not installed here yet: the current Supabase
 * Kotlin dependency set in this branch does not include the Realtime module.
 * Chat therefore keeps its safe polling fallback until that dependency is
 * added and verified by Gradle.
 */
object SupabaseProvider {
    private const val SUPABASE_URL = "https://uhzfjuquhqxhqtppispq.supabase.co"

    val client by lazy {
        createSupabaseClient(
            supabaseUrl = SUPABASE_URL,
            supabaseKey = AppConfig.SUPABASE_PUBLISHABLE_KEY
        ) {
            install(Auth)
            install(Postgrest)
        }
    }
}
