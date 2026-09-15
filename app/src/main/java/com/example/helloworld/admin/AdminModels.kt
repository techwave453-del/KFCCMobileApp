package com.example.helloworld.admin

import kotlinx.serialization.Serializable

@Serializable
data class AdminUser(
    val id: Long = 0,
    val username: String = "",
    val role: String = "",
    val is_active: Boolean = true,
    val permissions: List<String> = emptyList()
)

@Serializable
data class AdminLoginRequest(
    val username: String,
    val password: String
)

@Serializable
data class AdminLoginResponse(
    val ok: Boolean = false,
    val user: AdminUser? = null,
    val error: String? = null,
    val requiresPasswordSetup: Boolean = false
)

@Serializable
data class AdminMeResponse(
    val user: AdminUser? = null,
    val error: String? = null,
    val requiresPasswordSetup: Boolean = false
)

object AdminPermissions {
    const val IDENTITY_VIEW = "identity.view"
    const val IDENTITY_EDIT = "identity.edit"
    const val SITE_EDIT = "site.edit"
    const val HOMEPAGE_EDIT = "content.homepage.edit"
    const val ABOUT_EDIT = "content.about.edit"
    const val SERVICES_EDIT = "content.services.edit"
    const val LINKS_EDIT = "content.links.edit"
    const val CLASSES_EDIT = "content.classes.edit"
    const val GALLERY_EDIT = "content.gallery.edit"
    const val THEME_EDIT = "theme.edit"
    const val MEDIA_VIEW = "media.view"
    const val MEDIA_UPLOAD = "media.upload"
    const val MEDIA_EDIT = "media.edit"
    const val MEDIA_DELETE = "media.delete"
    const val LIVE_VIEW = "live.view"
    const val LIVE_MANAGE = "live.manage"
    const val COMMENTS_VIEW = "comments.view"
    const val COMMENTS_MODERATE = "comments.moderate"
    const val USERS_VIEW = "users.view"
    const val USERS_CREATE = "users.create"
    const val USERS_EDIT = "users.edit"
    const val USERS_DISABLE = "users.disable"
    const val USERS_DELETE = "users.delete"
    const val USERS_PERMISSIONS = "users.permissions"
    const val AUDIT_VIEW = "audit.view"
}

fun AdminUser.hasPermission(permission: String): Boolean =
    is_active && (role == "super_admin" || permissions.contains(permission))

fun AdminUser.canEditChurchIdentity(): Boolean =
    is_active && role == "super_admin" && hasPermission(AdminPermissions.IDENTITY_EDIT)
