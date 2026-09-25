package com.example.helloworld.notifications

import com.example.helloworld.data.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class NotificationBrandRepository {
    suspend fun getChurchName(): String =
        runCatching {
            SupabaseProvider.client
                .from("church_identity")
                .select(Columns.list("church_name", "official_name"))
                .decodeList<ChurchIdentityBrand>()
                .firstOrNull()
                ?.let { row ->
                    row.churchName.trim().ifBlank { row.officialName.trim() }
                }
                .orEmpty()
        }.getOrDefault("")

    @Serializable
    private data class ChurchIdentityBrand(
        @SerialName("church_name") val churchName: String = "",
        @SerialName("official_name") val officialName: String = ""
    )
}
