package com.example.helloworld.admin.identity

import kotlinx.serialization.Serializable

@Serializable
data class ChurchIdentity(
    val churchName: String = "",
    val officialName: String = "",
    val logo: String = "",
    val logoUrl: String = "",
    val officialLogo: String = "",
    val registrationDetails: String = ""
)

@Serializable
data class SiteContentResponse(
    val churchName: String = "",
    val officialName: String = "",
    val logo: String = "",
    val logoUrl: String = "",
    val officialLogo: String = "",
    val registrationDetails: String = ""
)
