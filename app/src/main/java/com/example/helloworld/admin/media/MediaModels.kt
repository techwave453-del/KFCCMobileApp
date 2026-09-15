package com.example.helloworld.admin.media

import kotlinx.serialization.Serializable

@Serializable
data class AdminMediaItem(
    val id: Long = 0,
    val title: String = "Untitled media",
    val type: String = "",
    val category: String = "general",
    val description: String = "",
    val url: String = "",
    val thumbnail_url: String = "",
    val featured: Boolean = false,
    val published: Boolean = true,
    val created_at: String = ""
) {
    val isVideo: Boolean
        get() = type.equals("video", ignoreCase = true) || url.contains("youtube.com", true) || url.contains("youtu.be", true)
}

@Serializable
data class MediaUpdateRequest(
    val title: String,
    val description: String,
    val category: String,
    val featured: Boolean
)
