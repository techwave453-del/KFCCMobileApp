package com.example.helloworld.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ChurchInfo(
    val churchName: String = "",
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
data class ChurchService(
    val title: String,
    val time: String,
    @SerialName("image") val imageUrl: String
)

@Serializable
data class ChurchLink(
    val title: String,
    val text: String = "",
    @SerialName("image") val imageUrl: String,
    val url: String
)

@Serializable
data class MembershipClass(
    val title: String,
    @SerialName("image") val imageUrl: String,
    val registrationUrl: String
)

@Serializable
data class LiveStream(
    val enabled: Boolean = false,
    val url: String = "",
    val title: String = "",
    val description: String = ""
)

@Serializable
data class MediaItem(
    val id: Long,
    val title: String,
    val type: String, // image, video, audio, document
    val category: String,
    val description: String = "",
    val url: String,
    val featured: Boolean = false,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class SiteContentRow(
    val key: String,
    val value: String
)

object ChurchContent {
    val default = ChurchInfo(
        churchName = "Kingdom Fellowship Christian Church",
        tagline = "Revealing Christ to Nations",
        title = "Welcome Home",
        subtitle = "A place of faith, fellowship, worship and transformation.",
        aboutTitle = "About Kingdom Fellowship Christian Church",
        aboutText = "Kingdom Fellowship Christian Church is a community committed to revealing Christ to nations through worship, fellowship, the Word of God, prayer, service and the transforming power of the Gospel. Everyone is welcome to find a place to belong and grow in faith.",
        phone = "+254 700 000 000",
        email = "hello@aickitanga.org",
        services = listOf(
            ChurchService("Sunday Worship Service", "Sundays | 9:00 AM", "https://images.unsplash.com/photo-1511632765486-a01980e01a18?auto=format&fit=crop&w=1600&q=85"),
            ChurchService("Healing and Deliverance Service", "Tuesdays | 5:30 PM", "https://images.unsplash.com/photo-1507692049790-de58290a4334?auto=format&fit=crop&w=1600&q=85"),
            ChurchService("Power Communion Service", "Wednesdays | 5:30 PM", "https://images.unsplash.com/photo-1544427920-c49ccfb85579?auto=format&fit=crop&w=1600&q=85"),
            ChurchService("Worship, Word & Wonders Night", "Fridays | 5:30 PM", "https://images.unsplash.com/photo-1529070538774-1843cb3265df?auto=format&fit=crop&w=1600&q=85"),
            ChurchService("Commanding the Day Midnight Prayer", "Last Friday | 11:00 PM", "https://images.unsplash.com/photo-1472162072942-cd5147eb3902?auto=format&fit=crop&w=1600&q=85")
        ),
        links = listOf(
            ChurchLink("I'm New Here", "Find out how to visit and get connected", "https://store.christianitytoday.com/cdn/shop/articles/Untitled_design_9_large.jpg?v=1717170785", "#visit"),
            ChurchLink("Find a Branch", "Connect with the church community", "https://cfni.org/wp-content/uploads/2024/12/Banner_Mackbook16_Worship.webp", "#visit"),
            ChurchLink("Upcoming Programs", "See our regular services and activities", "https://cdn.prod.website-files.com/5f6b9a421d5a61e1d0cd9e3d/67993630bb7f463a5b9c6b0a_worship-672c02982a03e589238fc443_62f285c4f9aa3441840257d6_nathan-mullet-pmiW630yDPE-unsplash.jpeg", "#events"),
            ChurchLink("Testimonies", "Celebrate what God is doing in our community", "https://store.christianitytoday.com/cdn/shop/articles/Untitled_design_9_large.jpg?v=1717170785", "#media"),
            ChurchLink("Resources", "Messages, media and helpful resources", "https://cfni.org/wp-content/uploads/2024/12/Banner_Mackbook16_Worship.webp", "#resources")
        ),
        membershipClasses = listOf(
            MembershipClass("Foundation Class", "https://store.christianitytoday.com/cdn/shop/articles/Untitled_design_9_large.jpg?v=1717170785", "#contact"),
            MembershipClass("Maturity Class", "https://cfni.org/wp-content/uploads/2024/12/Banner_Mackbook16_Worship.webp", "#contact")
        )
    )
}
