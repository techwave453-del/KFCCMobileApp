package com.example.helloworld.admin.content

import kotlinx.serialization.Serializable

@Serializable
data class WebsiteContent(
    val name: String = "",
    val churchName: String = "",
    val phone: String = "",
    val email: String = "",
    val theme: ThemeSettings = ThemeSettings(),
    val services: List<SiteService> = emptyList(),
    val links: List<SiteLink> = emptyList(),
    val membershipClasses: List<SiteLink> = emptyList(),
    val detailContent: Map<String, SiteDetail> = emptyMap()
)

@Serializable
data class ThemeSettings(
    val mode: String = "light",
    val accent: String = "#4da6ff"
)

@Serializable
data class SiteService(
    val title: String = "",
    val time: String = ""
)

@Serializable
data class SiteLink(
    val title: String = "",
    val url: String = ""
)

@Serializable
data class SiteDetail(
    val eyebrow: String = "Church Information",
    val summary: String = "",
    val schedule: String = "",
    val sections: List<List<String>> = emptyList()
)
