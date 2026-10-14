package com.example.helloworld.admin.members

import com.example.helloworld.data.SupabaseProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class BasicMemberUpdate(
    val username: String,
    @SerialName("display_name") val displayName: String,
    val bio: String
)

@Serializable
private data class MemberManagementRequest(
    val action: String,
    @SerialName("user_id") val userId: String? = null,
    val role: String? = null,
    val permissions: List<String>? = null
)

@Serializable
private data class MemberManagementResponse(
    val members: List<CommunityMember> = emptyList(),
    val ok: Boolean = false,
    val role: String? = null,
    val error: String? = null,
    val stage: String? = null
)

class CommunityMemberRepository {
    private val client = SupabaseProvider.client
    private val http = HttpClient(CIO) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }

    private suspend fun callManagement(request: MemberManagementRequest): MemberManagementResponse {
        val token = client.auth.currentAccessTokenOrNull()
            ?: error("Your administrator session has expired. Please sign in again.")
        return http.post("$FUNCTIONS_URL/admin-management") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun getMembers(): Result<List<CommunityMember>> = runCatching {
        val response = callManagement(MemberManagementRequest(action = "community_members"))
        if (response.error != null) {
            val stage = response.stage?.takeIf { it.isNotBlank() }?.let { " (stage: $it)" }.orEmpty()
            error(response.error + stage)
        }
        response.members
    }

    suspend fun updateMember(userId: String, username: String, displayName: String, bio: String): Result<Unit> = runCatching {
        client.from("chat_profiles")
            .update(BasicMemberUpdate(username.trim(), displayName.trim(), bio.trim())) {
                filter { eq("user_id", userId) }
            }
    }

    suspend fun updateMemberRole(userId: String, username: String, role: String, permissions: List<String>): Result<Unit> = runCatching {
        val response = callManagement(
            MemberManagementRequest(
                action = "community_role",
                userId = userId,
                role = role.trim().lowercase(),
                permissions = if (role.trim().equals("custom", ignoreCase = true)) permissions.distinct().sorted() else null
            )
        )
        if (response.error != null) {
            val stage = response.stage?.takeIf { it.isNotBlank() }?.let { " (stage: $it)" }.orEmpty()
            error(response.error + stage)
        }
        if (!response.ok) error("The server did not confirm the role assignment.")
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

    companion object {
        private const val FUNCTIONS_URL = "https://uhzfjuquhqxhqtppispq.supabase.co/functions/v1"
    }
}
