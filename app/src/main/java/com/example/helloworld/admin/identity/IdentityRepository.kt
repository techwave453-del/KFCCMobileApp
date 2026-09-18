package com.example.helloworld.admin.identity

import com.example.helloworld.data.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.Serializable

@Serializable
private data class ChurchIdentityRow(
    val id: Int = 1,
    val church_name: String = "",
    val official_name: String = "",
    val logo_url: String = "",
    val official_logo: String = "",
    val registration_details: String = ""
)

class IdentityRepository {
    private val client get() = SupabaseProvider.client

    suspend fun load(): Result<ChurchIdentity> = runCatching {
        val row = client.from("church_identity")
            .select()
            .decodeList<ChurchIdentityRow>()
            .firstOrNull()
            ?: error("Church Identity has not been configured yet.")

        ChurchIdentity(
            churchName = row.church_name,
            officialName = row.official_name,
            logo = row.logo_url,
            logoUrl = row.logo_url,
            officialLogo = row.official_logo,
            registrationDetails = row.registration_details
        )
    }

    suspend fun save(identity: ChurchIdentity): Result<ChurchIdentity> = runCatching {
        client.from("church_identity").upsert(
            ChurchIdentityRow(
                church_name = identity.churchName.trim(),
                official_name = identity.officialName.trim(),
                logo_url = identity.logoUrl.trim().ifBlank { identity.logo.trim() },
                official_logo = identity.officialLogo.trim(),
                registration_details = identity.registrationDetails.trim()
            )
        )
        identity
    }
}
