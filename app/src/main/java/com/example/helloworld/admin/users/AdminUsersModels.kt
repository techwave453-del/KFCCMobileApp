package com.example.helloworld.admin.users

import kotlinx.serialization.Serializable

@Serializable
data class AdminManagedUser(
    val id: Long = 0,
    val username: String = "",
    val role: String = "custom",
    val is_active: Boolean = true,
    val must_change_password: Boolean = false,
    val permissions: List<String> = emptyList(),
    val created_at: String? = null,
    val last_login_at: String? = null
)

@Serializable
data class AdminAccessRequest(
    val id: Long = 0,
    val username: String = "",
    val status: String = "pending",
    val created_at: String? = null,
    val expires_at: String? = null
)
