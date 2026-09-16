package com.example.helloworld.data.media

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PublicMediaItem(
    val id: Long,
    val title: String,
    val type: String,
    val category: String = "general",
    val description: String? = null,
    val url: String,
    @SerialName("storage_path")
    val storagePath: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    val published: Boolean = true,
    @SerialName("thumbnail_url")
    val thumbnailUrl: String? = null,
    val featured: Boolean = false,
)
