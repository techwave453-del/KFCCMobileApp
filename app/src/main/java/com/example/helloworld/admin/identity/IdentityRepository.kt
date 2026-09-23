package com.example.helloworld.admin.identity

import com.example.helloworld.data.SupabaseProvider
import io.github.jan.supabase.postgrest.from

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

        // Keep the legacy public logo value synchronized because some public
        // app/site readers still consume it from site_content.
        client.from("site_content").upsert(
            mapOf("key" to "churchName", "value" to identity.churchName.trim())
        )
        client.from("site_content").upsert(
            mapOf("key" to "logo", "value" to identity.logo.trim())
        )
        client.from("site_content").upsert(
            mapOf("key" to "logoUrl", "value" to identity.logoUrl.trim())
        )

        identity
    }

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
