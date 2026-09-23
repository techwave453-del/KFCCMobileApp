package com.example.helloworld.admin.identity

import com.example.helloworld.data.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.Serializable

class IdentityRepository {
    private val client = SupabaseProvider.client

    suspend fun load(): Result<ChurchIdentity> = runCatching {
        client.from("church_identity")
            .select()
            .decodeSingle<ChurchIdentityRow>()
            .toDomain()
    }

    suspend fun save(identity: ChurchIdentity): Result<ChurchIdentity> = runCatching {
        client.from("church_identity").update(
            {
                set("church_name", identity.churchName.trim())
                set("official_name", identity.officialName.trim())
                set("logo_url", identity.logoUrl.trim())
                set("official_logo", identity.officialLogo.trim())
                set("registration_details", identity.registrationDetails.trim())
            }
        ) {
            filter { eq("id", 1) }
        }

        // Keep the public church name synchronized with the legacy site_content
        // key. This key is explicitly protected by identity.edit RLS.
        client.from("site_content").upsert(
            mapOf("key" to "churchName", "value" to identity.churchName.trim())
        )

        identity
    }

    @Serializable
    private data class ChurchIdentityRow(
        val id: Int = 1,
        val church_name: String = "",
        val official_name: String = "",
        val logo_url: String = "",
        val official_logo: String = "",
        val registration_details: String = ""
    ) {
        fun toDomain() = ChurchIdentity(
            churchName = church_name,
            officialName = official_name,
            logoUrl = logo_url,
            officialLogo = official_logo,
            registrationDetails = registration_details
        )
    }
}
