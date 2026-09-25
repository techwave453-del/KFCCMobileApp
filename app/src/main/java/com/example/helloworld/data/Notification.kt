package com.example.helloworld.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppNotification(
    val id: String,
    val title: String,
    val message: String,
    val type: String, // welcome, admin, chat, general
    @SerialName("created_at") val createdAt: String,
    @SerialName("user_id") val userId: String? = null,
    val readAt: String? = null
)
