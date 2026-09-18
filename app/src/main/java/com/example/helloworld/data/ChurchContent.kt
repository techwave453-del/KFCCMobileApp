package com.example.helloworld.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChurchInfo(
    val churchName: String = "",
    val logoUrl: String = "",
    val tagline: String = "",
    val title: String = "",
    val subtitle: String = "",
    val aboutTitle: String = "",
    val aboutText: String = "",
    val phone: String = "",
    val email: String = "",
    val services: List<ChurchService> = emptyList(),
    val links: List<ChurchLink> = emptyList(),
    val membershipClasses: List<MembershipClass> = emptyList(),
    val liveStream: LiveStream = LiveStream()
)

@Serializable
data class ChurchService(val title: String, val time: String, @SerialName("image") val imageUrl: String)

@Serializable
data class ChurchLink(val title: String, val text: String = "", @SerialName("image") val imageUrl: String, val url: String)

@Serializable
data class MembershipClass(val title: String, @SerialName("image") val imageUrl: String, val registrationUrl: String)

@Serializable
data class LiveStream(val enabled: Boolean = false, val url: String = "", val title: String = "", val description: String = "")

@Serializable
data class MediaItem(val id: Long, val title: String, val type: String, val category: String, val description: String = "", val url: String, val featured: Boolean = false, @SerialName("created_at") val createdAt: String)

@Serializable
data class SiteContentRow(val key: String, val value: String)

/** Empty fallback prevents branding one church as another when Supabase is unavailable. */
object ChurchContent {
    val default = ChurchInfo()
}
