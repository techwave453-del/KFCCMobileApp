package com.example.helloworld.data

import com.example.helloworld.config.AppConfig
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage

/**
 * Single Supabase client for non-privileged application data access.
 * The publishable/anon key is safe to ship in a client only when paired with
 * correct RLS policies. Never replace it with a service-role/secret key.
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
            install(Storage)
            install(Realtime) {}
        }
    }
}
