package com.example.helloworld.data

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonArray
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class KanisaAssistantMessage(
    val role: String,
    val content: String
)

@Serializable
data class KanisaAssistantRequest(
    val message: String,
    val provider: String = "cloud",
    val conversation: List<KanisaAssistantMessage> = emptyList()
)

@Serializable
data class StoredKanisaAssistantMessage(
    val id: Long,
    val user_id: String,
    val role: String,
    val content: String,
    val bible_references: List<String> = emptyList(),
    val created_at: String
)

@Serializable
data class KanisaAssistantResponse(
    val assistant_name: String = "Kanisa Assistant",
    val answer: String,
    val bible_references: List<String> = emptyList(),
    val provider: String = "cloud",
    val model: String? = null,
    val error: String? = null
)

class KanisaAssistantRepository {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    suspend fun loadHistory(limit: Int = 80): Result<List<StoredKanisaAssistantMessage>> = runCatching {
        val userId = currentUserId() ?: error("Please sign in to use Kanisa Assistant.")
        SupabaseProvider.client.from("kanisa_assistant_messages")
            .select {
                filter { eq("user_id", userId) }
                order("created_at", io.github.jan.supabase.postgrest.query.Order.ASCENDING)
                limit(limit.toLong())
            }
            .decodeList()
    }

    suspend fun saveMessage(
        role: String,
        content: String,
        bibleReferences: List<String> = emptyList()
    ): Result<Unit> = runCatching {
        val userId = currentUserId() ?: error("Please sign in to use Kanisa Assistant.")
        require(role == "user" || role == "assistant") { "Invalid assistant message role." }
        require(content.isNotBlank()) { "Message cannot be empty." }

        SupabaseProvider.client.from("kanisa_assistant_messages").insert(
            mapOf(
                "user_id" to userId,
                "role" to role,
                "content" to content.trim(),
                "bible_references" to JsonArray(bibleReferences.map { kotlinx.serialization.json.JsonPrimitive(it) })
            )
        )
    }

    suspend fun deleteMessage(id: Long): Result<Unit> = runCatching {
        val userId = currentUserId() ?: error("Please sign in again.")
        SupabaseProvider.client.from("kanisa_assistant_messages").delete {
            filter {
                eq("id", id)
                eq("user_id", userId)
            }
        }
    }

    suspend fun clearHistory(): Result<Unit> = runCatching {
        val userId = currentUserId() ?: error("Please sign in again.")
        SupabaseProvider.client.from("kanisa_assistant_messages").delete {
            filter { eq("user_id", userId) }
        }
    }

    private fun currentUserId(): String? =
        SupabaseProvider.client.auth.currentUserOrNull()?.id
            ?: SupabaseProvider.client.auth.currentSessionOrNull()?.user?.id

    suspend fun ask(
        message: String,
        conversation: List<KanisaAssistantMessage> = emptyList()
    ): Result<KanisaAssistantResponse> = runCatching {
        val token = SupabaseProvider.client.auth.currentAccessTokenOrNull()
            ?: error("Please sign in to use Kanisa Assistant.")

        val response = client.post("$FUNCTIONS_URL/kanisa-assistant") {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            setBody(KanisaAssistantRequest(message = message, conversation = conversation))
        }

        if (response.status.value !in 200..299) {
            val errorBody = runCatching { response.body<KanisaAssistantResponse>() }.getOrNull()
            error(errorBody?.error ?: "Kanisa Assistant is temporarily unavailable.")
        }

        response.body()
    }

    companion object {
        private const val FUNCTIONS_URL =
            "https://uhzfjuquhqxhqtppispq.supabase.co/functions/v1"
    }
}
