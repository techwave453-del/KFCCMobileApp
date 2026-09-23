package com.example.helloworld.admin.identity

import com.example.helloworld.admin.AdminRepository
import com.example.helloworld.data.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns

class IdentityRepository(private val adminRepository: AdminRepository) {
    private val client = SupabaseProvider.client

    suspend fun load(): Result<ChurchIdentity> = runCatching {
        // Read directly from Supabase. The UI/server remains responsible for
        // authorization; this repository does not use the website API.
        val row = client.from("church_identity")
            .select(Columns.list("id", "church_name", "official_name", "logo_url", "official_logo", "registration_details"))
            .decodeSingle<ChurchIdentityRow>()

        row.toDomain()
    }

    suspend fun save(identity: ChurchIdentity): Result<ChurchIdentity> = runCatching {
        client.from("church_identity").upsert(
            ChurchIdentityRow(
                id = 1,
                church_name = identity.churchName.trim(),
                official_name = identity.officialName.trim(),
                logo_url = identity.logoUrl.trim(),
                official_logo = identity.officialLogo.trim(),
                registration_details = identity.registrationDetails.trim()
            )
        )

        // The legacy logo field is not part of the new direct Supabase schema.
        identity.copy(logo = identity.logo.trim()).copy(
            churchName = identity.churchName.trim(),
            officialName = identity.officialName.trim(),
            logoUrl = identity.logoUrl.trim(),
            officialLogo = identity.officialLogo.trim(),
            registrationDetails = identity.registrationDetails.trim()
        )
    }

    @kotlinx.serialization.Serializable
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
