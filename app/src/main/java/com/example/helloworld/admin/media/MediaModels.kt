package com.example.helloworld.admin.media

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AdminMediaItem(
    val id: Long = 0,
    @SerialName("legacy_id") val legacy_id: Long? = null,
    val title: String = "Untitled media",
    val type: String = "",
    val category: String = "general",
    val description: String = "",
    val url: String = "",
    @SerialName("storage_path") val storage_path: String? = null,
    val featured: Boolean = false,
    val published: Boolean = true,
    @SerialName("thumbnail_url") val thumbnail_url: String? = null,
    val created_at: String = ""
) {
    val isVideo: Boolean
        get() = type.equals("video", ignoreCase = true) || url.contains("youtube.com", true) || url.contains("youtu.be", true)
}
