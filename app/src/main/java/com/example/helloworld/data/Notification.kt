package com.example.helloworld.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppNotification(
    val id: String,
    val title: String,
    val message: String,
    val type: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("user_id") val userId: String? = null,
    val readAt: String? = null,
    @SerialName("is_enabled") val isEnabled: Boolean = true,
    @SerialName("show_on_install") val showOnInstall: Boolean = false,
    @SerialName("show_on_sign_in") val showOnSignIn: Boolean = false,
    @SerialName("updated_at") val updatedAt: String? = null
)
