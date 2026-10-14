package com.example.helloworld.admin.members

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CommunityMember(
    @SerialName("user_id") val userId: String,
    val username: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val bio: String? = null,
    val email: String? = null,
    @SerialName("admin_role") val adminRole: String? = null,
    val role: String? = null,
    @SerialName("is_admin_visible") val isAdminVisible: Boolean? = false,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("permissions") val assignedPermissions: List<String> = emptyList()
) {
    val safeUsername: String get() = username ?: "User"
    val safeDisplayName: String get() = displayName ?: safeUsername
    val safeBio: String get() = bio ?: ""
    val safeRole: String get() = adminRole ?: role ?: "member"
    val safePermissions: List<String> get() = if (assignedPermissions.isNotEmpty()) assignedPermissions else when (safeRole.lowercase()) {
        "admin" -> listOf("chat.send", "chat.moderate", "events.view", "events.manage", "media.view", "media.upload", "bible.quiz", "notifications.receive", "users.view")
        "leader" -> listOf("chat.send", "events.view", "events.manage", "media.view", "bible.quiz", "notifications.receive")
        "moderator" -> listOf("chat.send", "chat.moderate", "events.view", "media.view", "bible.quiz", "notifications.receive")
        else -> listOf("chat.send", "events.view", "media.view", "bible.quiz", "notifications.receive")
    }
}
