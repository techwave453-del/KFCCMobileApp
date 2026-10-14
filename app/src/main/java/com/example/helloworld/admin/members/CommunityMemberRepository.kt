package com.example.helloworld.admin.members

import com.example.helloworld.data.SupabaseProvider
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class BasicMemberUpdate(
    val username: String,
    @SerialName("display_name") val displayName: String,
    val bio: String
)

@Serializable
private data class RoleUpdatePayload(
    @SerialName("admin_role") val adminRole: String
)

@Serializable
private data class AdminProfileUpsert(
    @SerialName("auth_user_id") val authUserId: String,
    val username: String,
    val role: String,
    @SerialName("is_active") val isActive: Boolean,
    val permissions: List<String>
)

class CommunityMemberRepository {
    private val client = SupabaseProvider.client

    suspend fun getMembers(): Result<List<CommunityMember>> = runCatching {
        client.from("chat_profiles")
            .select {
                order("created_at", Order.DESCENDING)
                limit(100)
            }
            .decodeList<CommunityMember>()
    }

    suspend fun updateMember(userId: String, username: String, displayName: String, bio: String): Result<Unit> = runCatching {
        client.from("chat_profiles")
            .update(BasicMemberUpdate(username.trim(), displayName.trim(), bio.trim())) {
                filter { eq("user_id", userId) }
            }
    }

    suspend fun updateMemberRole(userId: String, username: String, role: String, permissions: List<String>): Result<Unit> = runCatching {
        val normalizedRole = role.trim().lowercase()
        // 1. Update chat_profiles admin_role
        client.from("chat_profiles")
            .update(RoleUpdatePayload(normalizedRole)) {
                filter { eq("user_id", userId) }
            }

        // 2. If promoted to admin/moderator/leader, upsert into admin_profiles so they gain access to allowed admin modules
        if (normalizedRole != "member") {
            runCatching {
                client.from("admin_profiles").upsert(
                    AdminProfileUpsert(
                        authUserId = userId,
                        username = username.ifBlank { "user_$userId" },
                        role = normalizedRole,
                        isActive = true,
                        permissions = permissions
                    )
                )
            }
        } else {
            // If demoted to regular member, deactivate in admin_profiles
            runCatching {
                client.from("admin_profiles")
                    .update(mapOf("is_active" to false)) {
                        filter { eq("auth_user_id", userId) }
                    }
            }
        }
    }

    suspend fun deleteMember(userId: String): Result<Unit> = runCatching {
        client.from("chat_profiles")
            .delete {
                filter { eq("user_id", userId) }
            }
        runCatching {
            client.from("admin_profiles")
                .delete {
                    filter { eq("auth_user_id", userId) }
                }
        }
    }
}
